from __future__ import annotations

from pathlib import Path

from mpi4py import MPI

from master_process import MasterProcess
from slave_process import SlaveProcess


class Main:
    def __init__(self, pages_dir: str):
        self.pages_dir = Path(pages_dir)

    def get_page_paths(self) -> list[str]:
        """
        Метод для парсинга папки с со страницами и формирования списка путей на эти страницы
        :return:
        """
        page_paths: list[str] = []

        for path in self.pages_dir.glob("*.html"):
            if path.is_file():
                #print(path)
                page_paths.append(str(path.resolve()))

        return sorted(page_paths)

    def run(self) -> None:
        """
        Метод, являющийся точкой входя для каждого процесса
        Тут определяется ранг процесса и определяется какую роль он будет выполнять
        :return:
        """
        comm = MPI.COMM_WORLD # шина коммуникации между процессами
        rank = comm.Get_rank() # ранг процесса

        if rank == 0:
            # если ранг 0 - то это "главный" процесс
            page_paths = self.get_page_paths()
            master = MasterProcess(comm=comm, page_paths=page_paths)
            master.run()

        else:
            # если ранг != 0, то это "рабочий" процесс
            slave = SlaveProcess(comm=comm, rank=rank)
            slave.run()


if __name__ == "__main__":
    base_dir = Path(__file__).parent
    pages_dir = base_dir / "web_pages" # ссылка на папку с веб-страницами

    app = Main(pages_dir=str(pages_dir))
    app.run()

# этой командой запускается
# mpiexec -n 4 python main.py