from __future__ import annotations

from mpi4py import MPI

from page_loader import PageLoader
from parser import Parser

# Флажки, которые одинаковы для master и slave, они определяют тип сообщения
TAG_TASK = 1
TAG_RESULT = 2
TAG_STOP = 3


class SlaveProcess:
    def __init__(self, comm: MPI.Comm, rank: int):
        self.comm = comm  # шина коммуникации
        self.rank = rank  # ранг процесса
        self.page_loader = PageLoader()  # свой для каждого процесса загрузчик страниц
        self.parser = Parser()  # свой для каждого процесса парсер

    def run(self) -> None:

        while True:
            # Пока процесс жив - джем сообщения от master процесса (ранг = 0)
            status = MPI.Status()
            message = self.comm.recv(source=0, tag=MPI.ANY_TAG, status=status)

            tag = status.Get_tag()

            if tag == TAG_STOP:
                break

            if tag != TAG_TASK:
                continue

            # если мы тут - то надо парсить страницу
            page_path = message

            try:
                # запуск парсера и получение результата
                html = self.page_loader.load(page_path)
                result = self.parser.parse(html=html, page_path=page_path)
            except Exception as exc:
                # если ошибка - пустая модель с указанием ошибки
                result = {
                    "page_path": page_path,
                    "province": "",
                    "sections": [],
                    "error": f"{type(exc).__name__}: {exc}",
                }
            else:
                # если ошибки нет - собираем модель
                result = {
                    "page_path": result.page_path,
                    "province": result.province,
                    "sections": [s.to_dict() for s in result.sections],
                    "error": result.error,
                }

            # в любом случае докладываем результат обработки master процессу
            self.comm.send(result, dest=0, tag=TAG_RESULT)
