---
priority: 2
base_branch: features/260911-055701-configure-slash-command-candidates
description: "Align voice status controls with the keyboard UI and show partial recognition text"
created_at: "2026-09-11T06:02:38Z"
started_at: 2026-09-11T08:35:24Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-060238-align-voice-status-ui-and-show-partials

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
音声認識中の状態と操作を他のキーと同じ見た目で読み取りやすくし、認識途中の内容を確認できるようにする。

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
この ticket が終わると、Gesture IME利用者が候補・確定エリアで音声認識の途中内容と操作状態を確認できる。

- [x] AC 1: 端末内認識エンジンが途中結果を返した場合、最新の認識途中テキストが候補・確定エリアへ表示される。
- [x] AC 2: 途中結果を返さない間は「音声を聞いています」または「音声を認識しています」が表示される。
- [x] AC 3: 「取消」「非対応」「許可」の表示はキートップ・候補と形状、角丸、影、Light/Dark配色を揃える。
- [x] AC 4: 最終結果は既存どおり指を離した後にそのまま入力し、途中結果は入力欄へ確定しない。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
端末内`SpeechRecognizer`だけを使い、途中結果を保存・送信せず、AI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- `RecognizerIntent.EXTRA_PARTIAL_RESULTS`を有効にし、`onPartialResults`の最新候補だけを一時表示する。
- 認識セッション終了、取消、エラー、入力欄切替で途中表示を破棄する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 途中結果の入力欄への逐次commit。
- クラウド認識へのfallback、途中結果の保存。

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
