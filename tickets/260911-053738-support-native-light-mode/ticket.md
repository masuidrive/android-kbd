---
priority: 2
base_branch: features/260911-003916-add-english-candidate-buffer
description: "Android native keyboard follows the device Light/Dark theme"
created_at: "2026-09-11T05:37:38Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-053738-support-native-light-mode

### Why
Product Brief の Fold7 幅対応とテーマ検証を満たすため、Dark 固定の native IME を端末の表示テーマに追従させる。

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
この ticket が終わると、Gesture IME 利用者が、端末の Light/Dark 設定に合うキーボードを使えるようになる。

- [ ] AC 1: 端末を Light テーマにすると、キーボード、候補欄、フリックポップアップ、設定内プレビューが HTML 正本の Light 配色で表示される。
- [ ] AC 2: 端末を Dark テーマにすると、同じ表示箇所が現行の Dark 配色で表示される。
- [ ] AC 3: テーマ切替後も文字入力、候補選択、レイヤー切替、フリック操作が利用できる。

### Architectural Invariants check
色リソースと描画のみを変更し、AI-1〜AI-4の入力処理・通信・状態管理に影響しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- Android の `uiMode` に自動追従し、独自のテーマ設定は追加しない。
- Light 配色は `docs/reference/mock-source.html` の CSS 変数を正本とし、既存 Dark 配色を保持する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 手動で選ぶテーマ設定の追加。
- HTMLデモの配色変更（すでにLight/Dark対応済み）。

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
