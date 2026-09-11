#!/usr/bin/env python3
"""Build the bundled English prefix lexicon from wordfreq's cBpack data.

Requires the pinned dependency in scripts/requirements-english-lexicon.txt.
"""

import argparse
import gzip
import hashlib
import re
from pathlib import Path

import msgpack

SOURCE_SHA256 = "f94a80cba6a3857b260d0666b5432bb7ea9b85315574dee9c306e87f61298247"
ASCII_WORD = re.compile(r"[A-Za-z]+\Z")


def build(source: Path, output: Path) -> None:
    source_bytes = source.read_bytes()
    actual_sha = hashlib.sha256(source_bytes).hexdigest()
    if actual_sha != SOURCE_SHA256:
        raise SystemExit(f"source SHA-256 {actual_sha} did not match {SOURCE_SHA256}")

    with gzip.open(source, "rb") as stream:
        packed = msgpack.load(stream, raw=False)
    if packed[0] != {"format": "cB", "version": 1}:
        raise SystemExit(f"unexpected cBpack header: {packed[0]!r}")

    words: list[str] = []
    seen: set[str] = set()
    for frequency_bucket in packed[1:]:
        for value in frequency_bucket:
            word = value.lower()
            if ASCII_WORD.fullmatch(word) and word not in seen:
                seen.add(word)
                words.append(word)

    if len(words) != 28_001:
        raise SystemExit(f"generated {len(words)} words; expected 28001")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(words) + "\n", encoding="ascii", newline="\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path, help="wordfreq small_en.msgpack.gz")
    parser.add_argument("output", type=Path, nargs="?", default=Path("app/src/main/assets/english-words.txt"))
    args = parser.parse_args()
    build(args.source, args.output)
