---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "テンキー小数点と記号切替表示の修正版をv0.15.20として公開する"
created_at: "2026-09-20T07:59:12Z"
started_at: 2026-09-20T08:00:04Z # Do not modify manually
closed_at: 2026-09-20T08:16:51Z # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260920-075912-publish-v0-15-20

### Why
検証済みのテンキー小数点左右フリックと`?}`記号切替表示を、利用者がAPKと製品ページから取得できる状態にする。

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
このticketが終わると、利用者がv0.15.20 APKを直接取得でき、製品ページと操作モックで同じ変更を確認できる。

- [x] AC 1: GitHub public mainにv0.15.20の実装と公開記録があり、GitHub ReleaseからZIPでないAPK 1個を直接取得できる。
- [x] AC 2: 公開APKがlocal成果物とbyte一致し、versionCode 36、versionName 0.15.20、arm64-v8a、RECORD_AUDIOあり、INTERNETなしである。
- [x] AC 3: 製品ページ、操作mock、マニュアルがv0.15.20と今回の割当を示し、412px・840pxで横にはみ出さない。

### Architectural Invariants check
公開物だけを更新し、端末内処理とAI-1〜AI-4を維持する。INTERNET permissionを追加しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- versionCode 36、versionName 0.15.20とする。
- Release assetは`gesture-ime-v0.15.20.apk` 1個とし、ZIPを作らない。
- 製品ページは`masuidrive.jp/products/md-kbd/`へ反映する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 新しい入力機能、既存キー割当の追加変更。

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
