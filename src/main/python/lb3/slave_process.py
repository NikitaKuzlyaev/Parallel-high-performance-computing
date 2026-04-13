from __future__ import annotations

from mpi4py import MPI

from page_loader import PageLoader
from parser import Parser

TAG_TASK = 1
TAG_RESULT = 2
TAG_STOP = 3


class SlaveProcess:
    def __init__(self, comm: MPI.Comm, rank: int):
        self.comm = comm
        self.rank = rank
        self.page_loader = PageLoader()
        self.parser = Parser()

    def run(self) -> None:

        while True:
            status = MPI.Status()
            message = self.comm.recv(source=0, tag=MPI.ANY_TAG, status=status)

            tag = status.Get_tag()

            if tag == TAG_STOP:
                break

            if tag != TAG_TASK:
                continue

            page_path = message

            try:
                html = self.page_loader.load(page_path)
                result = self.parser.parse(html=html, page_path=page_path)
            except Exception as exc:
                result = {
                    "page_path": page_path,
                    "province": "",
                    "sections": [],
                    "error": f"{type(exc).__name__}: {exc}",
                }
            else:
                result = {
                    "page_path": result.page_path,
                    "province": result.province,
                    "sections": [s.to_dict() for s in result.sections],
                    "error": result.error,
                }

            self.comm.send(result, dest=0, tag=TAG_RESULT)
