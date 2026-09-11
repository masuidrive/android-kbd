---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Enable Mozc conversion learning and integrate supported personal dictionary entries"
created_at: "2026-09-11T10:20:26Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-102026-enable-mozc-learning-and-user-dictionary

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->
候補確定を重ねても並び順が利用者の入力へ適応せず、端末で登録した語もGesture IMEの候補へ反映されない。端末内完結を保ちながら個人向け変換を有効にする。

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
このticketが終わると、Gesture IME利用者が確定履歴と利用可能な端末内個人辞書を変換候補へ反映できる。

- [x] AC 1: 通常入力欄で確定した変換候補をMozcが端末内で学習し、以後の候補順位へ反映する。
- [x] AC 2: private/incognito入力欄では学習せず、学習済みデータや入力原文を端末外へ送信しない。
- [x] AC 3: 学習由来候補を長押しすると、その候補の履歴学習を削除できる。
- [x] AC 4: 現行AndroidでOS個人辞書を安全に読み出せる場合は登録語を候補へ表示し、OSが提供しない場合は仕様と理由を製品内文書へ明記する。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->
学習データと辞書はアプリ専用端末内storageだけで扱い、network権限を追加せず、AI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- Mozcの通常学習を既定で有効にし、private inputではセッション単位で無効化する。
- 候補長押しはMozcが提供する履歴候補削除commandを使い、通常tap確定と分離する。
- Android個人辞書は現行SDKの公式提供範囲を調査し、非公開APIや権限回避は使わない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- クラウド同期、辞書export/import。
- Android非公開providerへのアクセス。

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
