---
priority: 2
base_branch: features/260911-055701-center-qwerty-down-swipe-label
description: "Align Japanese, English, and demo candidate UI with the HTML reference"
created_at: "2026-09-11T06:27:19Z"
started_at: 2026-09-11T06:29:26Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-062719-align-candidate-ui-with-html

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->
日本語変換候補と英字補完候補の形状・余白・影・文字位置がHTML正本と異なり、キーボード全体の一貫性が崩れている。nativeと公開demoを同じ候補表現へ揃える。

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
この ticket が終わると、Gesture IME利用者が日本語変換候補と英字補完候補をHTML正本と同じ形状・間隔・選択表現で利用でき、公開demoでも同じ表示を確認できる。

- [x] AC 1: nativeの日本語変換候補と英字補完候補が、高さ34dp、最小幅82dp、左右padding 14dp、間隔5dp、角丸7dp、1dp下影、HTMLの15pxに対応する15dp相当の固定文字で表示される。
- [x] AC 2: 未選択候補と選択候補がHTML正本のLight/Dark配色に従い、選択候補だけaccent背景・対応する文字色になる。
- [x] AC 3: 候補欄は高さ42dp、左右3dp・下8dpの余白を持ち、横スクロール・選択候補への追従・候補tap確定を維持する。
- [x] AC 4: `demo.html`が読み込む`mock.html`でも、日本語候補と英字補完候補を同じ候補部品・同じLight/Dark表現で表示できる。
- [x] AC 5: 候補なしのplaceholderは表示せず、private入力欄では英字候補を出さない既存挙動を維持する。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->
表示部品と公開mockだけを変更し、端末内候補生成・private欄抑止・AI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 正本は`docs/reference/mock-source.html`のcandidate bar/button定義とする。
- 日本語候補と英字候補はnativeの同じ`CandidateStripView`と、mockの同じcandidate button表現を使う。
- 音声状態UIも後続ticketで同じfaceへ統合できる構造にする。
- HTMLの固定pxをnativeのdpへ写し、キートップ同様に端末fontScaleで候補faceから文字がはみ出さない固定サイズとする。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 候補生成辞書・順位・最大件数の変更。
- 音声途中結果の接続と音声セッション処理（`260911-060238-align-voice-status-ui-and-show-partials`で行う）。

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
