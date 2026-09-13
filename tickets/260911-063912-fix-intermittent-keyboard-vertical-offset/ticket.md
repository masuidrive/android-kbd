---
priority: 2
base_branch: features/260911-063048-unify-four-row-keyboard-heights
description: "Fix intermittent vertical keyboard offset after entering an app"
created_at: "2026-09-11T06:39:12Z"
started_at: 2026-09-11T07:00:01Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-063912-fix-intermittent-keyboard-vertical-offset

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->
アプリの入力欄へ入った際、IME上部の空きが大きくなりキー群が下へずれて見えることがある。起動や入力欄切替に左右されず、安定したキー位置で入力できるようにする。

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
この ticket が終わると、Gesture IME利用者がアプリへ入った直後やIMEを再表示したときも、キー群を同じ縦位置で利用できる。

- [x] AC 1: 通常入力欄を初めてfocusしたとき、候補なしのかな4行が規定位置へ表示され、候補欄上部に過大な空白が生じない。
- [x] AC 2: QWERTY/かなを最後のレイヤーとして保存した各状態で、IME hide→showと別入力欄への切替後もキー領域の上端・下端が同じ位置を保つ。
- [x] AC 3: 候補なし、かな候補あり、英字候補あり、音声状態表示の各状態で、候補欄の規定高以外にキー位置が動かない。
- [x] AC 4: 端末bottom inset、外画面/内画面、画面回転またはwindow再計測後も、hit targetと描画位置が一致する。
- [x] AC 5: 高さをまだ選択していない利用者には「大」の4行が初回から表示され、IME再生成やアプリ切替後も「標準」へ縮まない。設定で「小」「標準」「大」を明示選択した場合は、その選択が維持される。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->
IME lifecycleとlayoutだけを修正し、入力内容の保存・外部送信を増やさずAI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 添付再現画像を`docs/verification/intermittent-keyboard-vertical-offset.jpg`へ保存した。
- IME生成時の保存レイヤー復元、CandidateStrip固定高、KeyboardViewの`onMeasure`/`requestLayout`、bottom inset適用順を観測して原因を特定してから修正する。
- v0.15.4の実機確認で最下段がOSナビゲーション領域へ潜り込んだため、高い方の4行を規定位置とし、候補表示前後の実スクリーンショットで確認する。
- v0.15.7実機画像ではbottom inset位置は安定していた一方、未選択時の高さが「標準」へ解決され、4行全体が「大」より縮んでいた。既定だけを「大」へ変更し、明示保存済みの各プリセットは保持する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- xterm.js固有のカーソル移動互換性。
- キーfaceの意匠変更。

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
