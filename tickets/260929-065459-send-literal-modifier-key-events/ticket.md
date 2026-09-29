---
priority: 2
base_branch: features/260925-094954-unassigned-flick-tap-and-number-key-hints
description: "Ctrl/Alt と文字キーの組み合わせを編集メニュー操作へ変換せず送信する"
created_at: "2026-09-29T06:54:59Z"
started_at: 2026-09-29T06:55:54Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## Ctrl/Alt の文字キー操作をキーイベントとして送る

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->
Product Brief の「同じジェスチャー体系で素早く操作したい」を満たすため、Ctrl/Alt を付けた文字キーをアプリへそのまま届ける。現在の Ctrl+A/C/X/V は Android の編集メニュー操作へ変換され、特に Ctrl+C をショートカットとして受け取りたいアプリがキーイベントを受け取れない。
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
この ticket が終わると、Android 利用者が Ctrl/Alt と英字キーを組み合わせ、入力先へ修飾キー付きのキーイベントを送れる。

- [ ] AC 1: Ctrl+A/C/X/V/S と英字キーの組み合わせは、入力先へ Ctrl 修飾付きの同じ文字の押下・解放イベントを送る。編集メニューの選択・コピー・切り取り・貼り付け操作へ IME が変換しない。
- [ ] AC 2: Alt+A/C/S と英字キーの組み合わせは、入力先へ Alt 修飾付きの同じ文字の押下・解放イベントを送る。
- [ ] AC 3: Ctrl/Alt は次の一文字だけに適用され、その次の文字には残らない。機密入力欄でも Ctrl+V を IME が別扱いしない。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->
AI-1〜AI-4 の端末内入力・非記録・Mozc 境界・一回修飾状態と矛盾しない。
### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- Ctrl/Alt と文字キーの組み合わせは Android の `KeyEvent` の `META_CTRL_ON` / `META_ALT_ON` を持つ DOWN・UP ペアとして `InputConnection` へ送る。入力先が何を実行するかは入力先に委ねる。
- 変換中の composing は既存どおり先に確定する。修飾状態は既存どおり一文字後に解除する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 素の Paste キー、Android の OS 自体のコピー・貼り付け UI、数字・記号キーへの修飾適用、キーボード外観の変更。

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
