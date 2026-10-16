# Open-source code and data / 开源代码与数据

词映 Ciying 0.2.0 is distributed under GPL-3.0-or-later; see LICENSE. Source accompanies the APK in this project directory, including the data sources and reproducible conversion script. No Qingjian logo or application name is used as Ciying branding.

## Qingjian / 青简

Upstream: https://github.com/qingjian-team/qingjian

Pinned revision: c97ff2b61933d6fcd845ee4f4f470faf3ea83bf6 (retrieved 2026-10-16).

Architectural references: `crates/qingjian-core/src/parser/mod.rs`, `crates/qingjian-core/src/sentence/viterbi.rs`, and `crates/qingjian-dictionary/src/lib.rs`. The Android UI, SQLite adapter, asynchronous query integration and Kotlin unigram converter are Ciying code. Qingjian's Rust engine, desktop integrations, bigram tables and neural models are not bundled or ported in this version.

Bundled upstream data: `assets/lexicon/dict.tsv`, the eleven `assets/lexicon/dicts/*.tsv` domain dictionaries, and `assets/glossary/glossary-en.tsv`. Copyright and credit: Qingjian contributors and the original data providers listed below. Glossary tables are distributed by upstream under GPL-3.0-or-later. English translations are machine-generated and may be incomplete or inaccurate.

Modifications: merged dictionary files, normalized ASCII pinyin (ü/u: -> v), generated initials, merged duplicate word/pronunciation records using maximum frequency, joined English glosses, and generated indexed SQLite data. No online translation or input upload is used. Database and source checksums and exact counts are in `test-results/lexicon-build.json`; reproduce with `scripts/build_lexicon.py`.

## Data provenance and terms retained from upstream

- Chinese Wikipedia / Wikimedia contributors, https://dumps.wikimedia.org/zhwiki/ — source of upstream frequency statistics; CC BY-SA 4.0, https://creativecommons.org/licenses/by-sa/4.0/ . The transformed frequency-bearing lexicon remains subject to these attribution/share-alike terms; no endorsement is implied.
- LCCC / Tsinghua University Conversational AI, https://github.com/thu-coai/LCCC — MIT corpus used by upstream for frequency statistics.
- THUOCL / Tsinghua NLP Group, https://github.com/thunlp/THUOCL — domain vocabulary, MIT. Full notice retained in `licenses/THUOCL_LICENSE.txt`.
- Unicode Unihan — readings, Unicode License v3; full notice retained in `licenses/LICENSE-unicode.txt`.
- General Standard Chinese Characters table: official table transcribed by https://github.com/iDvel/The-Table-of-General-Standard-Chinese-Characters and cross-checked by shengdoushi; common word table transcribed by https://github.com/liuxilu/Proofread-Modern-Chinese-Common-Lexicon . Upstream notes these transcription repositories do not attach a separate license. This notice does not replace or assert additional rights to those sources.
- Rare-character readings include CNS data; provenance and notes are retained from upstream in `third_party/qingjian/LEXICON-BUILD.md` and `LEXICON-README.md`.

Full upstream data descriptions are retained under `third_party/qingjian/`. The GPL code license does not replace the data sources' individual terms. This project does not claim independently generated ownership of the imported data.

Local selected-word counts are stored on the device only (up to 2,000 entries) and are removed by clearing the app's data. Raw text and surrounding application content are not logged.

## Ciying 0.3.0 context and cover photo

`scripts/context_examples.txt` contains original, explicitly segmented Ciying examples (GPL-3.0-or-later). The offline context table combines weak lexical collocations inferred from the pinned word list and these examples; it is not Qingjian's trained bigram/neural model. Up to 2,000 word-frequency keys and an overall maximum of 4,000 frequency/context keys are stored locally. Password fields and editors requesting no personalized learning do not store new selections or read surrounding context.

Cover photo: “Fog over green forest in mountains”, Kieren Ridley / Pexels, photo 5407567. Source: https://www.pexels.com/photo/fog-over-green-forest-in-mountains-5407567/ . Downloaded 2026-10-16 from https://images.pexels.com/photos/5407567/pexels-photo-5407567.jpeg with Pexels CDN resizing/compression. Used as an app-homepage background with an app-drawn gradient and text. License: Pexels License, https://www.pexels.com/license/ (verified on retrieval date). This photo is a separately licensed visual asset, not GPL data and not part of the Ciying trademark or launcher icon. No photographer endorsement is implied. The launcher vector is original Ciying artwork under the project license.
