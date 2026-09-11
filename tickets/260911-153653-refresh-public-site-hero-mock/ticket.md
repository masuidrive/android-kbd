---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Make the public-site hero an interactive device and theme preview"
created_at: "2026-09-11T15:36:53Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-153653-refresh-public-site-hero-mock

### Why
公開トップがFold7専用品の静的紹介に見え、実際の入力操作とスマホ・タブレット双方への対応が伝わらない。一般Android向けフリックキーボードとして位置づけ、最初の画面で操作と表示幅を試せるようにする。

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
このticketが終わると、公開サイトの閲覧者がトップ画面だけで製品の対象と操作感を理解し、入力と表示条件を試せる。

- [ ] AC 1: トップは「Android Flick Keyboard by masuidrive」を主軸に表示し、ブランド短縮名として`md-kbd`を使う。
- [ ] AC 2: ヒーローに実際にキー入力できるモックを表示し、入力欄には初期文として「ここは入力できるよ」を表示する。
- [ ] AC 3: トップの操作でLight/Darkを明示的に切り替え、ページと埋め込みモックを同じテーマへ切り替えられる。
- [ ] AC 4: トップの操作でMobile/Tabletを切り替え、Tablet時はDual FlickのON/OFFを切り替えて左右2組のかなキーを確認できる。
- [ ] AC 5: 製品をFold専用とは表現せず一般Android向けとし、Foldは閉じたスマホ幅と開いたタブレット幅の両方で使える対応例として記載する。
- [ ] AC 6: デスクトップとスマホ幅で横overflowや操作不能がなく、既存の独立した操作モックページも引き続き使える。

### Architectural Invariants check
公開用静的HTML/CSS/JavaScriptと説明文の変更で、IME本体の端末内完結を定めるAI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- ブランド表示は`md-kbd`、説明は「Android Flick Keyboard by masuidrive」とする。
- 端末幅の名称はMobile/Tabletとし、Foldは両方を使う具体例として扱う。
- ヒーロー内モックは既存`mock.html`の入力・レイヤー・Dual Flick挙動を再利用し、独立demoも維持する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- Androidアプリ名、package、アイコン画像の変更。
- Sites本番への公開。今回はローカル確認までとする。

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
