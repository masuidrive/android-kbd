---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "wide絵文字配置と音声・カーソル修正をv0.15.16としてAPK・GitHub・製品ページへ公開する"
created_at: "2026-09-16T08:23:57Z"
started_at: 2026-09-16T08:25:22Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260916-082357-publish-v0-15-16

### Why
wide画面の絵文字配置、空の音声layerから戻れない問題、標準selectionを扱えない入力先のカーソル移動を直した版を、利用者が直接導入できる状態にする。
Product Briefの配布可能なAndroid IMEと、端末内完結の公開導線を担う。

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
このticketが終わると、利用者が修正版APKをGitHub Releaseと製品ページから直接取得できるようになる。

- [x] AC 1: GitHubのpublicな`masuidrive/android-kbd` mainにv0.15.16のソースと公開記録があり、Releaseには未圧縮APKが1個だけ添付される。
- [x] AC 2: 公開APKがローカル最終成果物とbyte一致し、package、versionCode 32、versionName 0.15.16、ARM64 ABI、RECORD_AUDIOあり、INTERNETなしを確認できる。
- [x] AC 3: 公開APKでwide絵文字7列が一覧領域の中央へ揃い、音声候補が空でも最下段操作行から戻れ、非privateのselection非対応入力先だけカーソル操作がDPADへfallbackする。
- [x] AC 4: `https://masuidrive.jp/products/md-kbd/`とマニュアルがv0.15.16のAPK直リンクと変更点を表示し、スマホ・wide幅で横overflowを起こさない。

### Architectural Invariants check
端末内完結、入力内容を送信しないAI-1〜AI-4を維持する。APKにはINTERNET permissionを追加しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 版番号は直前のv0.15.15からpatchを1つ上げ、versionCodeは31から32へ上げる。
- GitHub Release assetはZIP化せず`gesture-ime-v0.15.16.apk`だけにする。
- 製品サイトの正本は本repoの`site/`、公開元は`../masuidrive.jp/docs/products/md-kbd/`とし、4主要fileをbyte比較してからpushする。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 新機能追加、入力辞書変更、既存Releaseの削除。

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

- 260915-114300-center-emoji-grid-wide（main統合済み）
- 260916-073031-fix-voice-panel-and-cursor-fallback（main統合済み）

---
Work notes: `note.md`
