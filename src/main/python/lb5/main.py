from __future__ import annotations

import argparse
import asyncio

import uvicorn

from tester import run_tests


class Main:
    """"""

    def run(self) -> None:
        """"""
        parser = argparse.ArgumentParser()
        subparsers = parser.add_subparsers(dest="command")

        serve_parser = subparsers.add_parser("serve")
        serve_parser.add_argument("--host", default="127.0.0.1")
        serve_parser.add_argument("--port", type=int, default=8000)

        test_parser = subparsers.add_parser("test")
        test_parser.add_argument("--scenario", default="all")
        test_parser.add_argument("--repeats", type=int, default=10)
        test_parser.add_argument("--output", default="report")

        args = parser.parse_args()

        if args.command == "serve":
            uvicorn.run("app:app", host=args.host, port=args.port)

        elif args.command == "test":
            asyncio.run(run_tests(args.scenario, args.repeats, args.output))

        else:
            parser.print_help()


if __name__ == "__main__":
    app = Main()
    app.run()
