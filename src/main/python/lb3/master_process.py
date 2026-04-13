from __future__ import annotations

from mpi4py import MPI

from utils import compute_section_winners, save_json, benchmark

# Флажки, которые одинаковы для master и slave, они определяют тип сообщения
TAG_TASK = 1
TAG_RESULT = 2
TAG_STOP = 3


class MasterProcess:
    def __init__(self, comm: MPI.Comm, page_paths: list[str]):
        self.comm = comm  # шина коммуникации
        self.page_paths = page_paths  # пути к файлам, которые нужно раздавать slave процессам
        self.world_size = comm.Get_size()  # число slave процессов

    @benchmark
    def run(self) -> None:
        # список рангов slave процессов
        workers = list(range(1, self.world_size))

        pending_paths = list(self.page_paths)  # пути, что надо раздавать
        active_workers = 0
        results: list[dict] = []  # результаты, которые потом получаем от slave процессов

        for worker_rank in workers:
            if pending_paths:
                page_path = pending_paths.pop(0)  # отдаем вначале каждому процессу по одному файлу
                self.comm.send(page_path, dest=worker_rank, tag=TAG_TASK)
                active_workers += 1

            else:
                # если путей меньше чем slave процессов - остальных останавливаем
                self.comm.send(None, dest=worker_rank, tag=TAG_STOP)

        while active_workers > 0:
            # пока есть активные slave процессы - даем им задачи

            status = MPI.Status()
            # ждем пока кто-то не прислал результат обработки файла
            result = self.comm.recv(source=MPI.ANY_SOURCE, tag=TAG_RESULT, status=status)

            # как только прислал - запоминаем, кто прислал
            worker_rank = status.Get_source()

            results.append(result)

            # теперь тому, кто прислал, даем новую задачу, так как он свободен
            if pending_paths:
                next_path = pending_paths.pop(0)
                self.comm.send(next_path, dest=worker_rank, tag=TAG_TASK)

            else:
                # если нечего выдать, то отпускаем slave процесс отдыхать
                self.comm.send(None, dest=worker_rank, tag=TAG_STOP)
                active_workers -= 1

        self._save_results(results)


    def _save_results(self, results: list[dict]) -> None:
        """
        Метод для агрегации результатов и сохранения в json
        """
        rows: list[dict] = []
        errors: list[dict] = []

        print("_save_results")

        for result in results:
            # print(result)
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
