#!/usr/bin/env python3
"""Phase 1 static quality checks for Neuronova Apps.

No third-party dependencies are required. The checker validates repository
structure, merge-conflict markers, HTML basics, local links/resources, sitemap
integrity and core SEO metadata. JavaScript syntax is checked separately by
Node.js in the GitHub Actions workflow.
"""

from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_FILES = (
    "index.html",
    "favicon.svg",
    "privacy/index.html",
    "sitemap.xml",
    "README.md",
    ".nojekyll",
)
SKIP_DIRS = {".git", "node_modules", "build", "dist", ".gradle", ".idea", ".kotlin"}
TEXT_SUFFIXES = {".html", ".css", ".js", ".json", ".md", ".xml", ".yml", ".yaml", ".py", ".txt", ".svg"}
CONFLICT_RE = re.compile(r"(?m)^(<<<<<<< |>>>>>>> )")
CSS_URL_RE = re.compile(r"url\(\s*([^)]+?)\s*\)", re.IGNORECASE)

ERRORS: list[str] = []
HTML_CACHE: dict[Path, "HTMLInfo"] = {}


def error(message: str) -> None:
    ERRORS.append(message)


def iter_repo_files():
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.relative_to(ROOT).parts):
            continue
        yield path


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="strict")


class HTMLInfo(HTMLParser):
    def __init__(self, source: Path):
        super().__init__(convert_charrefs=True)
        self.source = source
        self.ids: set[str] = set()
        self.duplicate_ids: set[str] = set()
        self.refs: list[tuple[str, str]] = []
        self.meta_name: dict[str, str] = {}
        self.meta_property: dict[str, str] = {}
        self.canonical: str | None = None
        self.icons: list[str] = []
        self.tags: dict[str, int] = {}
        self._in_title = False
        self._title_parts: list[str] = []

    @property
    def title(self) -> str:
        return " ".join("".join(self._title_parts).split())

    def handle_starttag(self, tag: str, attrs):
        tag = tag.lower()
        self.tags[tag] = self.tags.get(tag, 0) + 1
        data = {key.lower(): value or "" for key, value in attrs}

        element_id = data.get("id")
        if element_id:
            if element_id in self.ids:
                self.duplicate_ids.add(element_id)
            self.ids.add(element_id)

        if tag == "title":
            self._in_title = True

        if tag == "meta":
            content = data.get("content", "").strip()
            name = data.get("name", "").strip().lower()
            prop = data.get("property", "").strip().lower()
            if name:
                self.meta_name[name] = content
            if prop:
                self.meta_property[prop] = content

        if tag == "link":
            href = data.get("href", "").strip()
            rel = {item.lower() for item in data.get("rel", "").split()}
            if href:
                self.refs.append(("href", href))
                if "canonical" in rel:
                    self.canonical = href
                if "icon" in rel or "shortcut" in rel:
                    self.icons.append(href)

        ref_attributes = {
            "a": "href",
            "script": "src",
            "img": "src",
            "source": "src",
            "iframe": "src",
            "audio": "src",
            "video": "src",
        }
        attribute = ref_attributes.get(tag)
        if attribute and data.get(attribute):
            self.refs.append((attribute, data[attribute].strip()))

        if tag == "video" and data.get("poster"):
            self.refs.append(("poster", data["poster"].strip()))

        srcset = data.get("srcset", "").strip()
        if srcset:
            for candidate in srcset.split(","):
                value = candidate.strip().split()[0] if candidate.strip() else ""
                if value:
                    self.refs.append(("srcset", value))

    def handle_endtag(self, tag: str):
        if tag.lower() == "title":
            self._in_title = False

    def handle_data(self, data: str):
        if self._in_title:
            self._title_parts.append(data)


def parse_html(path: Path) -> HTMLInfo:
    path = path.resolve()
    if path in HTML_CACHE:
        return HTML_CACHE[path]
    parser = HTMLInfo(path)
    try:
        parser.feed(read_text(path))
        parser.close()
    except Exception as exc:  # HTMLParser is permissive; decoding/parser failures are not.
        error(f"HTML parse error in {path.relative_to(ROOT)}: {exc}")
    HTML_CACHE[path] = parser
    return parser


def github_pages_context() -> tuple[str | None, str | None]:
    repository = os.getenv("GITHUB_REPOSITORY", "").strip()
    if "/" not in repository:
        return None, None
    owner, repo = repository.split("/", 1)
    return f"{owner}.github.io", f"/{repo}/"


PAGES_HOST, PAGES_PREFIX = github_pages_context()


def local_target(source: Path, reference: str) -> tuple[Path | None, str]:
    value = reference.strip().strip('"\'')
    if not value or value.startswith("#"):
        parsed = urlparse(value)
        return source, unquote(parsed.fragment)

    parsed = urlparse(value)
    if parsed.scheme in {"mailto", "tel", "data", "javascript"}:
        return None, ""

    if parsed.scheme in {"http", "https"} or parsed.netloc:
        if not (PAGES_HOST and PAGES_PREFIX and parsed.netloc == PAGES_HOST and parsed.path.startswith(PAGES_PREFIX)):
            return None, ""
        relative_path = unquote(parsed.path[len(PAGES_PREFIX):])
        target = ROOT / relative_path
    else:
        path_part = unquote(parsed.path)
        if not path_part:
            target = source
        elif path_part.startswith("/"):
            # Root-relative links point to the GitHub Pages account root, not necessarily this repo.
            return None, ""
        else:
            target = source.parent / path_part

    try:
        resolved = target.resolve()
        resolved.relative_to(ROOT)
    except ValueError:
        error(f"Reference escapes repository: {source.relative_to(ROOT)} -> {reference}")
        return None, ""

    if resolved.is_dir() or reference.split("?", 1)[0].split("#", 1)[0].endswith("/"):
        resolved = resolved / "index.html"

    return resolved, unquote(parsed.fragment)


def check_reference(source: Path, attribute: str, reference: str) -> None:
    target, fragment = local_target(source, reference)
    if target is None:
        return
    if not target.exists() or not target.is_file():
        error(f"Broken local {attribute} in {source.relative_to(ROOT)}: {reference}")
        return
    if fragment and target.suffix.lower() in {".html", ".htm"}:
        info = parse_html(target)
        if fragment not in info.ids:
            error(
                f"Missing fragment #{fragment} referenced from {source.relative_to(ROOT)} "
                f"to {target.relative_to(ROOT)}"
            )


def check_required_structure() -> None:
    for relative in REQUIRED_FILES:
        if not (ROOT / relative).exists():
            error(f"Missing required file: {relative}")


def check_conflict_markers() -> None:
    for path in iter_repo_files():
        if path.suffix.lower() not in TEXT_SUFFIXES and path.name != ".nojekyll":
            continue
        try:
            text = read_text(path)
        except UnicodeDecodeError:
            continue
        if CONFLICT_RE.search(text):
            error(f"Merge-conflict marker found in {path.relative_to(ROOT)}")


def check_html_files() -> None:
    for path in iter_repo_files():
        if path.suffix.lower() not in {".html", ".htm"}:
            continue
        info = parse_html(path)
        relative = path.relative_to(ROOT)
        for required_tag in ("html", "head", "body"):
            if not info.tags.get(required_tag):
                error(f"Missing <{required_tag}> in {relative}")
        if not info.title:
            error(f"Missing or empty <title> in {relative}")
        for duplicate in sorted(info.duplicate_ids):
            error(f"Duplicate id '{duplicate}' in {relative}")
        for attribute, reference in info.refs:
            check_reference(path, attribute, reference)


def check_css_resources() -> None:
    for path in iter_repo_files():
        if path.suffix.lower() != ".css":
            continue
        try:
            text = read_text(path)
        except UnicodeDecodeError:
            continue
        for match in CSS_URL_RE.finditer(text):
            value = match.group(1).strip().strip('"\'')
            if not value or value.startswith("#") or value.startswith("data:") or value.startswith("var("):
                continue
            check_reference(path, "CSS url()", value)


def require_meta(info: HTMLInfo, kind: str, key: str, label: str) -> None:
    mapping = info.meta_name if kind == "name" else info.meta_property
    if not mapping.get(key):
        error(f"Missing SEO metadata {label} in {info.source.relative_to(ROOT)}")


def check_seo() -> None:
    index_path = ROOT / "index.html"
    if not index_path.exists():
        return
    info = parse_html(index_path)
    if not info.title:
        error("Homepage is missing a title")
    require_meta(info, "name", "description", "meta description")
    for prop in ("og:site_name", "og:title", "og:description", "og:url", "og:image"):
        require_meta(info, "property", prop, prop)
    for name in ("twitter:card", "twitter:title", "twitter:description", "twitter:image"):
        require_meta(info, "name", name, name)
    if not info.canonical:
        error("Homepage is missing rel=canonical")
    if not info.icons:
        error("Homepage is missing a favicon link")

    if PAGES_HOST and PAGES_PREFIX and info.canonical:
        expected = f"https://{PAGES_HOST}{PAGES_PREFIX}"
        if info.canonical != expected:
            error(f"Homepage canonical mismatch: expected {expected}, found {info.canonical}")
        if info.meta_property.get("og:url") != expected:
            error(f"og:url mismatch: expected {expected}, found {info.meta_property.get('og:url', '')}")

    for image_key, mapping in (
        ("og:image", info.meta_property),
        ("twitter:image", info.meta_name),
    ):
        value = mapping.get(image_key, "")
        if value:
            check_reference(index_path, image_key, value)

    privacy = ROOT / "privacy" / "index.html"
    if privacy.exists():
        pinfo = parse_html(privacy)
        require_meta(pinfo, "name", "description", "privacy meta description")
        if not pinfo.canonical:
            error("privacy/index.html is missing rel=canonical")


def check_sitemap() -> None:
    sitemap = ROOT / "sitemap.xml"
    if not sitemap.exists():
        return
    try:
        tree = ET.parse(sitemap)
    except ET.ParseError as exc:
        error(f"Invalid sitemap.xml: {exc}")
        return

    locations = [
        (element.text or "").strip()
        for element in tree.iter()
        if element.tag.rsplit("}", 1)[-1] == "loc"
    ]
    locations = [loc for loc in locations if loc]
    if not locations:
        error("sitemap.xml contains no <loc> entries")
        return
    if len(locations) != len(set(locations)):
        error("sitemap.xml contains duplicate <loc> entries")
    for location in locations:
        if not location.startswith("https://"):
            error(f"Sitemap URL must use https: {location}")

    homepage = parse_html(ROOT / "index.html") if (ROOT / "index.html").exists() else None
    if homepage and homepage.canonical and homepage.canonical not in locations:
        error(f"Homepage canonical missing from sitemap.xml: {homepage.canonical}")

    privacy_path = ROOT / "privacy" / "index.html"
    if privacy_path.exists():
        privacy = parse_html(privacy_path)
        if privacy.canonical and privacy.canonical not in locations:
            error(f"Privacy canonical missing from sitemap.xml: {privacy.canonical}")


def main() -> int:
    check_required_structure()
    check_conflict_markers()
    check_html_files()
    check_css_resources()
    check_seo()
    check_sitemap()

    if ERRORS:
        print(f"\nQuality checks failed with {len(ERRORS)} issue(s):")
        for item in ERRORS:
            print(f"  - {item}")
        return 1

    html_count = sum(1 for path in iter_repo_files() if path.suffix.lower() in {".html", ".htm"})
    print(f"Quality checks passed: {html_count} HTML file(s), local links/resources, SEO and sitemap are consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
