from __future__ import annotations

from mpi4py import MPI

from utils import compute_section_winners, save_json, benchmark

TAG_TASK = 1
TAG_RESULT = 2
TAG_STOP = 3


class MasterProcess:
    def __init__(self, comm: MPI.Comm, page_paths: list[str]):
        self.comm = comm
        self.page_paths = page_paths
        self.world_size = comm.Get_size()

    @benchmark
    def run(self) -> None:
        workers = list(range(1, self.world_size))

        pending_paths = list(self.page_paths)
        active_workers = 0
        results: list[dict] = []

        for worker_rank in workers:
            if pending_paths:
                page_path = pending_paths.pop(0)
                self.comm.send(page_path, dest=worker_rank, tag=TAG_TASK)
                active_workers += 1

            else:
                self.comm.send(None, dest=worker_rank, tag=TAG_STOP)

        while active_workers > 0:

            status = MPI.Status()
            result = self.comm.recv(source=MPI.ANY_SOURCE, tag=TAG_RESULT, status=status)
            worker_rank = status.Get_source()

            results.append(result)

            if pending_paths:
                next_path = pending_paths.pop(0)
                self.comm.send(next_path, dest=worker_rank, tag=TAG_TASK)

            else:
                self.comm.send(None, dest=worker_rank, tag=TAG_STOP)
                active_workers -= 1

        self._save_results(results)

    def _save_results(self, results: list[dict]) -> None:
        rows: list[dict] = []
        errors: list[dict] = []

        print("_save_results")

        for result in results:
            #print(result)
            if result.get("error"):
                errors.append(
                    {
                        "page_path": result["page_path"],
                        "error": result["error"],
                    }
                )
                continue

            for section in result["sections"]:
                row = dict(section)
                row["page_path"] = result["page_path"]
                rows.append(row)

        winners = compute_section_winners(rows)

        save_json(rows, "section_stats.json")
        save_json(winners, "section_winners.json")

        if errors:
            save_json(errors, "errors.json")
