from __future__ import annotations

from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt

STRATEGIES = ["fixed", "timeout_race", "adaptive"]


def build_graphs(summary_rows: list[dict], adaptive_rows: list[dict], output_dir: Path) -> None:
    """"""
    build_avg_time_graph(summary_rows, output_dir / "avg_time_by_strategy.png")
    build_adaptive_graph(adaptive_rows, output_dir / "adaptive_concurrency_unstable.png")
    build_success_graph(summary_rows, output_dir / "success_failed.png")


def build_avg_time_graph(rows: list[dict], path: Path) -> None:
    """"""
    grouped = defaultdict(list)

    for row in rows:
        key = row["scenario"] + " / " + row["strategy"]
        grouped[key].append(row["total_time_ms"])

    labels = []
    values = []

    for key in sorted(grouped):
        labels.append(key)
        values.append(sum(grouped[key]) / len(grouped[key]))

    plt.figure(figsize=(11, 5))
    plt.bar(labels, values)
    plt.title("Среднее время выполнения")
    plt.ylabel("мс")
    plt.xticks(rotation=45, ha="right")
    plt.tight_layout()
    plt.savefig(path)
    plt.close()


def build_adaptive_graph(rows: list[dict], path: Path) -> None:
    """"""
    grouped = defaultdict(list)

    for row in rows:
        if row["scenario"] == "unstable":
            grouped[row["url"]].append(row)

    plt.figure(figsize=(11, 5))

    for url, items in grouped.items():
        items = sorted(items, key=lambda item: item["repeat"])
        x = [item["repeat"] for item in items]
        y = [item["concurrency"] for item in items]
        label = url.split("/")[-1]
        plt.plot(x, y, marker="o", label=label)

    plt.title("Изменение concurrency у adaptive в нестабильном сценарии")
    plt.xlabel("номер повтора")
    plt.ylabel("concurrency")
    plt.legend()
    plt.tight_layout()
    plt.savefig(path)
    plt.close()


def build_success_graph(rows: list[dict], path: Path) -> None:
    """"""
    grouped = {}

    for row in rows:
        key = row["scenario"] + " / " + row["strategy"]

        if key not in grouped:
            grouped[key] = {"successful": 0, "failed": 0}

        grouped[key]["successful"] += row["successful"]
        grouped[key]["failed"] += row["failed"]

    labels = sorted(grouped)
    successful = [grouped[label]["successful"] for label in labels]
    failed = [grouped[label]["failed"] for label in labels]

    plt.figure(figsize=(11, 5))
    plt.bar(labels, successful, label="успешные")
    plt.bar(labels, failed, bottom=successful, label="ошибки/таймауты")
    plt.title("Успешные и неуспешные запросы")
    plt.ylabel("количество")
    plt.xticks(rotation=45, ha="right")
    plt.legend()
    plt.tight_layout()
    plt.savefig(path)
    plt.close()
