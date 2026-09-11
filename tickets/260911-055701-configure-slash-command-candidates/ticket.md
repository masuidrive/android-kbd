---
priority: 2
base_branch: features/260911-055701-assign-layer-left-swipe-to-voice
description: "Let users configure up to six slash command candidates"
created_at: "2026-09-11T05:57:01Z"
started_at: 2026-09-11T08:15:40Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-055701-configure-slash-command-candidates

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
ターミナルやチャットで繰り返し入力するスラッシュコマンドを候補から選べるようにする。

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
この ticket が終わると、Gesture IME利用者が`/`に続く候補を設定し、入力中に選べるようになる。

- [ ] AC 1: 設定画面でスラッシュコマンド候補を最大6件入力・保存できる。
- [ ] AC 2: 初期値として`/compact`、`/clear`、`/quit`が設定され、残りの欄は空である。
- [ ] AC 3: 通常入力欄で`/`を入力すると保存済み候補が表示され、候補をタップすると入力中の`/`を選んだコマンドへ置き換えて確定する。
- [ ] AC 4: 空欄と重複候補は表示せず、パスワード欄では候補を表示しない。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
候補設定を端末内だけに保存し、AI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 設定枠は6件固定とし、初期3件以外は空欄にする。
- 候補タップ時だけ`/`から始まる入力中テキストを置換確定する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- コマンド履歴からの自動学習、同期、アプリ別設定。
- `/`以外を起点にする定型文候補。

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
