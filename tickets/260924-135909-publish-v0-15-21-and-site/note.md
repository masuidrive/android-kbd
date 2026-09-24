# Work Notes: 260924-135909-publish-v0-15-21-and-site

## Status: PDH-open (Opening)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [ ] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [ ] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [ ] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [ ] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [ ] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [ ] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [ ] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた
  - 直前公開はv0.15.20/versionCode 36、GitHub Releaseの最新tagはv0.15.20、製品ページもv0.15.20。前段Enter/Tab ticketはmain統合済み。v0.15.21のAPK metadata、配布byte一致、サイト同期とpublic描画は作成・公開後に測る。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 2026-09-24: 実装前に、`app/build.gradle.kts` の現行値がversionCode 36/versionName 0.15.20であること、現行サイトのAPK URLがtagとファイル名の両方へ同じ版を使うこと、前段ticketでEnterとTabの実装・正本仕様がmainへ統合済みであることを測定した。v0.15.21ではversionCode 37/versionName 0.15.21と同じURL規約を使う。
- 2026-09-24: `d10607b` `[260924-135909-publish-v0-15-21-and-site] chore(release): prepare v0.15.21 package metadata` — app versionを37/0.15.21へ更新し、Enter上Paste・下Ctrl+J、待機表示、右端Tabを記したリリースノートを追加した。
- 2026-09-24: `2e73f9b` `[260924-135909-publish-v0-15-21-and-site] docs(release): update v0.15.21 product surfaces` — README、公開URL、製品ページ、マニュアルをv0.15.21へ揃え、製品ページにもEnterの操作・待機ラベル・記号Tab右端を明記した。AGENTS.mdへ、release-worthy変更のhuman review後は明示的なlocal-only/non-public指定がない限りAPKとsite/manual/mockの公開まで進める既定手順を追加し、PDH-human-reviewのticket close承認を置換しないことを明記した。
- `site/mock.html`は前段ticketの時点で上Paste・下Ctrl+J、待機中の上C-j・中央Enter・下paste、記号3行目右端Tabを実装済みで、`docs/reference/mock-source.html`とbyte一致していたため、正本との同期を壊さないよう非変更とした。
- 構造的重複検出は`similarity-generic -t 0.7`を各変更HTML/Gradleファイルへ試行したが、インストール済みCLIがHTML/Kotlinを対応言語に含めないためskipした（対応言語はGo/Java/C/C++/C#/Ruby）。JS/TS/Python変更はない。
- focused checks: `scripts/fast-checks.sh`（5 checks passed）、`git diff --check`、旧v0.15.20参照なし、両product pageのv0.15.21直リンク、`site/mock.html`と`docs/reference/mock-source.html`のbyte一致を確認した。`scripts/test-all.sh`はDirectorがfinal SHAで実行する指定のため未実行。

## PDH-review. 品質検証結果
<!-- PDH-review-1 / PDH-review-2 のように attempt ごとに記録する。
     独立 reviewer（1 人以上。構成と model は CLAUDE.md「チーム構成・モデル設定」）の
     実装後 review 結果を統合。
     2 attempt 同種 Critical 再発で root cause 診断 → escalation。
     実装後 review 特有 gate: Ticket 不可侵 / logical commit cadence / E2E real API / テスト全件 PASS -->

### Findings (PDH-review-1)
<!-- 記録・分類・提示の運用は pdh-dev `_review.md`
     「スコープ外問題と過剰実装の扱い」に従う。 -->

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
|   |      |     |      |      |      |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- 該当なし。`technical-reference.md`のDesign decision 19と22が、今回公開する記号Tab右端とEnter上Paste・下Ctrl+J・待機ラベルを既に現在形で記録している。本担当のversion metadataと公開面の同期はアーキテクチャを変更しない。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
- [ ] ユーザ依頼: v0.15.21のAPKをビルドしてGitHub Releaseへ直接公開する
- [ ] ユーザ依頼: masuidrive.jpの製品ページ・操作モック・マニュアルを更新して公開する
- [ ] ユーザ依頼: レビュー後のAPK・サイト更新をプロジェクトAGENTS.mdの既定手順にする
- Product BriefのオフラインIMEの利用者がレビュー済み入力を入手できることへ接続する。ユーザの直近の公開指示をACと将来手順の承認として扱う。
- 影響レイヤー: Android appのversion metadata、公開APK、静的site/mock/manual、docs。IME service、custom view、Mozc JNI、入力test本体は前段ticketから変更しない。
- 出荷前に同じAPKのmetadata、byte再取得、siteの4 core fileとassets、GitHub Pagesのcommit反映、412px/840pxの表示・操作を測る。
