# Work Notes: 260924-135909-publish-v0-15-21-and-site

## Status: PDH-human-review

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [x] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [-] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した - skip: 実装後reviewの指摘はticket文面の節配置のみで、アプリ挙動の修正をしていない
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した
- [x] ユーザ依頼: v0.15.21のAPKをビルドしてGitHub Releaseへ直接公開する
- [x] ユーザ依頼: masuidrive.jpの製品ページ・操作モック・マニュアルを更新して公開する
- [x] ユーザ依頼: レビュー後のAPK・サイト更新をプロジェクトAGENTS.mdの既定手順にする

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
| 1 | ticket contract | Minor | WhyとArchitectural Invariants checkの内容がWork notes以下にずれていた | 採用・修正 | 同じ文面を正式な節へ移した。ACと設計判断は変更していない。 |

- 独立review `8cd6e1f`：Critical/Major 0。版、直リンク、Enter/Tabのnative・mock・仕様・マニュアル整合、`AGENTS.md`公開規則、ticketのAC不可侵を確認した。
- `scripts/test-all.sh --parallel --connected`：fast-checks PASS、API 36.1エミュレーターの接続24 tests PASS。unit/lint/APK区分は285件中`ImeHideBarTest.emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway`の一時的nullで1件FAIL。変更していない絵文字再入場経路のテストであり、同じテストの単独再実行PASS、単体全件再実行285/285 PASS、`lintDebug`・`assembleDebug` PASS。判定はretry-pass 1件（初回1失敗、再実行1成功）として記録する。
- ローカル製品ページは412px/840pxで横はみ出し・画像欠落なし。操作モックで記号レイヤー最右端Tabと`-`キーの入力欄反映を確認した。公開サイトでは再確認する。
- 公開APK：`https://github.com/masuidrive/android-kbd/releases/tag/v0.15.21`。直接配布assetは`gesture-ime-v0.15.21.apk`（ZIPなし）、38,651,216 bytes。`/tmp/gesture-ime-v0.15.21.apk`と再取得した`/tmp/md-kbd-verify-v01521/gesture-ime-v0.15.21.apk`は`cmp`完全一致、SHA-256 `d7ae3e0eb91812e1b40fe9aa35cd9fc5e742edfa6c0a0230fa3d8618c67c3473`。`aapt2`でpackage `com.masuidrive.gestureime`、versionCode 37、versionName 0.15.21、minSdk 28、ABI `arm64-v8a`、permission `RECORD_AUDIO`とアプリ内dynamic receiverのみ、`INTERNET`なし。公開assetをAPI 36.1 emulatorへ`adb install -r`成功後、Android `dumpsys package`もcode 37/name 0.15.21。
- 公開site：`masuidrive/masuidrive.jp` main/docs `a2394b7a937dca8a43655b376d4b94c36aeb668e`。push前に作業siteと公開repoのindex/mock/manual/stylesの4ファイルをbyte比較し、assetsも`diff -qr`一致。GitHub Pages APIが同じSHAを`built`と返した後、公開URLから4ファイルを再取得し、そのSHAのファイルと`cmp`一致。APK/ZIPバイナリはsite repoに追加していない。
- 公開ページ`https://masuidrive.jp/products/md-kbd/`と公開mockで412px/840pxの`innerWidth == scrollWidth`、画像欠落なし。mockでは412pxで「あ」tapが入力欄へ反映され、840pxで「か」tapが「あか」へ反映された。記号レイヤーの3行目最右端Tabを確認した。公開ページのphone/tablet screenshotは`/tmp/md-kbd-public-v01521/phone.png`と`tablet.png`。
- test-allのsuite自身のsummary：`PASS: fast-checks`、`PASS: android connected (real Mozc)`、`FAIL: android unit, lint, apk`、`Passed: 2 / 3`。この1区分は上記単体1件の初回失敗で止まったため、単独test・単体全285件・lint・assembleを別途成功させた。retry-pass 1件をgreen summaryへ混ぜない。初回失敗のbase-ref再現は未実施であり、human gateで残余リスクとして提示する。
- 独立AC verifierはapp release SHA `ae28329c25abd2495adc623b4e73094de2c31ed8`、site SHA `a2394b7a937dca8a43655b376d4b94c36aeb668e`についてAC1/AC2ともVERIFIEDと判定した。公開releaseは非Draft、Androidへの公開APKインストール後code37/name0.15.21、公開siteの両幅でEnterラベル順とTab最右端、実Pointer入力を確認。制約はFold実機未確認とretry-pass1件。
- 独立Surface Observerは公開siteの412px/840pxで横はみ出し、画像欠落、ブラウザerrorなし、かなタップ、記号切替、QWERTYのw下フリック→2、Tab最右端、Enter待機ラベルの上下位置を確認した。Enter上下フリックの終端結果はブラウザのクリップボード拒否とCtrl+Jの不可視性により独立観察できず、操作定義とariaまでの確認。native emulatorのUIはこの観察では非表示、物理Fold実機も未確認。前段ticketのnative MotionEvent接続テストと本ticketの接続24件は通過。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- 該当なし。`technical-reference.md`のDesign decision 19と22が、今回公開する記号Tab右端とEnter上Paste・下Ctrl+J・待機ラベルを既に現在形で記録している。本担当のversion metadataと公開面の同期はアーキテクチャを変更しない。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- 2026-09-25: 前ターンでAPK・site URL、公開assetのbyte一致、試験結果（単体1件retry-pass）、Fold実機未確認、進捗記録を提示して「この結果でリリースチケットを閉じてよいですか？」と確認した。ユーザは「y」と返信し、closeを明示承認した。
- close時点ではアプリのrelease `v0.15.21` と製品サイトのPages公開は完了済み。アプリ`main`には出荷codeが反映済みで、このfeature branchの追加差分は公開後の検証記録のみ。closeで新たに失われる利用者機能はない。`auto_push: false`のためclose後に`main`をpushする。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
- Android source release tag: `v0.15.21` / `ae28329c25abd2495adc623b4e73094de2c31ed8`。公開site SHA: `a2394b7a937dca8a43655b376d4b94c36aeb668e`。human review待ち。
