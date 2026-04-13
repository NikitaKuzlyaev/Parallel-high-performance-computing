from __future__ import annotations

from pathlib import Path


class PageLoader:
    """
    Этот класс занимается лишь тем, что читает файлы в текст по их пути
    """
    def load(self, page_path: str) -> str:
        path = Path(page_path)
        return path.read_text(encoding="utf-8", errors="ignore")
