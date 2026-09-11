---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Publish the v0.9.1 APK directly through GitHub Releases and slim the Sites deployment"
created_at: "2026-09-11T17:10:53Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-171053-publish-v0-9-1-apk-via-github-releases

### Why
APKをZIPへ包むとAndroidで展開が必要になり、Sitesへ全世代を置くと容量も約200MBになる。
APKを直接インストールできる配布先へ移し、操作モックと説明ページだけをSitesで軽く配信する。

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
このticketが終わると、利用者がGitHub Releasesからv0.9.1 APKを直接ダウンロードでき、公開サイトを軽量なまま利用できる。

- [ ] AC 1: GitHub Release v0.9.1に`gesture-ime-v0.9.1.apk`が単体で公開される。
- [ ] AC 2: 製品紹介、操作モック、マニュアルのAPKリンクがGitHub Releaseの直接APKを開く。
- [ ] AC 3: Sitesの公開物に旧版を含むAPK ZIPが残らず、操作モック、製品紹介、マニュアルは引き続き表示できる。

### Architectural Invariants check
配布経路だけの変更であり、端末内完結とネットワーク権限なしを定めるAI-1〜AI-4を変更しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- APKはZIP化せずGitHub Release assetとして置く。
- SitesはHTML/CSS/画像と操作モックだけを配信し、バイナリの世代保管には使わない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 既存APKの署名方式変更。
- Androidアプリ本体の機能変更。

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
