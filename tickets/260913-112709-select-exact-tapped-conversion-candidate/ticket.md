---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Commit the exact Japanese conversion candidate that the user taps"
created_at: "2026-09-13T11:27:09Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260913-112709-select-exact-tapped-conversion-candidate

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->

Product Briefの「迷いを減らし、少ないタップで入力できるキーボード」を満たすには、候補欄で選んだ文字列と確定結果が一致しなければならない。現在は「2本ね」を選んでも「日本猫」が確定される。

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
日本語変換を使う人が、候補欄でタップした候補をそのまま確定できるようになる。

- [ ] AC 1: 「にほんね」の候補欄で「2本ね」をタップすると「2本ね」が確定される
- [ ] AC 2: 先頭以外の日本語候補をタップしても、別の表示候補へずれない
- [ ] AC 3: 候補欄の横スクロール後も、タップした表示候補と確定結果が一致する

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- UIの表示順ではなく、各候補が保持するMozc candidate IDで選択する。
- 実Mozcで「にほんね」から「2本ね」を選ぶ経路を回帰確認する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 候補の生成内容や順位そのものの変更。
- キーボード高さとsystem navigation領域の修正。

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
日本語変換は端末内Mozcで完結し、offline入力とprivate fieldの候補非表示に矛盾しない。
