from __future__ import annotations

from pathlib import Path

from aggregator import Aggregator
from fake_api import SCENARIOS, start_fake_api
from graphs import build_graphs
from stats import StatsStore
from utils import save_csv, save_json

# стратегии, которые нужно сравнить по заданию
STRATEGIES = ["fixed", "timeout_race", "adaptive"]


async def run_tests(scenario: str, repeats: int, output: str) -> None:
    """
    запуск тестовой системы

    тут мы:
    - запускаем локальные API-эмуляторы
    - прогоняем все стратегии
    - собираем статистику
    - сохраняем json/csv
    - строим графики
    """
    output_dir = Path(output)
    output_dir.mkdir(parents=True, exist_ok=True)

    if scenario == "all":
        scenarios = list(SCENARIOS.keys())
    else:
        scenarios = [scenario]

    # сюда сохраняем полные ответы агрегатора
    raw_results = []

    # сюда сохраняем краткую статистику для таблиц и графиков
    summary_rows = []

    # сюда сохраняем только статистику adaptive-стратегии
    adaptive_rows = []

    for scenario_name in scenarios:
        # у каждого сценария свой порт, чтобы серверы не мешали друг другу
        port = 9100 + list(SCENARIOS.keys()).index(scenario_name)

        runner, urls = await start_fake_api(scenario_name, port)

        try:
            print("\nСценарий:", scenario_name)

            for strategy in STRATEGIES:
                # для каждой стратегии создаём свой Store,
                # чтобы статистика не смешивалась между стратегиями
                stats = StatsStore()
                aggregator = Aggregator(stats)

                for repeat in range(1, repeats + 1):
                    result = await aggregator.aggregate(
                        urls=urls,
                        strategy=strategy,
                        max_concurrent=3,
                        timeout_sec=4,
                    )

                    summary = result["summary"]

                    raw_results.append(
                        {
                            "scenario": scenario_name,
                            "strategy": strategy,
                            "repeat": repeat,
                            "response": result,
                        }
                    )

                    # считаем количество таймаутов отдельно,
                    # чтобы потом можно было смотреть их в csv
                    timeouts = 0

                    for item in result["results"]:
                        if item.get("timeout"):
                            timeouts += 1

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

                    # adaptive дополнительно возвращает статистику по URL
                    if strategy == "adaptive":
                        for url, stat in result["adaptive_stats"].items():
                            adaptive_rows.append(
                                {
                                    "scenario": scenario_name,
                                    "repeat": repeat,
                                    "url": url,
                                    "success_rate": stat["success_rate"],
                                    "avg_ms": stat["avg_ms"],
                                    "concurrency": stat["adjusted_concurrency"],
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
            # после сценария обязательно останавливаем эмулятор
            await runner.cleanup()

    save_json(raw_results, output_dir / "raw_results.json")
    save_json(summary_rows, output_dir / "summary.json")
    save_csv(summary_rows, output_dir / "summary.csv")
    save_csv(adaptive_rows, output_dir / "adaptive_concurrency.csv")

    # графики строятся уже по собранным данным
    build_graphs(summary_rows, adaptive_rows, output_dir)

    print("\nГотово. Результаты сохранены в", output_dir)
