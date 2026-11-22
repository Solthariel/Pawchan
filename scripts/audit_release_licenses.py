#!/usr/bin/env python3
"""Build third-party notices from a resolved Gradle graph; missing licenses fail closed.

Run :app:exportReleaseDependencies first. This records declared licensing and
embedded attribution, not legal clearance of repository history or binary behavior.
"""
import argparse
import concurrent.futures
import hashlib
import json
from pathlib import Path
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
REPOSITORIES = ("https://dl.google.com/dl/android/maven2/", "https://repo.maven.apache.org/maven2/")


def parse_pom(data):
    """Return only literal POM metadata; do not execute properties or remote code."""
    root = ET.fromstring(data)
    licenses = [
        {"name": (item.findtext("m:name", "", NS)).strip(),
         "url": (item.findtext("m:url", "", NS)).strip()}
        for item in root.findall("m:licenses/m:license", NS)
    ]
    parent = root.find("m:parent", NS)
    inherited = None
    if parent is not None:
        parts = [parent.findtext(f"m:{key}", "", NS).strip() for key in ("groupId", "artifactId", "version")]
        if all(parts) and not any("${" in part for part in parts):
            inherited = ":".join(parts)
    return licenses, inherited


def pom_bytes(coordinate, cache, gradle_cache):
    group, artifact, version = coordinate.split(":")
    local = gradle_cache / group / artifact / version
    candidates = list(local.glob(f"*/{artifact}-{version}.pom"))
    if candidates:
        data = candidates[0].read_bytes()
        return data, "resolved Gradle POM"
    relative = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
    saved = cache / (hashlib.sha256(coordinate.encode()).hexdigest() + ".pom")
    if saved.exists():
        return saved.read_bytes(), relative
    for base in REPOSITORIES:
        try:
            with urllib.request.urlopen(base + relative, timeout=45) as response:
                data = response.read(4 * 1024 * 1024)
            parse_pom(data)
            saved.write_bytes(data)
            return data, base + relative
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
    raise ValueError(f"No POM found for {coordinate}")


def declared_licenses(coordinate, cache, gradle_cache, seen=()):
    if coordinate in seen or len(seen) >= 8:
        raise ValueError(f"Cyclic or excessive POM inheritance: {coordinate}")
    data, source = pom_bytes(coordinate, cache, gradle_cache)
    licenses, parent = parse_pom(data)
    if licenses and all(item["name"] and "${" not in item["name"] for item in licenses):
        return {"coordinate": coordinate, "licenses": licenses, "license_pom": source,
                "pom_sha256": hashlib.sha256(data).hexdigest()}
    if parent:
        record = declared_licenses(parent, cache, gradle_cache, seen + (coordinate,))
        return dict(record, coordinate=coordinate, inherited_from=parent)
    raise ValueError(f"No declared license for {coordinate}")


def embedded_notices(coordinate, gradle_cache):
    group, artifact, version = coordinate.split(":")
    notices = []
    for file in sorted((gradle_cache / group / artifact / version).glob("*/*")):
        if file.suffix not in (".jar", ".aar") or file.name.endswith("-sources.jar"):
            continue
        with zipfile.ZipFile(file) as archive:
            for name in sorted(archive.namelist()):
                leaf = name.rsplit("/", 1)[-1].upper()
                if name.endswith("/") or not leaf.startswith(("LICENSE", "NOTICE", "COPYING")):
                    continue
                info = archive.getinfo(name)
                if info.file_size <= 512 * 1024:
                    notices.append({"artifact": file.name, "path": name,
                                    "text": archive.read(name).decode("utf-8", errors="replace")})
    return notices


def audit(coordinates, cache, gradle_cache):
    def inspect(coordinate):
        record = declared_licenses(coordinate, cache, gradle_cache)
        record["embedded_notices"] = embedded_notices(coordinate, gradle_cache)
        return record
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as workers:
        return list(workers.map(inspect, sorted(set(coordinates))))


def write_notices(records, output):
    lines = ["Pawchan: resolved release third-party notices", "",
             "Licenses below are declared by upstream POMs; preserved embedded notices follow.",
             "Platform/BOM entries may have no runtime binary. Root GPL and native licenses also apply.", ""]
    for record in records:
        lines.extend([record["coordinate"], "Licensing source: " + record["license_pom"]])
        for license_info in record["licenses"]:
            lines.append(license_info["name"] + " — " + license_info["url"])
        for notice in record["embedded_notices"]:
            lines.extend([f"--- {notice['artifact']} / {notice['path']} ---", notice["text"]])
        lines.append("")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("coordinates", type=Path)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--notices", type=Path, required=True)
    parser.add_argument("--gradle-cache", type=Path, default=Path.home() / ".gradle/caches/modules-2/files-2.1")
    args = parser.parse_args()
    args.cache.mkdir(parents=True, exist_ok=True)
    coordinates = [line.strip() for line in args.coordinates.read_text().splitlines() if line.strip()]
    if not coordinates:
        raise ValueError("Empty resolved dependency inventory")
    records = audit(coordinates, args.cache, args.gradle_cache)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n")
    write_notices(records, args.notices)
    print(f"Declared licenses found for {len(records)} resolved components; notices preserved.")


if __name__ == "__main__":
    main()
