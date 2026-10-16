"""Build an offline Android SQLite lexicon from the pinned Qingjian TSV sources.

No network access or third-party Python dependencies required. See THIRD_PARTY.md.
"""
import hashlib
import json
from pathlib import Path
import re
import sqlite3
import os
import shutil
import tempfile
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "third_party" / "qingjian"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "lexicon-v3.db"


def build():
    glosses = {}
    for line in (SOURCE / "glossary-en.tsv").read_text(encoding="utf-8-sig").splitlines():
        if not line or line.startswith("#"):
            continue
        word, *meanings = line.split("\t")
        glosses[word] = "; ".join(dict.fromkeys(meanings))
    entries = {}
    sources = [SOURCE / "lexicon" / "dict.tsv", *sorted((SOURCE / "lexicon" / "dicts").glob("*.tsv"))]
    for source in sources:
        for line in source.read_text(encoding="utf-8-sig").splitlines():
            if not line or line.startswith("#"):
                continue
            fields = line.split("\t")
            if len(fields) < 3:
                continue
            word, pronunciation, frequency = fields[:3]
            syllables = pronunciation.lower().replace("u:", "v").replace("ü", "v").split()
            if not syllables or any(not re.fullmatch("[a-zv]+", s) for s in syllables):
                continue
            key = "".join(syllables)
            freq = max(0, int(frequency))
            row = (key, " ".join(syllables), "".join(s[0] for s in syllables), word, freq, glosses.get(word, ""))
            identity = (key, word)
            if identity not in entries or entries[identity][4] < freq:
                entries[identity] = row
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    local_cache = Path(os.environ.get("LOCALAPPDATA", tempfile.gettempdir())) / "Ciying" / "lexicon-build"
    local_cache.mkdir(parents=True, exist_ok=True)
    temp = local_cache / "lexicon-v2.building"
    if temp.exists():
        temp.unlink()
    db = sqlite3.connect(temp)
    db.execute("PRAGMA page_size=4096")
    db.execute("CREATE TABLE lexicon (key TEXT NOT NULL, pinyin TEXT NOT NULL, initials TEXT NOT NULL, word TEXT NOT NULL, frequency INTEGER NOT NULL, gloss TEXT NOT NULL)")
    db.executemany("INSERT INTO lexicon VALUES (?,?,?,?,?,?)", sorted(entries.values()))
    db.execute("CREATE INDEX lexicon_key ON lexicon(key,frequency DESC)")
    db.execute("CREATE INDEX lexicon_initials ON lexicon(initials,frequency DESC)")
    db.execute("CREATE INDEX lexicon_word ON lexicon(word)")
    # Lexical collocations are weaker evidence than the deliberately segmented examples.
    words = {row[3]: row[4] for row in entries.values()}
    pairs = Counter()
    for word, frequency in words.items():
        if 2 <= len(word) <= 8 and frequency >= 20:
            for split in range(1, len(word)):
                left, right = word[:split], word[split:]
                if left in words and right in words:
                    pairs[left, right] += min(100, max(1, frequency // 100))
    for line in (ROOT / "scripts" / "context_examples.txt").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        tokens = line.split()
        for left, right in zip(tokens, tokens[1:]):
            pairs[left, right] += 1000
    db.execute("CREATE TABLE context_pairs (previous TEXT NOT NULL, next TEXT NOT NULL, weight INTEGER NOT NULL, PRIMARY KEY(previous,next)) WITHOUT ROWID")
    db.executemany("INSERT INTO context_pairs VALUES (?,?,?)", [(left, right, count) for (left, right), count in sorted(pairs.items())])
    db.execute("PRAGMA user_version=3")
    db.commit()
    assert db.execute("PRAGMA integrity_check").fetchone()[0] == "ok"
    tuoxie = db.execute("SELECT word,gloss FROM lexicon WHERE key='tuoxie' ORDER BY frequency DESC").fetchall()
    assert {"拖鞋", "妥协"}.issubset({word for word, _ in tuoxie}), tuoxie
    assert any(word == "拖鞋" and "slippers" in gloss for word, gloss in tuoxie)
    for key, word in [("nihao", "你好"), ("xuexi", "学习"), ("yingyu", "英语"), ("chongqing", "重庆"), ("lvse", "绿色")]:
        assert db.execute("SELECT 1 FROM lexicon WHERE key=? AND word=?", (key, word)).fetchone(), (key, word)
    summary = {
        "upstream_commit": "c97ff2b61933d6fcd845ee4f4f470faf3ea83bf6",
        "entries": len(entries),
        "with_english_gloss": sum(bool(row[5]) for row in entries.values()),
        "context_pairs": len(pairs),
        "tuoxie": tuoxie,
        "source_sha256": {str(p.relative_to(ROOT)).replace("\\", "/"): hashlib.sha256(p.read_bytes()).hexdigest() for p in [*sources, SOURCE / "glossary-en.tsv", ROOT / "scripts" / "context_examples.txt"]},
    }
    db.close()
    packaged = OUTPUT.with_suffix(".building")
    shutil.copyfile(temp, packaged)
    packaged.replace(OUTPUT)
    summary["database_sha256"] = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    (ROOT / "test-results" / "lexicon-build.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in summary.items() if k != "source_sha256"}, ensure_ascii=False))


if __name__ == "__main__":
    build()
