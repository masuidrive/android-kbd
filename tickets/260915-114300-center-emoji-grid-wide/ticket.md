---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "wide画面で絵文字7列を一覧領域の中央へ揃える"
created_at: "2026-09-15T11:43:00Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260915-114300-center-emoji-grid-wide

### Why
タブレット幅の絵文字レイヤーで、右側7列の絵文字が一覧領域の左へ偏り、右側だけ余白が大きく見える。Product Briefの「wideでも同じ操作体系」を、キー境界だけでなく一覧の視覚的な配分でも満たす。

### What / Acceptance Criteria
<!-- 完了を判定できる条件。プロダクトの観察可能な振る舞いだけを書く。
     読み手はこの ticket を承認する人であり、実装する agent ではない。

     箇条書きを書き始める前に、この節の冒頭へ 1 文を書く:
       「この ticket が終わると、〈誰〉が、いままでできなかった〈何〉をできるようになる」
     各 AC はその 1 文の分割として書く。非退行の AC だけは例外で、〈誰〉のみ必須。
     読めているかの判定は pdh-dev skill の「AC に書いてよいもの / 書いてはいけないもの」に従う。

     例: 「新しく登録したユーザが、一覧の画面に出る」
     例: 「画面幅 375px 以下でメニューがハンバーガーに切り替わる」

     プロセス要件 (レビュー済み、テストパス等) はここには書かない。
     ワークフロー (SKILL.md) と作業ノート (note) が保証する。

     runtime で UX/Security invariant を強制する ticket では、AC に「runtime enforce の
     保証メカニズム」を 1 行明記する (例: editor 警告だけでなく 422 reject されること)。 -->
この ticket が終わると、wide画面で絵文字を探す利用者が、左railを除いた一覧領域の中央へ均等に並ぶ7列を見られるようになる。

- [ ] AC 1: 840dp相当のwide画面で、各行の7個の絵文字セルが右側一覧領域へ均等に配分され、先頭側と末尾側の外側余白が視覚上同じになる。
- [ ] AC 2: 412dp相当のphone画面でも7列、4行、縦scroll、絵文字選択とvariation選択を従来どおり利用できる。
- [ ] AC 3: 左railの幅・touch境界・`あ`・`#!`・`19`・`AZ`、Recent初期表示、カテゴリ行、private欄、記号Backspaceの挙動は変わらない。
- [ ] AC 4: API 36.1の実AndroidX Emoji Pickerを含むInputMethodService画面で、wide時の中央揃えを観察できる。

### Architectural Invariants check
端末内完結、入力文字列非送信、小さな変換境界、大文字をモード化しないAI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 左railの外側ではなく、AndroidX pickerの右側body内部でセル配置を中央へ補正する。
- 固定pxの見た目合わせではなく、実際のbody幅と7列のセル幅から左右余白を算出する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- APK・GitHub Release・製品サイトの公開。ユーザ指示どおり本ticketではdeployしない。
- 絵文字収録内容、カテゴリ、Recent件数、キー高さ、左rail幅の変更。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
<!-- ユーザの明示指示、またはユーザが会話で言及した事項のみ書く (関数名 / module 名レベルまで)。
     設計判断は「Design Decisions」に書く。
     Coding Engineer は Implementation Notes が空でも実装できる責務を持つ。
     PM が自主的に実装詳細を書いてはならない (下流の自由度を奪う)。 -->

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
ユーザはv0.15.15のwide実画面を提示し、「これはdeployしなくていいけど、絵文字リストが左寄りなの直して」と明示した。
