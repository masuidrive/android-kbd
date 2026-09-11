---
priority: 2
base_branch: features/260911-055701-configure-slash-command-candidates
description: "Wrap long voice-recognition candidates inside the candidate strip"
created_at: "2026-09-11T23:47:48Z"
started_at: 2026-09-11T23:49:17Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-234748-wrap-long-voice-candidates

### Why
音声認識の候補は発話全体なので、日本語変換候補より長くなりやすい。
候補が画面幅を超えると後半を読むために横スクロールが必要になり、候補同士を比較しにくい。

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
このticketが終わると、キーボード利用者が長い音声認識候補を候補欄内の改行で読める。

- [ ] AC 1: 音声認識の途中結果と最終候補は、1候補が候補欄の横幅を超える場合に候補face内で改行される。
- [ ] AC 2: 日本語変換、英字補完、スラッシュコマンドの候補は従来どおり1行表示と横スクロールを維持する。
- [ ] AC 3: 音声候補が改行されても、IME全体と4行の音声レイヤーの高さは変化しない。
- [ ] AC 4: ブラウザモックのMobile/Tablet、Light/Darkでも同じ改行挙動になり、横overflowを生じない。

### Architectural Invariants check
端末内完結、固定IME高、通常候補UIの再利用という既存方針と矛盾しない。

<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 音声候補だけに折返しを適用し、通常候補の密度と横スクロール操作は変えない。
- 1候補の最大幅を候補欄の可視幅に制限し、固定候補欄高に収まる最大2行で表示する。
- 3行以上になる文字列は末尾を省略し、IME全体高は増やさない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 音声認識エンジン、候補順序、確定処理の変更。
- 通常の日本語・英字・スラッシュ候補の複数行化。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「音声入力の候補、横幅超えたら候補を改行して」。Android本体と公開ブラウザモックを同期する。

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
