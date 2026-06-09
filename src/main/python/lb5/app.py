from __future__ import annotations

from fastapi import FastAPI

from aggregator import Aggregator
from models import AggregateRequest
from stats import StatsStore

app = FastAPI(title="Async Gateway")

stats = StatsStore()
aggregator = Aggregator(stats)


@app.post("/aggregate")
async def aggregate(request: AggregateRequest) -> dict:
    """"""
    return await aggregator.aggregate(
        urls=request.urls,
        strategy=request.strategy,
        max_concurrent=request.max_concurrent,
        timeout_sec=request.timeout_sec,
    )
