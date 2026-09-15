---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "絵文字レイヤーをRecent起点の4行一覧と固定レイヤー切替列へ整える"
created_at: "2026-09-15T10:07:01Z"
started_at: 2026-09-15T10:09:12Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260915-100701-reshape-emoji-layer-grid

### Why
絵文字レイヤー内の削除キーと現在レイヤーを指す先頭キーが一覧領域を狭め、他の日本語系レイヤーと切替位置も揃っていない。Product Briefの6レイヤーを同じ操作体系で行き来しつつ、絵文字を4行すべてで探せる状態にする。

### What / Acceptance Criteria
この ticket が終わると、絵文字を入力する利用者がRecentから始め、他レイヤーと同じ位置の切替キーを残したまま4行分の一覧を使えるようになる。

- [ ] AC 1: 絵文字レイヤーを開くたび、カテゴリはRecentが選択され、一覧は先頭から表示される。
- [ ] AC 2: 絵文字レイヤー左端の4行は上から日本語、記号、テンキー、QWERTYへの切替となり、先頭に絵文字レイヤー自身へ移るキーは表示されない。
- [ ] AC 3: 左端切替列の表示幅とtouch境界は、日本語・テンキーレイヤー左端列と同じである。
- [ ] AC 4: 絵文字レイヤーに削除キーを表示せず、その位置を含む右側4行・7列を縦scrollする絵文字一覧として利用できる。
- [ ] AC 5: Recent最大100件、カテゴリ切替、絵文字入力、private欄でのRecent非表示・非保存、および他レイヤーの削除操作は従来どおり利用できる。
- [ ] AC 6: native、公開操作モック、正本仕様、技術資料、マニュアルが同じ配列と遷移を示す。

### Architectural Invariants check
端末内完結、入力文字列非送信、小さな変換境界、大文字をモード化しないAI-1〜AI-4と矛盾しない。

### Design Decisions
- 絵文字レイヤー進入は毎回Recentへ戻し、以前選んだカテゴリとscroll位置を復元しない。
- 左端の先頭は「日本語」とし、表示は日本語レイヤーへ移る既存の「あん」キー表現へ揃える。
- 削除キーを撤去した領域は余白や別controlにせず、絵文字一覧の4行目として使う。

### Out-of-scope
- 絵文字カテゴリや収録絵文字、Recent件数上限の変更。
- 日本語・テンキー・QWERTY・記号・音声レイヤーの削除キーや文字配列の変更。
- 新しい設定項目の追加。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザは「絵文字にバックスペースキーはいらない」「その代わり下まで」「開いたときは常にrecentタブ」「左のレイヤー切り替えキーの幅はかなレイヤーと同じ」「一番上はかなレイヤー。絵文字の代わりに」と明示した。

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
