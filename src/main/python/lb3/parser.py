from __future__ import annotations

import re

from bs4 import BeautifulSoup, Tag

from models import PageParseResult, SectionStat


class Parser:
    STOP_SECTIONS = {
        "see also",
        "notes",
        "references",
        "external links",
        "further reading",
    }

    def parse(self, html: str, page_path: str) -> PageParseResult:
        soup = BeautifulSoup(html, "html.parser")

        article_root = self._get_article_root(soup)
        province = self._get_page_title(soup)

        if article_root is None:
            return PageParseResult(
                page_path=page_path,
                province=province,
                sections=[],
                error="Main article content not found",
            )

        headings = self._get_section_headings(article_root)
        sections: list[SectionStat] = []

        for index, heading in enumerate(headings):
            title = self._extract_heading_text(heading)
            if not title:
                continue

            if title.lower() in self.STOP_SECTIONS:
                break

            next_heading = headings[index + 1] if index + 1 < len(headings) else None
            images, tables, references = self._count_section_content(
                start_heading=heading,
                end_heading=next_heading,
            )

            sections.append(
                SectionStat(
                    province=province,
                    section=title,
                    images=images,
                    tables=tables,
                    references=references,
                )
            )

        return PageParseResult(
            page_path=page_path,
            province=province,
            sections=sections,
            error=None,
        )

    def _get_article_root(self, soup: BeautifulSoup) -> Tag | None:
        selectors = (
            "#mw-content-text > .mw-parser-output",
            "#mw-content-text > .mw-content-ltr.mw-parser-output",
            "main #mw-content-text > .mw-parser-output",
            ".mw-body-content #mw-content-text > .mw-parser-output",
        )

        for selector in selectors:
            node = soup.select_one(selector)
            if isinstance(node, Tag):
                return node

        for node in soup.select("div.mw-parser-output"):
            if node.select_one(".mw-heading2"):
                return node

        return None

    def _get_section_headings(self, article_root: Tag) -> list[Tag]:
        headings: list[Tag] = []
        seen: set[int] = set()

        for node in article_root.select(".mw-heading2"):
            if not isinstance(node, Tag):
                continue

            node_id = id(node)
            if node_id in seen:
                continue

            seen.add(node_id)
            headings.append(node)

        return headings

    def _get_page_title(self, soup: BeautifulSoup) -> str:
        h1 = soup.select_one("#firstHeading")
        if h1:
            return self._normalize_space(h1.get_text(" ", strip=True))

        title = soup.title.get_text(" ", strip=True) if soup.title else ""
        title = re.sub(r"\s*-\s*Wikipedia\s*$", "", title, flags=re.IGNORECASE)
        return self._normalize_space(title)

    def _normalize_space(self, text: str) -> str:
        return re.sub(r"\s+", " ", text).strip()

    def _extract_heading_text(self, heading_tag: Tag) -> str:
        h2 = heading_tag.find("h2")
        if h2 is not None:
            text = h2.get_text(" ", strip=True)
        else:
            headline = heading_tag.select_one(".mw-headline")
            text = headline.get_text(" ", strip=True) if headline else heading_tag.get_text(" ", strip=True)

        text = re.sub(r"\[\s*edit\s*\]\s*$", "", text, flags=re.IGNORECASE)
        return self._normalize_space(text)

    def _count_section_content(self, start_heading: Tag, end_heading: Tag | None) -> tuple[int, int, int]:
        images_seen: set[tuple[str | None, str | None, str | None, str | None]] = set()
        tables_seen: set[int] = set()
        refs_seen: set[tuple[str, str]] = set()

        node = start_heading.next_element

        while node is not None and node is not end_heading:
            if isinstance(node, Tag):
                if self._is_inside_heading(node):
                    node = node.next_element
                    continue

                if node.name == "img" and not self._is_non_content_image(node):
                    key = (
                        node.get("src") or node.get("data-src"),
                        node.get("data-file-width"),
                        node.get("data-file-height"),
                        node.get("alt"),
                    )
                    images_seen.add(key)

                elif node.name == "table" and not self._is_non_content_table(node):
                    tables_seen.add(id(node))

                elif node.name == "sup" and "reference" in (node.get("class") or []):
                    ref_key = (
                        node.get("id", ""),
                        self._normalize_space(node.get_text(" ", strip=True)),
                    )
                    refs_seen.add(ref_key)

            node = node.next_element

        return len(images_seen), len(tables_seen), len(refs_seen)

    def _is_inside_heading(self, node: Tag) -> bool:
        parent = node if "mw-heading2" in (node.get("class") or []) else node.parent

        while isinstance(parent, Tag):
            classes = set(parent.get("class") or [])
            if "mw-heading2" in classes:
                return True
            parent = parent.parent

        return False

    def _is_non_content_image(self, img: Tag) -> bool:
        classes = set(img.get("class") or [])
        if {"mw-logo-icon", "lazyload"} & classes:
            return True

        src = (img.get("src") or img.get("data-src") or "").lower()
        return (
                not src
                or "static/" in src
                or "wikipedia-wordmark" in src
                or "wikipedia-tagline" in src
                or "sprite" in src
                or "icon" in src
        )

    def _is_non_content_table(self, table: Tag) -> bool:
        classes = set(table.get("class") or [])
        return bool({"navbox", "vertical-navbox", "metadata", "ambox", "sidebar"} & classes)
