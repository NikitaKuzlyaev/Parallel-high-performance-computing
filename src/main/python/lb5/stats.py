from __future__ import annotations

from collections import defaultdict, deque
from dataclasses import dataclass


@dataclass
class RequestStat:
    """
    одна запись статистики по запросу
    """
    success: bool
    elapsed_ms: int


class StatsStore:
    """
    тут хранится статистика по каждому URL

    для каждого адреса запоминаем последние window_size запросов,
    а потом по этим данным считаем:
    - процент успешных запросов
    - среднее время ответа
    - текущий concurrency для adaptive-стратегии
    """

    def __init__(self, window_size: int = 20):
        self.window_size = window_size

        # для каждого URL будет свой список последних запросов
        self.items = defaultdict(lambda: deque(maxlen=window_size))

        # тут храним текущий concurrency для каждого URL
        self.concurrency: dict[str, int] = {}

    def add(
        self,
        url: str,
        success: bool,
        elapsed_ms: int,
        max_concurrent: int,
    ) -> None:
        """
        добавить новый результат запроса и пересчитать concurrency
        """
        self.items[url].append(
            RequestStat(
                success=success,
                elapsed_ms=elapsed_ms,
            )
        )

        stat = self.get_url_stats(url, max_concurrent)
        current = self.concurrency.get(url, max_concurrent)

        # если много ошибок или сервис медленный,
        # то уменьшаем количество одновременных запросов
        if stat["success_rate"] < 0.7 or stat["avg_ms"] > 2000:
            current -= 1

        # если сервис отвечает быстро и почти без ошибок,
        # то можно аккуратно поднять нагрузку обратно
        elif stat["success_rate"] > 0.9 and stat["avg_ms"] < 500:
            current += 1

        # concurrency не должен быть меньше 1
        if current < 1:
            current = 1

        # и не должен быть больше общего лимита
        if current > max_concurrent:
            current = max_concurrent

        self.concurrency[url] = current

    def get_concurrency(self, url: str, max_concurrent: int) -> int:
        """
        получить текущий concurrency для конкретного URL
        если статистики ещё нет, возвращаем max_concurrent
        """
        return self.concurrency.get(url, max_concurrent)

    def get_url_stats(self, url: str, max_concurrent: int) -> dict:
        """
        посчитать статистику по одному URL
        """
        items = list(self.items.get(url, []))
        current = self.concurrency.get(url, max_concurrent)

        if not items:
            return {
                "success_rate": 1.0,
                "avg_ms": 0,
                "adjusted_concurrency": current,
            }

        successful = 0
        total_ms = 0

        for item in items:
            if item.success:
                successful += 1

            total_ms += item.elapsed_ms

        success_rate = successful / len(items)
        avg_ms = total_ms / len(items)

        return {
            "success_rate": round(success_rate, 2),
            "avg_ms": round(avg_ms),
            "adjusted_concurrency": current,
        }

    def get_many_stats(self, urls: list[str], max_concurrent: int) -> dict:
        """
        получить статистику сразу по нескольким URL
        это нужно, чтобы вернуть adaptive_stats в ответе API
        """
        result = {}

        for url in urls:
            result[url] = self.get_url_stats(url, max_concurrent)

        return result