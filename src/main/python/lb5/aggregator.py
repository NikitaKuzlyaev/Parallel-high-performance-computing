from __future__ import annotations

import asyncio
import time
import uuid

import aiohttp

from stats import StatsStore


class Aggregator:
    """
    основной класс, который делает запросы к внешним API и собирает ответы в один общий результат
    """

    def __init__(self, stats: StatsStore):
        """
        stats нужен для adaptive-стратегии
        """
        self.stats = stats

    async def aggregate(
            self,
            urls: list[str],
            strategy: str = "fixed",
            max_concurrent: int = 3,
            timeout_sec: float = 5,
    ) -> dict:
        """
        главный метод агрегации

        он выбирает стратегию, запускает запросы,
        считает успешные/неуспешные ответы и формирует итоговый json
        """
        started = time.perf_counter()

        if strategy == "timeout_race":
            results = await self._timeout_race(
                urls,
                max_concurrent,
                timeout_sec,
            )
            concurrent_used = len(urls)

        elif strategy == "adaptive":
            results = await self._adaptive(
                urls,
                max_concurrent,
                timeout_sec,
            )

            values = [
                self.stats.get_concurrency(url, max_concurrent)
                for url in urls
            ]

            concurrent_used = min(values) if values else max_concurrent

        else:
            strategy = "fixed"
            results = await self._fixed(
                urls,
                max_concurrent,
                timeout_sec,
            )
            concurrent_used = max_concurrent

        total_time_ms = round((time.perf_counter() - started) * 1000)

        successful = 0

        for item in results:
            if 200 <= item.get("status", 0) < 300 and not item.get("timeout"):
                successful += 1

        failed = len(results) - successful

        response = {
            "request_id": str(uuid.uuid4()),
            "results": results,
            "summary": {
                "total": len(results),
                "successful": successful,
                "failed": failed,
                "total_time_ms": total_time_ms,
                "strategy_used": strategy,
                "concurrent_used": concurrent_used,
            },
            "adaptive_stats": {},
        }

        if strategy == "adaptive":
            response["adaptive_stats"] = self.stats.get_many_stats(
                urls,
                max_concurrent,
            )

        return response

    async def _fixed(
            self,
            urls: list[str],
            max_concurrent: int,
            timeout_sec: float,
    ) -> list[dict]:
        """
        fixed-стратегия

        запускаем запросы с фиксированным ограничением
        на количество одновременных запросов
        """
        semaphore = asyncio.Semaphore(max_concurrent)

        async with aiohttp.ClientSession() as session:
            tasks = []

            for url in urls:
                tasks.append(
                    self._limited_fetch(
                        session,
                        semaphore,
                        url,
                        timeout_sec,
                        max_concurrent,
                    )
                )

            return await asyncio.gather(*tasks)

    async def _timeout_race(
            self,
            urls: list[str],
            max_concurrent: int,
            timeout_sec: float,
    ) -> list[dict]:
        """
        timeout_race-стратегия

        запускаем все запросы сразу, каждый отдельный запрос ограничивается timeout_sec
        """
        async with aiohttp.ClientSession() as session:
            tasks = []

            for url in urls:
                tasks.append(
                    self._fetch(
                        session,
                        url,
                        timeout_sec,
                        max_concurrent,
                    )
                )

            return await asyncio.gather(*tasks)

    async def _adaptive(
            self,
            urls: list[str],
            max_concurrent: int,
            timeout_sec: float,
    ) -> list[dict]:
        """
        adaptive-стратегия

        есть общий лимит max_concurrent, для каждого URL дополнительно берём свой лимит из статистики
        """
        global_semaphore = asyncio.Semaphore(max_concurrent)

        url_semaphores = {}

        for url in urls:
            limit = self.stats.get_concurrency(url, max_concurrent)
            url_semaphores[url] = asyncio.Semaphore(limit)

        async with aiohttp.ClientSession() as session:
            tasks = []

            for url in urls:
                tasks.append(
                    self._adaptive_fetch(
                        session=session,
                        global_semaphore=global_semaphore,
                        url_semaphore=url_semaphores[url],
                        url=url,
                        timeout_sec=timeout_sec,
                        max_concurrent=max_concurrent,
                    )
                )

            return await asyncio.gather(*tasks)

    async def _adaptive_fetch(
            self,
            session: aiohttp.ClientSession,
            global_semaphore: asyncio.Semaphore,
            url_semaphore: asyncio.Semaphore,
            url: str,
            timeout_sec: float,
            max_concurrent: int,
    ) -> dict:
        """
        запрос для adaptive-стратегии

        сначала проверяем общий лимит, потом лимит конкретного URL
        """
        async with global_semaphore:
            async with url_semaphore:
                return await self._fetch(
                    session,
                    url,
                    timeout_sec,
                    max_concurrent,
                )

    async def _limited_fetch(
            self,
            session: aiohttp.ClientSession,
            semaphore: asyncio.Semaphore,
            url: str,
            timeout_sec: float,
            max_concurrent: int,
    ) -> dict:
        """
        запрос с обычным ограничением по семафору, используется в fixed-стратегии
        """
        async with semaphore:
            return await self._fetch(
                session,
                url,
                timeout_sec,
                max_concurrent,
            )

    async def _fetch(
            self,
            session: aiohttp.ClientSession,
            url: str,
            timeout_sec: float,
            max_concurrent: int,
    ) -> dict:
        """
        один HTTP-запрос к внешнему API

        здесь замеряем время, обрабатываем успешный ответ, HTTP-ошибку, timeout и другие исключения
        """
        started = time.perf_counter()

        try:
            async with session.get(url, timeout=timeout_sec) as response:
                elapsed_ms = round((time.perf_counter() - started) * 1000)

                try:
                    data = await response.json()
                except Exception:
                    data = await response.text()

                success = 200 <= response.status < 300

                self.stats.add(
                    url,
                    success,
                    elapsed_ms,
                    max_concurrent,
                )

                if success:
                    return {
                        "url": url,
                        "status": response.status,
                        "data": data,
                        "elapsed_ms": elapsed_ms,
                    }

                return {
                    "url": url,
                    "status": response.status,
                    "error": str(data),
                    "elapsed_ms": elapsed_ms,
                }

        except asyncio.TimeoutError:
            elapsed_ms = round((time.perf_counter() - started) * 1000)

            self.stats.add(
                url,
                False,
                elapsed_ms,
                max_concurrent,
            )

            return {
                "url": url,
                "timeout": True,
                "elapsed_ms": elapsed_ms,
            }

        except Exception as exc:
            elapsed_ms = round((time.perf_counter() - started) * 1000)

            self.stats.add(
                url,
                False,
                elapsed_ms,
                max_concurrent,
            )

            return {
                "url": url,
                "error": f"{type(exc).__name__}: {exc}",
                "elapsed_ms": elapsed_ms,
            }
