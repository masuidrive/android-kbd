---
priority: 2
base_branch: features/260912-002431-commit-enter-flick-conversions
description: "Align native settings and voice layer with the current HTML and CSS"
created_at: "2026-09-12T00:24:31Z"
started_at: 2026-09-12T00:57:12Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-002431-align-native-settings-and-voice-ui

### Why
設定画面は機能を追加した結果、素のcontrolが縦に並び、製品ページとキーボードの視覚設計から離れている。
音声レイヤーも旧スクリーンショットの大きな空白や独自buttonではなく、現行HTML/CSSの候補欄・キー形状・固定4行構成と一致させる必要がある。
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
このticketが終わると、IME利用者が製品ページと同じ視覚規則の設定画面と音声レイヤーを使える。

- [ ] AC 1: 設定画面は初期設定・入力設定・スラッシュコマンド候補・アプリ情報の区切りが視覚的に分かり、Light/Darkで文字やcontrolが背景へ埋もれない。
- [ ] AC 2: 設定画面のbutton、switch、入力欄は48dp以上の操作領域を持ち、スマホ幅で切れや横overflowがなく、バージョンを表示する。
- [ ] AC 3: native音声レイヤーは現行`site/mock.html`と同じ候補欄＋固定4行高で、独自の上部状態行を表示せず、左下に通常キーと同じ形状の「キャンセル」を表示する。
- [ ] AC 4: 音声認識の途中結果と最終候補は通常候補欄のUIで表示し、最終候補tapで1回確定、キャンセルで入力せず復帰する。
- [ ] AC 5: 音声レイヤーのキャンセルキーは上で日本語、右でQWERTY、下でテンキーへ切り替えられ、切り替え後のレイヤーを次回も引き継ぐ。
- [ ] AC 6: 現行HTML/CSSとnativeの設定・音声画面を並べて確認できる最新スクリーンショットをマニュアルへ反映する。

### Architectural Invariants check
音声認識はAndroidの端末内SpeechRecognizer経路を維持し、固定IME高・共通候補UI・system Light/Dark追従と矛盾しない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- `site/mock.html`の候補欄、キーface、gap、色token、固定4行を音声レイヤーの視覚正本として比較する。
- 設定画面は製品ページの余白・階層・控えめなgrayとaccentを参照し、Android標準の可読性と操作領域を保つ。
- 現行native音声レイヤーは候補欄、固定4行、キャンセル、partial/final、レイヤー切替を既に備えるため実機で再確認し、差分だけを実装する。
- 旧マニュアル画像は現行実装を表していないため、Light/Dark設定画面と音声partial/finalの最新画像へ更新する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 音声認識engineや候補順序の変更。
- 新しい設定項目の追加。
- キーボードの通常QWERTY・日本語・テンキー・記号配列の変更。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「設定画面見直し、音声レイヤーも実装して。今のhtmlやcssをよく見て。」
現行参照: `site/mock.html`、`site/styles.css`、`site/assets/setup-v0.9-user-dictionary.png`、`site/assets/voice-layer-v0.9.png`。
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
