---
priority: 2
base_branch: features/260912-014204-enable-non-terminal-settings-by-default
description: "Keep keyboard height stable and add a height setting"
created_at: "2026-09-12T01:49:43Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-014943-fixed-keyboard-height

### Why
現在のキー行間隔は画面幅で変わるため、Foldを開くと同じ4行でもキーボード全体が高くなる。
IME初回表示やアプリ切替では幅・inset確定の順序によって高さが途中で変わり、入力欄を覆う量とキー位置が安定しない。
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
このticketが終わると、利用者がキーボードの高さを選び、Foldの開閉やアプリ切替後も同じ高さで入力できる。

- [ ] AC 1: 設定画面でキーボードの高さを「小・標準・大」から選択でき、選択値が再起動後も維持される。
- [ ] AC 2: 初期値「標準」では、スマホ幅・タブレット幅、Dual FlickのON/OFF、全レイヤーでキー4行の外形高が同じになる。
- [ ] AC 3: IME初回表示、アプリ切替、Foldの開閉、画面回転後に、設定した高さからキー位置とタップ領域がずれない。
- [ ] AC 4: 候補欄の高さ、キー間隔、ポップアップ、フリック判定、音声レイヤーの4行構成を維持する。
- [ ] AC 5: 設定値が壊れている場合は「標準」へ戻り、IMEが表示不能にならない。

### Architectural Invariants check
端末内SharedPreferencesだけに高さpresetを保存し、入力内容や外部通信の扱いは変更しない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 行高さは画面幅とDual Flickの列数から切り離し、「小・標準・大」の3段階とする。
- 初期値「標準」は現行スマホ表示と同じ4行高を基準にする。
- Dual Flickは列数だけを変え、高さを変えない。
- system bottom insetはキー行を伸ばさずsafe areaとして扱い、同じ端末姿勢内で遅れて反映されてもキー位置を動かさない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 候補欄自体の高さ変更。
- 5行以上のレイアウト、キーごとの個別高さ設定。
- Mozcの次単語予測。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「キーボードの高さは開いても変わらないようにして。固定でいい。設定画面で高さ指定できるといい。まだ最初の高さはバグる。」
添付画像ではFold内画面のDual Flickでキー領域が入力欄を大きく覆っている。
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
