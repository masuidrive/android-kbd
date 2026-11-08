---
priority: 2
base_branch: features/261030-134021-fix-personal-picker-footer
description: "英数字・記号レイヤーの修飾キーは左右フリックでTabを送る"
created_at: "2026-11-08T02:49:49Z"
started_at: 2026-11-08T02:51:51Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 261108-024949-move-ca-tab-to-horizontal-flick

### Why
Product Brief の「同じジェスチャー体系で素早く操作したい」に沿い、英数字入力中の修飾キーを誤タップしてTabが送られることを防ぎ、必要なときはフリックでTabを入力できるようにする。

### What / Acceptance Criteria
この ticket が終わると、英数字レイヤーを使う人が、左端の修飾キーの誤タップでTabを送らず、左右フリックでTabを送れるようになる。
- [x] AC 1: 英数字レイヤーの左端の修飾キーを、修飾状態がないときにタップしても入力先に何も送らない。
- [x] AC 2: 同じキーを左または右へフリックすると、入力先にTabを1回送る。キーの表示と操作時の反応から、その方向を判別できる。
- [x] AC 3: 英数字・記号レイヤーの利用者は、同じキーの上下フリックで従来どおりAlt・Ctrlを次の1キーに指定でき、指定中の中央タップで解除できる。
- [x] AC 4: ブラウザの操作デモでも、英数字・記号レイヤーで同じタップ・左右・上下の操作結果になる。

### Architectural Invariants check
端末内のキー操作だけを変更し、AI-1〜AI-4 と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 英数字と記号は同じ修飾キーを使うため、両レイヤーで左右をTabにする。
- Alt・Ctrlが選択中の中央タップは既存どおり解除する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 他のキー配置、変換処理、レイヤー切替は変更しない。

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
