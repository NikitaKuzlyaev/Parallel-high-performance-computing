from __future__ import annotations

from pydantic import BaseModel


class AggregateRequest(BaseModel):
    urls: list[str]
    strategy: str = "fixed"
    max_concurrent: int = 3
    timeout_sec: float = 5
