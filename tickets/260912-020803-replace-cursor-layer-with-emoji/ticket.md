---
priority: 2
base_branch: features/260912-112512-align-candidate-strip-spacing
description: "Replace the cursor layer with a recent-first emoji layer"
created_at: "2026-09-12T02:08:03Z"
started_at: 2026-09-12T04:08:34Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-020803-replace-cursor-layer-with-emoji

### Why
Spaceフリックと日本語左列でカーソル移動できるため、専用カーソルレイヤーの利用価値が重複している。
空いた入口を絵文字へ使い、最近使ったものをすぐ再入力しながら、ほかの絵文字もキーボード内で選べるようにする。
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->

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
このticketが終わると、利用者が従来のカーソルレイヤー入口から絵文字を開き、最近使った絵文字と一覧から選んで入力できる。

- [ ] AC 1: 日本語・テンキー左上の従来カーソルレイヤーキーをタップすると、同じ4行高の絵文字レイヤーが開く。
- [ ] AC 2: 絵文字レイヤー上部には端末内に保存した最近使った絵文字が新しい順に重複なく表示され、タップで直接入力できる。
- [ ] AC 3: 最近使った絵文字の下には絵文字一覧が複数行で並び、キーボードの高さを変えずにページまたはカテゴリを移動して選べる。
- [ ] AC 4: 絵文字を入力すると最近使った一覧の先頭へ移り、IMEを再表示しても順序が維持される。
- [ ] AC 5: 専用カーソルレイヤーは表示経路と保存済み最終レイヤーから削除されるが、Spaceフリックと既存の直接カーソルキーによる上下左右移動は維持される。
- [ ] AC 6: 絵文字レイヤーから日本語・QWERTY・テンキー・音声へ既存のレイヤーフリックで移動できる。

### Architectural Invariants check
絵文字一覧と利用履歴はAPK内データと端末内SharedPreferencesだけで扱い、入力内容や履歴を外部へ送らない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 現在の`↔`キーを絵文字入口へ置き換え、`KeyboardMode.CURSOR`を製品レイヤーから削除する。
- 1行目を最近使った絵文字、残りをページまたはカテゴリで切り替える一覧にする。
- 絵文字は候補欄ではなくキーとして表示し、タップ時に直接確定する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 絵文字の検索、肌色・性別variation picker、GIF、スタンプ。
- Android個人辞書やMozc候補への絵文字学習統合。
- Spaceフリックのカーソル操作廃止。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「カーソルレイヤー削除して、絵文字レイヤーいれるのがいいかなー。上の方に最後に使ったやつが出て。あとは並ぶやつ。」
<!-- ユーザの明示指示、またはユーザが会話で言及した事項のみ書く (関数名 / module 名レベルまで)。
     設計判断は「Design Decisions」に書く。
     Coding Engineer は Implementation Notes が空でも実装できる責務を持つ。
     PM が自主的に実装詳細を書いてはならない (下流の自由度を奪う)。 -->

### Dependencies
`260912-112512-align-candidate-strip-spacing`。固定高さと候補欄の外周余白が揃った後で、絵文字一覧のページングを追加する。
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
