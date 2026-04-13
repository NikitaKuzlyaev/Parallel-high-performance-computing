from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Optional


@dataclass
class SectionStat:
    """
    Модель для
    """
    province: str  # название провинции
    section: str  # название секции на веб-странице - заголовок h2
    images: int  # число изображений
    tables: int  # число таблиц
    references: int  # число ссылок

    @property
    def total(self) -> int:
        # свойство для вычисления суммы изображений + таблиц + ссылок
        return self.images + self.tables + self.references

    def to_dict(self) -> dict:
        # перевод структуры в словарь
        data = asdict(self)
        data["total"] = self.total
        return data


@dataclass
class PageParseResult:
    """
    Модель для результата парсинга целой страницы
    """
    page_path: str  # путь к странице на диске
    province: str  # название провинции
    sections: list[SectionStat]  # список по всем секциям страницы
    error: Optional[str] = None  # сюда пишется ошибка, если возникает
