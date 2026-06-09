from __future__ import annotations

import asyncio
import random

from aiohttp import web

"""
API к которым эмулируется доступ
"""
SCENARIOS = {
    "stable": [
        {"name": "api1", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api2", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api3", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api4", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api5", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api6", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
    ],
    "slow": [
        {"name": "api1", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api2", "min_delay": 3.0, "max_delay": 5.0, "error_chance": 0.0},
        {"name": "api3", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api4", "min_delay": 1.5, "max_delay": 3.0, "error_chance": 0.0},
        {"name": "api5", "min_delay": 2.0, "max_delay": 6.0, "error_chance": 0.0},
        {"name": "api6", "min_delay": 0.1, "max_delay": 0.35, "error_chance": 0.0},
    ],
    "unstable": [
        {"name": "api1", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.0},
        {"name": "api2", "min_delay": 0.25, "max_delay": 0.5, "error_chance": 0.3},
        {"name": "api3", "min_delay": 0.15, "max_delay": 0.4, "error_chance": 0.5},
        {"name": "api4", "min_delay": 0.05, "max_delay": 0.15, "error_chance": 0.2},
        {"name": "api5", "min_delay": 0.1, "max_delay": 0.4, "error_chance": 0.8},
        {"name": "api6", "min_delay": 0.2, "max_delay": 0.5, "error_chance": 0.15},
    ],
}


async def make_answer(request: web.Request) -> web.Response:
    """
    методо для эмуляции отправики запроса по API

    """
    settings = request.app["settings"][request.match_info["name"]]

    delay = random.uniform(settings["min_delay"], settings["max_delay"])
    await asyncio.sleep(delay)

    if random.random() < settings["error_chance"]:
        status = random.choice([500, 503])
        return web.json_response(
            {"error": "fake server error", "api": settings["name"]},
            status=status,
        )

    return web.json_response(
        {
            "api": settings["name"],
            "delay_sec": round(delay, 3),
            "message": "ok",
        }
    )


async def start_fake_api(scenario: str, port: int) -> tuple[web.AppRunner, list[str]]:
    """
    стартер эмулятора

    """
    app = web.Application() # запускается отдельный процесс с эмулятором
    app["settings"] = {}

    for item in SCENARIOS[scenario]:
        app["settings"][item["name"]] = item

    app.router.add_get("/{name}", make_answer)

    runner = web.AppRunner(app)
    await runner.setup()

    site = web.TCPSite(runner, "127.0.0.1", port)
    await site.start()

    urls = []
    for item in SCENARIOS[scenario]:
        urls.append(f"http://127.0.0.1:{port}/{item['name']}")

    return runner, urls
