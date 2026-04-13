from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Optional


@dataclass
class SectionStat:
    province: str
    section: str
    images: int
    tables: int
    references: int

    @property
    def total(self) -> int:
        return self.images + self.tables + self.references

    def to_dict(self) -> dict:
        data = asdict(self)
        data["total"] = self.total
        return data


@dataclass
class PageParseResult:
    page_path: str
    province: str
    sections: list[SectionStat]
    error: Optional[str] = None

    def to_rows(self) -> list[dict]:
        rows: list[dict] = []
        for section in self.sections:
            row = section.to_dict()
            row["page_path"] = self.page_path
            rows.append(row)
        return rows
