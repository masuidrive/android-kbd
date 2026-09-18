---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "QWERTYの.?キーと0.5w Backspaceの位置と幅を入れ替える"
created_at: "2026-09-18T05:22:56Z"
started_at: 2026-09-18T05:25:38Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260918-052256-swap-qwerty-period-backspace

### Why
QWERTYレイヤーでは、削除キーが右上端の半幅、句点・疑問符キーが右下端の全幅にあり、ユーザが求めるキー配置と操作面積になっていない。
Product Brief の「英字はタップ・上スワイプ・下フリック」とスマホ・タブレットで同じジェスチャー体系を使う方針に沿い、両キーを入れ替える。

### What / Acceptance Criteria
この ticket が終わると、QWERTYを使う人が、右上端の半幅キーで句点・疑問符を入力し、右下端の全幅キーで削除・Escを操作できる。

- [ ] AC 1: QWERTYの2行目右端に0.5wの「.」キーが表示され、タップで`.`、下フリックで`?`を入力できる。
- [ ] AC 2: QWERTYの3行目右端に1wのBackspaceキーが表示され、タップで1文字削除、下フリックでEscを送信でき、左・上・右フリックには動作が割り当てられない。
- [ ] AC 3: QWERTYの上3行は各10wを保ち、4行の高さ・キー間隔は変更されない。記号レイヤーの配置と操作は変更されない。
- [ ] AC 4: Android native、ブラウザモック、操作仕様のQWERTY配置が一致し、スマホ幅とタブレット幅の両方で横にはみ出さない。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。既存の端末内キー入力とジェスチャー割り当ての配置だけを変更する。

### Design Decisions
- QWERTYのKeySpec配置と幅を入れ替え、既存のBackspaceタップ削除・下フリックEsc、および「.」タップ・「?」下フリックの操作を保つ。
- 変更対象はQWERTYだけとし、同じBackspace helperを使う記号レイヤーには波及させない。

### Out-of-scope
- 記号・かな・テンキー・絵文字・音声レイヤーのキー配置変更。
- 新しい記号やジェスチャーの追加。
- APKの公開、GitHubへのpush。

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
