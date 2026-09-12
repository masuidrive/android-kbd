---
priority: 2
base_branch: features/260912-020555-add-mozc-next-word-prediction
description: "Keep Setup below system bars and add a fixed settings app bar"
created_at: "2026-09-12T02:17:14Z"
started_at: 2026-09-12T02:48:38Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-021714-fix-setup-safe-area-and-app-bar

### Why
target SDK 36のedge-to-edge表示に対してSetup画面がsystem bar insetを処理しておらず、見出しがステータスバーへ入り込む。
他のAndroid設定画面と同じように固定トップアプリバーで画面の役割と戻る操作を示し、設定内容を安全領域内でスクロールできるようにする。
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
このticketが終わると、設定を開いた利用者が、ステータスバーやノッチに隠れないトップアプリバーから安全に設定を確認・終了できる。

- [x] AC 1: API 35/36の縦・横画面で、トップアプリバーと設定内容がステータスバー、display cutout、下端navigation barへ入り込まない。
- [x] AC 2: 画面上部に「masuidrive-kbd 設定」と戻るボタンを持つトップアプリバーが表示され、設定項目をスクロールしても上部へ固定される。
- [x] AC 3: 戻るボタンをタップするとSetup画面が終了し、Android標準の戻る操作も同じ結果になる。
- [x] AC 4: Lightでは暗いsystem bar icon、Darkでは明るいsystem bar iconが使われ、トップアプリバーと設定カードの文字を判読できる。
- [x] AC 5: 回転やsystem bar inset再配信後もpaddingが累積せず、既存switch、スラッシュ候補、マイク権限、入力テスト、バージョン表示の操作領域と保存動作を維持する。

### Architectural Invariants check
Setup画面のView階層とWindowInsetsだけを変更し、IME入力処理、権限範囲、端末内データの扱いを変えない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- target SDK 36のedge-to-edgeを前提に、`systemBars`と`displayCutout`のinsetをViewへ適用する。
- top insetと56dpトップアプリバーは固定領域、設定カードはその下のScrollViewとし、bottom insetはscroll終端へ適用する。
- 戻るボタンは48dp以上の操作領域を持ち、Activityをfinishする。
- system bar icon appearanceはLight/Dark themeに合わせて明示する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- キーボードViewのIME window inset・高さ変更。
- 設定項目の追加、削除、並び替え。
- Material Components依存の追加。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「設定画面が上のステータスバーにめり込んでる。トップナビバー的なのは入れたら？他のアプリの設定画面みたいに。」
現行`SetupActivity`はcontentへ24dp固定paddingだけを設定し、WindowInsets listenerがない。
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
