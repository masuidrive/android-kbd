---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "トップの埋め込みデモをキーボード中心にし、機能一覧を主要6項目へ絞る"
created_at: "2026-09-11T17:58:28Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-175828-simplify-homepage-demo-features

### Why
トップの入力枠と細かな機能一覧が、キーボード本体の操作より目立っている。
操作モックと主要な違いへ情報を絞り、短時間で製品の特徴を把握できるようにする。

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
このticketが終わると、閲覧者がトップでキーボード操作へ集中し、主要な6機能だけを読み取れる。

- [x] AC 1: トップ埋め込みデモには入力枠が表示されず、キーボードのタップとフリックを操作できる。
- [x] AC 2: 機能一覧は01〜06だけを表示する。
- [x] AC 3: 04はCtrl、Alt、Escキーのサポートとして説明される。
- [x] AC 4: 独立した操作モックでは、従来どおり入力欄へ入力結果を確認できる。

### Architectural Invariants check
静的サイトと端末内完結の製品説明に関する既存方針と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 入力欄はトップ埋め込み時だけ視覚的に隠し、モックの入力処理と独立ページの入力欄は維持する。
- 詳細機能はマニュアルへ残し、トップの機能一覧はユーザ指定どおり01〜06へ絞る。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- Android本体のキー割り当てや表示は変更しない。
- サイトの公開は行わない。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: ページの入力デモの入力枠を外す。04をCtrl、Alt、Escキーのサポートという表現にする。07〜15は削除する。

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
