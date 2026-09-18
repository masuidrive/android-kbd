# Work Notes: 260918-075503-publish-v0-15-17

## Status: PDH-close (Approved and published)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部providerを利用しないAndroid APKと静的サイトの公開ticket
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [-] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した - skip: 独立reviewの指摘は0件で修正なし
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

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
  - 現行versionCode 32・versionName 0.15.16、直前公開版v0.15.16のため次版を33・0.15.17とする。
  - Release assetはAPK 1個とし、再取得byte一致、metadata、ABI、permissionを測る。
  - 公開サイト4主要fileのbyte一致、Pages成功、412px・840px overflowとQWERTY mock操作を測る。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

2026-09-18: 実装前に確認した。HEADはQWERTY変更`2600382`を含み、pseudo-spectrum実装`eaf70b3`はancestorではない。version metadataは`app/build.gradle.kts`、公開URLと利用者向け説明はREADME・site/index.html・site/manual.html、release notesは`docs/v0.15.16-release-notes.md`の形式に従う。v0.15.17 APKはdebug artifactを指定名へコピーして用意し、GitHub Release作成・push・製品サイト同期はroot担当のため実行しない。

2026-09-18: `fc8a30b`でversionCode 33/versionName 0.15.17、README、site index/manualのv0.15.17 direct APK URL、右端`.`/Backspace操作説明、`docs/v0.15.17-release-notes.md`を準備した。QWERTYの2行目右端は0.5wの`.`（tap `.`、down `?`）、3行目右端は1w Backspace（tap削除、down Esc、left/up/right no-op）と記載し、Symbols不変も明記した。

2026-09-18: `similarity-ts -t 0.70 --extensions html site/index.html site/manual.html`はduplicateなしでPASS。`similarity-generic`はPATHになくskipした。`scripts/fast-checks.sh`は5 checks PASS、QWERTYの`KeyboardLayoutsTest`と`KeyboardViewTest`はBUILD SUCCESSFUL、`lintDebug assembleDebug`はBUILD SUCCESSFUL。version/APK link static checkはversionCode/versionName、4つのv0.15.17 direct URL、旧v0.15.16参照なし、mock正本byte一致、APKにINTERNETなしをPASSした。

2026-09-18: 全unitを含む`./gradlew testDebugUnitTest lintDebug assembleDebug`は282 tests中1件、既存`ImeHideBarTest.privateEditorSwitchesToItsDedicatedEmptyPickerWithoutReusingPublicPicker`（`ImeHideBarTest.kt:203`）のAssertionErrorでFAILした。release変更はversion/documentationのみで、QWERTY focused、lint、APK buildは個別にPASSした。この全unit failureの再検証と公開後のGitHub Release asset再取得・site同期/Pages確認はroot担当へ引き継ぐ。

2026-09-18: artifactは`app/build/outputs/apk/debug/gesture-ime-v0.15.17.apk`。`app-debug.apk`とbyte一致、size 38,634,832 bytes、SHA-256 `d491d0ce357ae1e3bf8d569cd2f52a728d773e497fd6740f7f9a7102c0ee1fab`。aaptでpackage `com.masuidrive.gestureime`、versionCode 33、versionName 0.15.17、minSdk 28、targetSdk 36、native-code arm64-v8a、RECORD_AUDIOあり、INTERNETなしを確認した。

2026-09-18: 最終SHA `a9cf761` の成果物に対し、rootが `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel --connected` を再実行し、fast-checks・Android unit/lint/APK・connected Androidの3/3 PASSを確認した。独立reviewでも `scripts/test-all.sh --parallel` は2/2 PASSで、先行実行時の単発unit failureは再現しなかった。

## PDH-review. 品質検証結果
<!-- PDH-review-1 / PDH-review-2 のように attempt ごとに記録する。
     独立 reviewer（1 人以上。構成と model は CLAUDE.md「チーム構成・モデル設定」）の
     実装後 review 結果を統合。
     2 attempt 同種 Critical 再発で root cause 診断 → escalation。
     実装後 review 特有 gate: Ticket 不可侵 / logical commit cadence / E2E real API / テスト全件 PASS -->

### Findings (PDH-review-1)
<!-- 記録・分類・提示の運用は pdh-dev `_review.md`
     「スコープ外問題と過剰実装の扱い」に従う。 -->

対象SHA: `a9cf761732933253966ddd6d45c7b419a3dff5de`。Criticalなし、Majorなし、Minorなし。

- AC 1: release prepとして`fc8a30b`にversion・docs・site、`a9cf761`にartifact検証証拠を分けて収録し、asset名を`gesture-ime-v0.15.17.apk`へ統一している。公開mainへの反映、Release作成、assetが1個だけであることは本reviewでは未実施で、承認済みの公開工程と公開後検証で確認する。
- AC 2 / Architectural Invariant: `app/build.gradle.kts:11-17`はpackage `com.masuidrive.gestureime`、versionCode 33、versionName 0.15.17、arm64-v8aを指定する。`aapt dump badging/permissions`で同じpackage・版・ABI、minSdk 28、targetSdk 36、RECORD_AUDIOあり、INTERNETなしを独立確認した。`app-debug.apk`と`gesture-ime-v0.15.17.apk`は38,634,832 bytes、SHA-256 `d491d0ce357ae1e3bf8d569cd2f52a728d773e497fd6740f7f9a7102c0ee1fab`でbyte一致する。公開assetの再取得byte比較は公開後工程で行う。
- AC 3: main統合済み`2600382`をancestorに含む。`KeyboardLayouts.kt:25-35,99-108`と`KeyboardLayoutsTest.kt:41-67`がQWERTY 0.5w period／1w BackspaceとSymbols不変を固定し、`KeyboardViewTest.kt:82-156`がattached production `MotionEvent`でtap、down、未割当方向、threshold、center復帰、cancel、repeat、412/840幅を検証する。mockは18px選択・10px復帰と未割当方向no-opを実装し、412/840ブラウザで横overflowなし、840pxでperiod 41px／Backspace 82pxを確認した。
- Out-of-scope: pseudo-spectrum branchの先頭実装`eaf70b3`はHEADのancestorではない。固有の`voiceInputPhase`、`VOICE_SPECTRUM_BAR_COUNT`、6本bar生成は現sourceに存在せず、native/mock/specは承認済みの4本波形のままである。未承認機能の混入なし。
- Docs / links: `README.md:5,62`、`site/index.html:19,28,37,46,52`、`site/manual.html:6,12-18,36,45,47`、`docs/v0.15.17-release-notes.md:1-10`はv0.15.17と今回のQWERTY 3点（period、Backspace、Symbols不変）を一致して説明する。対象4文書にv0.15.16残存なし。index 2本・manual 2本のURLはすべて`releases/download/v0.15.17/gesture-ime-v0.15.17.apk`への直接リンクで、ZIP参照なし。release notesは直前版と同じ本文＋検証形式を保つ。
- Site / parity: local HTTPでindex、manual、埋込mockを412px・840px表示し、各documentの`scrollWidth == clientWidth`を確認した。参照assetはすべて200（未指定faviconの自動要求だけ404）。`site/mock.html`と`docs/reference/mock-source.html`はbyte-identical。
- Review検証: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel`はfast-checksとAndroid unit/lint/APKの2/2 PASS。実装ログの既存unit単発failureは最終SHAで再現しなかった。再build後もversioned APKのsize・SHA・byte一致は変わらない。
- Design Decisions / contract: versionCode 32→33、versionName 0.15.16→0.15.17、未圧縮APK 1個、site正本と公開先の4主要file byte比較という判断を変更していない。public push、Release作成、製品サイト同期はこのreviewでは行っていない。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

2026-09-18: 該当なし。版番号、配布リンク、利用者向け操作説明、release notesのみを更新し、実装構造は変えていない。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

2026-09-18: QWERTY差分と実機相当・browser検証、APK pathを提示した後、ユーザが「公開」と明示したため、本releaseのmain統合・push・GitHub Release・製品ページ公開・ticket closeを承認済みと判断した。

## PDH-verify. 公開後検証

2026-09-18: `masuidrive/android-kbd`はPUBLIC、default branchはmainで、公開main `b9307278877ac5a25143470b467811454a326214`がv0.15.17 release prepとQWERTY変更を含む。GitHub Release `v0.15.17`はdraft/prereleaseではなく、assetは未圧縮`gesture-ime-v0.15.17.apk` 1個だけである。

2026-09-18: 公開assetを`/tmp/md-kbd-v0.15.17-public/gesture-ime-v0.15.17.apk`へ再取得し、ローカル最終成果物と`cmp`でbyte一致を確認した。size 38,634,832 bytes、SHA-256 `d491d0ce357ae1e3bf8d569cd2f52a728d773e497fd6740f7f9a7102c0ee1fab`。公開前のaapt検証どおりpackage `com.masuidrive.gestureime`、versionCode 33、versionName 0.15.17、ARM64、RECORD_AUDIOあり、INTERNETなしである。

2026-09-18: 製品サイトcommit `844c8eb0e1a6129046d40aa96d05848c77994fdc`のPages run `35323344579`はsuccess。公開URLからindex/mock/manual/stylesを再取得して同commitと4件すべてbyte一致した。実browser 412pxでpage/mockとも横overflowなし、QWERTYでperiod tap `.`、下flick `?`、`q`入力後Backspace tapで`q`だけ削除されることを確認した。840pxでもpage/mockとも横overflowなし、Tablet・Dual Flickが有効であることを確認した。

2026-09-18: documentationはREADME、release notes、製品ページ、manualをv0.15.17へ更新済み。technical-reference.mdは版番号・配布導線の変更で実装構造に差分がないため更新不要。未承認pseudo-spectrumは公開mainとAPKに含まれない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
2026-09-18: QWERTY機能ticketの差分・検証結果・APK path提示後にユーザが「公開」と明示したため、本ticketに列挙したmain push、v0.15.17 APK Release、製品ページ公開、release ticket closeまでを承認済みとして扱う。直前版からのpatch更新で未確定判断はない。
