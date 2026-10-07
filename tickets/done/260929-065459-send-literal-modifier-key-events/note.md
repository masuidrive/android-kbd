# Work Notes: 260929-065459-send-literal-modifier-key-events

## Status: PDH-close (User approved; pending close)

## Checklist
- [x] 公開依頼に従い、直接ダウンロードできるAPKをビルド・検証・GitHub Releaseへ公開する。
- [x] 製品ページ・マニュアル・操作モックを同期公開し、公開ファイルと操作を確認する。
- [x] Ctrl+C/A/X/V を編集メニューへ変換せず、Ctrl 修飾付きキーイベントとして送る。
- [x] Ctrl+S と Alt 系も同じキーイベント経路であることを確認する。
- [x] 一回だけの修飾状態と機密欄での Ctrl+V を確認する。
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
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み（並列で既存絵文字testが1件fail、同一SHA逐次は3/3 PASS。retry-passとして下記に記録）
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み - skip: 修飾キー入力は端末内のInputConnectionだけを使い、外部providerはない。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [-] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した - skip: 独立reviewのfindingは0件で修正attemptはない。
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（PDH配布物の更新は不要。製品仕様・マニュアル・technical-referenceを更新）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [-] PDH-human-review: ユーザ自身が確認手順を実施した - skip: 実施したという申告はない。公開結果とエミュレータ検証を提示した後、ユーザは明示的にクローズを承認した。
- [x] PDH-human-review: ユーザが公開結果を受けてクローズを明示承認した

## PDH-ticket-review. Ticket contract check
2026-09-29: ユーザが「Ctrl+C のコピー変換は不要、キーコードを送る。Alt も全部そう。他の Ctrl+A や S は？」と明示。AC 1〜3 はその依頼を観察可能な操作へ分解した。影響レイヤーは Android app、IME service（既存経路の確認のみ）、unit tests、docs。Mozc JNI・UI・browser mock は挙動変更なし。未決のプロダクト判断なし。依存は現在ブランチの `TextInputController` と一回修飾実装だけで、別チケットの完了を要しない。consumer surface は Android の InputConnection キーイベント。
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

## Required Probes
- [x] `TextInputController` の現在の送信分岐と `KeyboardView` の一回修飾状態を読み、対象のキーと例外を確定する。
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた。`TextInputController.sendModifiedKey` は Ctrl+A/C/X/V を `performContextMenuAction` 優先、Ctrl+V は private 欄で抑制、それ以外を `sendKeyEvent` DOWN/UP で送る。`KeyboardView.dispatch` は次の一文字の後に `updateModifier(null)` を呼ぶ。

## PDH-implement. 実装ログ
変更前の経路を確認。`TextInputControllerTest` の fake InputConnection は event の keyCode のみ保存するため、metaState と DOWN/UP を観察できるように拡張する。
`TextInputController.sendModifiedKey` の Ctrl context action と private Ctrl+V 早期returnを削除。`TextInputControllerTest` で Ctrl/Alt の A/C/X/V/S 全件についてコード・metaState・DOWN/UP・context action不使用を検証し、private Ctrl+Vも検証。`KeyboardViewTest` は実 `MotionEvent` で modifier↓→C→S を送り、Cだけ修飾されSは通常文字となることを検証。ブラウザモックの Ctrl+A 独自選択も削除し、`virtualkey` イベントは維持。`site/mock.html` と保存版は byte 同一。マニュアルと仕様、technical-referenceを更新。
Focused checks: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk ./gradlew testDebugUnitTest --tests com.masuidrive.gestureime.TextInputControllerTest --tests com.masuidrive.gestureime.keyboard.KeyboardViewTest` → `BUILD SUCCESSFUL in 15s`。最初のSDKパスなし実行は `SDK location not found` で失敗し、環境指定して成功。
Commits: `7741de2` Android実装・unit・mock、`ee3c761` 仕様・manual・progress・note。検証対象 code SHA は `ee3c76159d4e029fb44f9c12f924612bc20a2ea4`。
Full suite exact command: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk PATH=/Users/masuidrive/Library/Android/sdk/platform-tools:$PATH bash scripts/test-all.sh --parallel --connected` → `PASS: fast-checks`, `PASS: android connected (real Mozc)`, `FAIL: android unit, lint, apk`, `Passed: 2 / 3`。失敗は `ImeHideBarTest > emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway` 1件（294 tests completed, 1 failed）。同じ SHA を `bash scripts/test-all.sh --connected` で逐次再実行 → `PASS: fast-checks`, `PASS: android unit, lint, apk`, `PASS: android connected (real Mozc)`, `Passed: 3 / 3`。接続27件、失敗0。絵文字testは retry-pass としてhuman reviewへ提示する。
実ブラウザのローカル合成済み製品ページ `http://127.0.0.1:18766/` でQWERTYのCtrl↓→Aをマウスpointer経路で操作。`virtualkey` は `{key:"a",ctrlKey:true,altKey:false}` を通知し、入力欄の選択範囲は `(1,1)` のまま。Ctrl+Aの選択変換は発生しなかった。browser listenerを二重登録した観測では同じ通知が2件に見えたため、件数の根拠には使わずdetailと選択範囲だけを採用する。
Surface Observer は `emulator-5554` に code SHA `ee3c761` からビルドしたAPKを入れ、`com.masuidrive.gestureime/.ImeService` を選択。一時的な別APKの `RecordingEditText`（ソース `/private/tmp/modifier-event-observer/app/src/main/java/com/example/keyobserver/MainActivity.java`）を入力先にし、画面上のproduction IMEキーをadbのswipe/tapで操作した。入力先では Ctrl+C/A/S が各 `KEYCODE_C/A/S` DOWN/UP `meta=4096 ctrl=true alt=false`、Alt+A/C/S が各 `KEYCODE_A/C/S` DOWN/UP `meta=2 ctrl=false alt=true` として記録された。Ctrl+Cの次にSをタップするとKeyEventログは増えず、入力欄へ通常文字`s`が入った。password targetでは`EditorInfo inputType=0x81`でCtrl+Vの`KEYCODE_V` DOWN/UP `meta=4096`を受信。IME側の編集メニュー操作は記録されなかった。証拠画像は `evidence/all-events.png`、`evidence/private-ctrl-v.png`。エミュレータ実証であり、物理Foldや全26英字の個別実証ではない。
AC Verifier はこのnative surface証拠を確認してAC 1〜3をすべてVERIFIEDと再判定。Ctrl+Xは実アプリで個別操作していないが、同一production送信経路とexact-key unit testで確認した。特定アプリごとのショートカット解釈までは契約外。
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

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
独立review（`pdh-reviewer`）は exact SHA `ee3c761` のdiffを読み Critical/Major/Minor いずれも0件で PASS。端末上の実ターゲットアプリが受け取るところまでは未観測と明記した。

## Technical reference 更新
設計判断34に修飾付きKeyEventのDOWN/UP、Ctrlメニュー変換の廃止、private Ctrl+V、Paste独立キーとの区別を記載。
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
2026-10-04 公開検証: リリース対象 SHA `dfd6f605abf72d6b22fbaf353eea1ba6fba94727`。最初の並列testはsandboxのGradle lock拒否、権限付き並列testは既知の `ImeHideBarTest.emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway` 1/294失敗とビルド競合。逐次testではunit/lint/APK成功、接続testはエミュレータ未起動、次回は既存AVDの空き容量不足で失敗。容量しきい値を一時調整した同じAVDで `bash scripts/test-all.sh --connected` は fast-checks / unit-lint-APK / connected 3/3 PASS、接続27/27。設定変更は元へ復帰。絵文字testは retry-pass として扱う。
2026-10-04 APK: `app/build/outputs/apk/debug/gesture-ime-v0.15.28.apk`、38,653,320 bytes、SHA-256 `3988999df346b33baafa749c35e285a9f410be3169853ac5263498b642604d3e`。package `com.masuidrive.gestureime`、versionCode 44 / versionName 0.15.28、minSdk 28、ARM64、RECORD_AUDIOあり、INTERNETなし、zipalignとAPK v2署名verify成功。GitHub Release `https://github.com/masuidrive/android-kbd/releases/tag/v0.15.28` に直接APKを添付し、再ダウンロードしたものとbyte一致。
2026-10-04 site: `masuidrive/masuidrive.jp` main commit `b6f94f0a125b7e66ba87de4cf06fbbc110338892`、GitHub Pages status built。公開の `index.html` / `mock.html` / `manual.html` / `styles.css` は同commitのファイルと4/4 byte一致。公開ページは412px/840pxで横overflowなし、スマホ幅で「あ」tap→入力値「あ」、タブレット Dual Flickで「あ」上フリック→「う」。公開QWERTYでCtrl下フリック→A tapは `{key:"a",ctrlKey:true,altKey:false}`を通知し、選択範囲(1,1)のまま。公開マニュアルの画像欠落・横overflow・ブラウザerrorなし。
2026-09-29: 差分、実IMEのキーイベント観測、並列実行のretry-pass、進捗と証拠画像の場所をユーザへ提示し、ticket close承認を依頼した。
2026-10-04: ユーザがその提示に対して「公開して」と指示した。v0.15.28としてAPK・サイトを公開する承認と扱う。ticket closeのchecklistはユーザ自身の確認手順実施と明示承認を要するため、公開後も未チェックを維持する。
2026-10-07: 公開結果と検証結果の提示後、ユーザが「クローズ」と明示承認した。ユーザ自身の端末確認について実施申告はないため、実施済みとは記録しない。
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
