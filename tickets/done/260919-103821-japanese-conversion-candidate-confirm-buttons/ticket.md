---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "日本語変換中のSpaceを候補選択、選択後のEnterを確定としてv0.15.19へ公開する"
created_at: "2026-09-19T10:38:21Z"
started_at: 2026-09-19T10:39:22Z # Do not modify manually
closed_at: 2026-09-19T11:22:47Z # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260919-103821-japanese-conversion-candidate-confirm-buttons

### Why
日本語変換中もSpaceとEnterが通常入力の表示に見えるため、候補を選び確定する操作をキートップだけで判断しにくい。
Product Briefの「かな入力から候補の巡回・選択・確定までを対象アプリ上で完了できる」を、状態に合うボタン名で明確にする。

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
このticketが終わると、日本語を入力する利用者がSpaceで候補を選び、候補選択後にEnterで確定でき、その状態をキートップから判断できる。

- [x] AC 1: 日本語の読みを入力して候補が表示され、まだ候補を選んでいない間はSpaceが「候補」と表示され、タップすると候補を選択する。
- [x] AC 2: 候補を選択した後はEnterが「確定」と表示され、タップすると表示中の候補を入力欄へ確定する。
- [x] AC 3: 候補選択前のEnterは従来どおり「無変換」で、上・左フリックのカタカナ変換も維持する。候補選択後もSpaceの「候補」で次候補へ進める。
- [x] AC 4: native、操作mock、正本仕様、使い方ページが同じ状態遷移を示し、公開v0.15.19 APKと製品ページから利用できる。

### Architectural Invariants check
端末内変換と小さな変換interfaceを維持し、AI-1〜AI-4と矛盾しない。INTERNET permissionを追加しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 「候補を選んだ」はSpaceによる候補巡回後、変換engineが選択indexを返した状態とする。候補欄のタップは従来どおり即時確定する。
- 候補選択前はEnterを「無変換」、候補選択後は「確定」と表示する。Spaceは変換中を通して「候補」と表示し、次候補へ進める。
- 次版はv0.15.19、versionCode 35とし、GitHub Releaseは未圧縮APK 1個、製品ページも同版へ更新する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 変換候補の内容、Mozc学習、候補欄タップの即時確定、英字・数字・記号・音声・絵文字レイヤーの動作変更。

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
