---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Backspaceの下フリックEscを補助ラベルと選択アニメーションで表示する"
created_at: "2026-09-18T08:54:46Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260918-085446-show-backspace-esc-label

### Why
QWERTY・記号レイヤーのBackspaceは下フリックでEscを送れるが、nativeのキートップから割当が分からない。
Product Briefの同じジェスチャー体系で編集操作を素早く使える状態に接続する。

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
このticketが終わると、利用者がBackspaceの下フリックにEscがあることをキートップで確認し、選択中もEscを見失わず操作できる。

- [ ] AC 1: QWERTYと記号レイヤーのBackspaceに、下フリックの補助ラベル`Esc`が表示される。
- [ ] AC 2: Backspaceを下へフリックすると、他の下フリックキーと同じ位置・拡大アニメーションで`Esc`が表示され、指を離すとEscapeが送られる。
- [ ] AC 3: native、browser mock、正本仕様でラベル表記と動きが一致する。

### Architectural Invariants check
表示だけを既存のEscape actionへ接続し、AI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 表記は直接キーと揃えた`Esc`とし、全大文字`ESC`は使わない。
- 下フリック中は既存の補助ラベル拡大アニメーションを再利用する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- Escapeの割当、gesture threshold、キー寸法、他レイヤーの変更。
- 明白な表示変更のため新規testは追加せず、既存testと静的整合を確認する。

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
