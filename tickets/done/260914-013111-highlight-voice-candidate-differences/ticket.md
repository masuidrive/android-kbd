---
priority: 2
base_branch: features/260913-174108-adjust-symbol-tab-and-katakana-flick
description: "音声認識候補どうしで異なる文字を強調し、似た全文候補を判別しやすくする"
created_at: "2026-09-14T01:31:11Z"
started_at: 2026-09-14T01:32:54Z # Do not modify manually
closed_at: 2026-09-14T05:41:04Z # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260914-013111-highlight-voice-candidate-differences

### Why
端末内音声認識が返す全文候補は、助詞や漢字表記だけが違う場合がある。現在は全候補を同じ書体で表示するため違いを見つけにくく、選択時に全文を読み比べる必要がある。Product Briefの、レイヤーを行き来しながら素早く入力する目的につなげる。

### What / Acceptance Criteria
音声入力を使う人が、よく似た全文候補の違う部分をひと目で見分けて選べるようになる。
- [x] AC 1: 複数の音声認識候補が表示されたとき、候補間で異なる文字だけがアクセント色と太字で表示される。
- [x] AC 2: 全文が同じ候補では文字が強調されず、候補が1件だけの場合も通常の表示になる。
- [x] AC 3: 強調表示された候補をタップすると、その候補の全文だけが入力され、音声認識が再開する。
- [x] AC 4: Androidネイティブと製品ページの操作mockで、同じ候補差分が強調される。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。認識結果の比較と表示は端末内で行い、入力文字列を保存・送信しない。

### Design Decisions
- 候補全文は維持し、候補すべてに共通する文字は通常表示、いずれかの候補と異なる文字はアクセント色と太字にする。
- 文字の挿入・削除がある候補でも、共通文字まで広い範囲で強調しないよう文字単位の並びを比較する。

### Out-of-scope
- 音声認識エンジンが返す候補の並び替え・正規化・削除。
- 音声候補以外のMozc変換候補や英字予測候補の表示変更。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザは、ほぼ同じ3つの音声候補について差が分からないと指摘し、候補間で異なる部分を強調する案を承認した。

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
