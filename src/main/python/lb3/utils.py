from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Callable


def save_json(data: object, path: str) -> None:
    Path(path).write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def compute_section_winners(rows: list[dict]) -> list[dict]:
    best_by_section: dict[str, dict] = {}

    for row in rows:
        section = row["section"]
        current = best_by_section.get(section)
        if current is None or row["total"] > current["total"]:
            best_by_section[section] = row

    winners = []
    for section, row in sorted(best_by_section.items()):
        winners.append(
            {
                "section": section,
                "province": row["province"],
                "images": row["images"],
                "tables": row["tables"],
                "references": row["references"],
                "total": row["total"],
            }
        )
    return winners


def benchmark(func):

    def wrap(*args, **kwargs):
        ts = time.time()

        res = func(*args, **kwargs)

        te = time.time() - ts
        print(te)

        return res

    return wrap
