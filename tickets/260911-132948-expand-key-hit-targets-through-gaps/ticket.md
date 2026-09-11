---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Keep the visual key gaps while assigning gap touches to the nearest key"
created_at: "2026-09-11T13:29:48Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-132948-expand-key-hit-targets-through-gaps

### Why
HTMLへ合わせてキー間隔を広げた結果、nativeでは見えるキー面とタップ判定が同じ矩形のため、横6dp・縦10dpの隙間が無反応になった。外観を維持したまま、ソフトキーボードとして必要なタップ許容度へ戻す。

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
このticketが終わると、Gesture IME利用者がキーの見た目の隙間付近をタップしても、最も近いキーを入力できる。

- [x] AC 1: 同じ行の隣接キー間をタップすると、隙間の左半分は左キー、右半分は右キーとして入力される。
- [x] AC 2: 隣接行のキー間をタップすると、隙間の上半分は上の行、下半分は下の行として入力される。
- [x] AC 3: キーの形状・間隔・影・ラベル・フリック閾値はv0.9.0の表示と操作を維持する。
- [x] AC 4: EMPTYで予約した領域は入力を発生させず、Dual Flickと複数行Enterでも隣のキーと判定が重ならない。

### Architectural Invariants check
描画と入力判定だけの端末内変更で、入力内容を外部へ送らないAI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 描画矩形とタップ矩形を分離し、描画矩形は変更しない。
- タップ矩形は論理セル全体を横方向へ割り当て、縦方向は見えるキー間の中点で分ける。
- ACTION_DOWNで選んだキーを指を離すまで保持する既存gesture契約は維持する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- フリック方向・閾値の変更。
- キー配列と見た目の隙間変更。

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
