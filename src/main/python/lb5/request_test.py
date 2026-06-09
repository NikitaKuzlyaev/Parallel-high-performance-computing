from __future__ import annotations

import argparse
import asyncio
from pathlib import Path

import aiohttp
import uvicorn

from app import app
from fake_api import SCENARIOS, start_fake_api
from graphs import build_graphs
from utils import save_csv, save_json

# стратегии, которые будем прогонять в тестах
STRATEGIES = ["fixed", "timeout_race", "adaptive"]


async def start_gateway(port: int) -> tuple[uvicorn.Server, asyncio.Task]:
    """
    запуск основного приложения
    """
    config = uvicorn.Config(
        app,
        host="127.0.0.1",
        port=port,
        log_level="warning",
    )

    server = uvicorn.Server(config)

    # запускаем FastAPI в фоне,
    # чтобы дальше можно было выполнять запросы
    task = asyncio.create_task(server.serve())

    # небольшая пауза, чтобы сервер успел подняться
    await asyncio.sleep(1)

    return server, task


async def make_requests(
        scenario: str,
        repeats: int,
        port: int,
        output: str,
) -> None:
    """
    метод для совершения и обработки запросов
    """
    output_dir = Path(output)
    output_dir.mkdir(parents=True, exist_ok=True)

    # сюда складываем все ответы целиком
    raw_results = []

    # краткая статистика для csv и графиков
    summary_rows = []

    # отдельная статистика adaptive-стратегии
    adaptive_rows = []

    server, task = await start_gateway(port)

    if scenario == "all":
        scenarios = list(SCENARIOS.keys())
    else:
        scenarios = [scenario]

    try:
        async with aiohttp.ClientSession() as session:

            for scenario_name in scenarios:

                # для каждого сценария используем свой порт чтобы не было конфликтов
                fake_port = (
                        9200
                        + list(SCENARIOS.keys()).index(scenario_name)
                )

                runner, urls = await start_fake_api(
                    scenario_name,
                    fake_port,
                )

                try:
                    print("\nСценарий:", scenario_name)

                    for strategy in STRATEGIES:

                        for repeat in range(1, repeats + 1):

                            body = {
                                "urls": urls,
                                "strategy": strategy,
                                "max_concurrent": 3,
                                "timeout_sec": 4,
                            }

                            async with session.post(
                                    f"http://127.0.0.1:{port}/aggregate",
                                    json=body,
                            ) as response:
                                data = await response.json()
                                summary = data["summary"]

                            # отдельно считаем таймауты
                            timeouts = 0

                            for item in data["results"]:
                                if item.get("timeout"):
                                    timeouts += 1

                            raw_results.append(
                                {
                                    "scenario": scenario_name,
                                    "strategy": strategy,
                                    "repeat": repeat,
                                    "response": data,
                                }
                            )

                            summary_rows.append(
                                {
                                    "scenario": scenario_name,
                                    "strategy": strategy,
                                    "repeat": repeat,
                                    "total_time_ms": summary["total_time_ms"],
                                    "successful": summary["successful"],
                                    "failed": summary["failed"],
                                    "timeouts": timeouts,
                                    "concurrent_used": summary["concurrent_used"],
                                }
                            )

                            # adaptive дополнительно отдаёт
                            # внутреннюю статистику по URL
                            if strategy == "adaptive":
                                for url, stat in data["adaptive_stats"].items():
                                    adaptive_rows.append(
                                        {
                                            "scenario": scenario_name,
                                            "repeat": repeat,
                                            "url": url,
                                            "success_rate": stat["success_rate"],
                                            "avg_ms": stat["avg_ms"],
                                            "concurrency": stat[
                                                "adjusted_concurrency"
                                            ],
                                        }
                                    )

                            print(
                                strategy,
                                "repeat", repeat,
                                "time_ms=", summary["total_time_ms"],
                                "success=", summary["successful"],
                                "failed=", summary["failed"],
                                "concurrency=", summary["concurrent_used"],
                            )

                finally:
                    # останавливаем фейковый API
                    await runner.cleanup()

    finally:
        # корректно завершаем FastAPI-сервер
        server.should_exit = True
        await task

    # сохраняем сырые данные
    save_json(raw_results, output_dir / "raw_results.json")

    # сохраняем сводную статистику
    save_json(summary_rows, output_dir / "summary.json")
    save_csv(summary_rows, output_dir / "summary.csv")

    # статистика adaptive отдельно
    save_csv(
        adaptive_rows,
        output_dir / "adaptive_concurrency.csv",
    )

    # строим графики по собранным данным
    build_graphs(
        summary_rows,
        adaptive_rows,
        output_dir,
    )

    print("\nФайлы сохранены в папку:", output_dir)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument("--scenario", default="all")
    parser.add_argument("--repeats", type=int, default=2)
    parser.add_argument("--port", type=int, default=8010)
    parser.add_argument("--output", default="request_report")

    args = parser.parse_args()

    asyncio.run(
        make_requests(
            scenario=args.scenario,
            repeats=args.repeats,
            port=args.port,
            output=args.output,
        )
    )
