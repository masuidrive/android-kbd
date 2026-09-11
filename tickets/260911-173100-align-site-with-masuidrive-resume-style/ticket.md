---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Align the local product-site header with the masuidrive.jp resume style"
created_at: "2026-09-11T17:31:00Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-173100-align-site-with-masuidrive-resume-style

### Why
製品サイト上部がmasuidrive本人のサイトと別のブランドに見える。
`resume.html`の名前表示とクマ画像を引き継ぎ、masuidrive製品だとひと目で分かるようにする。

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
このticketが終わると、閲覧者がローカル製品サイト上部からmasuidriveの製品だと認識し、クマ画像から本人サイトへ移動できる。

- [x] AC 1: トップ上部に`masuidrive-kbd`と`Android Flick Keyboard by masuidrive`が、参照resumeの名前表示に近い文字組みで表示される。
- [x] AC 2: 参照resumeと同じクマ画像が上部に残り、画像を押すと`https://masuidrive.jp/`へ移動する。
- [x] AC 3: 390pxと840pxで上部と実操作mockが横にはみ出さず、既存のLight/Darkとmock操作を利用できる。

### Architectural Invariants check
静的な製品ページの表現変更だけであり、IMEのAI-1〜AI-4を変更しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- `resume.html`のBootstrap構造は移さず、白基調、名前の字間、画像サイズ、余白のテイストを既存サイトへ適用する。
- 今回はローカル確認までとし、Sitesへは公開しない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 製品機能、キーボードmockの操作仕様、Android本体の変更。
- `resume.html`全体の複製。

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
