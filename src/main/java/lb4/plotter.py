import json
from pathlib import Path

import matplotlib.pyplot as plt


RESULTS_FILE = Path("output/results.json")
PLOTS_DIR = Path("output/plots")


def read_results():
    with open(RESULTS_FILE, "r", encoding="utf-8") as file:
        data = json.load(file)

    return data["results"]


def group_by_strategy(results):
    grouped = {}

    for row in results:
        strategy = row["strategy"]

        if strategy not in grouped:
            grouped[strategy] = []

        grouped[strategy].append(row)

    for strategy in grouped:
        grouped[strategy].sort(key=lambda x: x["botSpawnChance"])

    return grouped


def plot_average_steps(grouped):
    plt.figure(figsize=(9, 5))

    for strategy, rows in grouped.items():
        chances = [row["botSpawnChance"] for row in rows]
        steps = [row["averageSteps"] for row in rows]

        plt.plot(chances, steps, marker="o", label=strategy)

    plt.title("Среднее количество шагов доставщика")
    plt.xlabel("Вероятность появления бота")
    plt.ylabel("Среднее количество шагов")
    plt.grid(True)
    plt.legend()
    plt.tight_layout()

    plt.savefig(PLOTS_DIR / "average_steps.png", dpi=200)
    plt.close()


def plot_success_rate(grouped):
    plt.figure(figsize=(9, 5))

    for strategy, rows in grouped.items():
        chances = [row["botSpawnChance"] for row in rows]
        success_rate = [row["successRate"] for row in rows]

        plt.plot(chances, success_rate, marker="o", label=strategy)

    plt.title("Вероятность успешной доставки")
    plt.xlabel("Вероятность появления бота")
    plt.ylabel("Вероятность успеха")
    plt.ylim(0, 1.05)
    plt.grid(True)
    plt.legend()
    plt.tight_layout()

    plt.savefig(PLOTS_DIR / "success_rate.png", dpi=200)
    plt.close()


def main():
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    results = read_results()
    grouped = group_by_strategy(results)

    plot_average_steps(grouped)
    plot_success_rate(grouped)

    print(PLOTS_DIR / "average_steps.png")
    print(PLOTS_DIR / "success_rate.png")


if __name__ == "__main__":
    main()