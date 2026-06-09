from __future__ import annotations

import csv
import json
from pathlib import Path


def save_json(data: object, path: str | Path) -> None:
    Path(path).write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def save_csv(rows: list[dict], path: str | Path) -> None:
    if not rows:
        return

    with Path(path).open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)
