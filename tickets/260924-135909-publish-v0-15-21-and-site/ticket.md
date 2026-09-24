---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "レビュー済みEnter・記号Tab変更をv0.15.21として公開する"
created_at: "2026-09-24T13:59:09Z"
started_at: 2026-09-24T14:00:08Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## masuidrive-kbd v0.15.21 を公開する

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->

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
このticketが終わると、Android利用者がレビュー済みの操作を含むv0.15.21 APKを入手でき、製品ページで同じ操作を確認できる。

- [ ] AC 1: Android利用者がGitHub Releasesからv0.15.21のAPKをZIPなしで直接ダウンロードでき、インストール後のアプリ情報にv0.15.21が表示される。
- [ ] AC 2: 製品ページとマニュアルがv0.15.21への直リンクとEnterの上下操作・待機ラベル・記号Tab右端を示し、ページ内の操作モックもスマホ幅とタブレット幅で同じ配置になる。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 直前公開v0.15.20 / versionCode 36の次としてv0.15.21 / versionCode 37を使う。
- APKはGitHub Releasesへ直接添付し、公開サイトrepoにはバイナリを置かない。
- レビュー後のAPK・サイト公開を今後のプロジェクト既定手順として`AGENTS.md`へ記録する。ユーザが個別に非公開・ローカル限定を指定した場合はそれを優先する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 新しいキー割当、Mozc/音声処理、データ形式、権限の変更。

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
Product Briefの同じジェスチャー体系で素早く入力する目的に沿い、レビュー済みのEnter表示・操作と記号Tab位置を、利用者がAPKと製品ページから入手・確認できる状態にする。
版と配布面の更新のみで、AI-1〜AI-4の端末内入力・非記録・小さなMozc境界・一回修飾状態を変えない。
- ユーザ指示: 「バイナリつくってサイトも更新してね。これはレビュー終了後に常にやるようにagent.mdかなにかに書いておいて」。
- `260924-021245-swap-enter-flick-directions`がレビュー済みでmainに統合されていること。
