# Work Notes: 261027-015056-support-inline-autofill

## Status: PDH-human-review (2026-10-27 11:22 JST)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [ ] ユーザ依頼: Bitwardenと共に使えるAndroidインライン自動入力に対応する。
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み（2026-10-27の「Bitwardenと共に使うためにinline autofill対応して」という明示実装依頼の範囲をAC 1〜4へ分解）
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: Android AutofillServiceはHTTPではなく端末内Binder API。QA serviceのFillResponseと実tap入力を確認し、Bitwarden本体は未確認として記録。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->
PDH-open -> PDH-ticket-review — Android標準のIME inline autofillをユーザの依頼へ対応付け、Who/WhatをACへ分解した。
PDH-ticket-review -> PDH-ticket-human-review — Android/Bitwarden公式資料でAPI 30以上の能力と、サービス側Inline設定が必要なことを確認した。AC読み手は目的を復元できると判定。軽微な曖昧さは通常候補との同時表示だが、確定判断の優先/復帰で解消する。
PDH-ticket-human-review -> PDH-implement — 同じ能力の実装をユーザが明示依頼済み。pdh-decision-boardの「板を出さない条件」に従い、同じ内容の再承認を求めない。追加の製品判断が出れば影響経路を止めて再確認する。
影響レイヤー: Android app、IME service、custom view UI、unit tests、instrumentation/device tests、docs。Mozc JNIは対象外。
根拠: Android公式 https://developer.android.com/identity/autofill/ime-autofill はIME XML宣言、request/response、provider Viewのinflateを規定。Bitwarden公式 https://bitwarden.com/help/auto-fill-android/ はAndroid 11以降、Bitwarden Autofill ServiceとInline表示、互換IMEを要求。現行 `app/src/main/res/xml/method.xml` にinline宣言はなく、`ImeService`にcallback実装はない。`minSdk=28`、`targetSdk=36`。既存候補barは50dp。
`Open Questions`は現時点で承認者専有の未決なし。`draft/TBD/TODO/FIXME/[NEEDS CLARIFICATION]`なし。既存の未確定文字プレビューticketは未クローズだが、標準inline APIの実装・テストを物理的に妨げない。マージ時に候補欄の差分を照合する。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた。Android API 30で入力開始後にrequestを受け、サービスが返す候補はIMEから内容を読めない保護Viewとしてinflateできることを公式資料で確認。Bitwarden公式はAndroid 11+と対応IMEを必要条件とする。実際のBitwarden候補・選択結果は実装後にproviderとアカウントが必要なため、PDH-verifyで実端末により確認する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->
2026-10-27 10:56 JST: 独立QAがAndroidX標準のinline UI provider契約を調べ、IME側の`InlinePresentationSpec` style Bundle未設定ではv1 UI候補を返さないproviderがあると指摘した。Android公式`InlinePresentationSpec.Builder.setStyle`はstyle必須と記載。Bitwardenの現在版がこのhandshakeを必ず要求する直接証拠は未取得。実装担当へ伝え、v1 style交渉と実service経路の検証を要求した。参照: https://developer.android.com/reference/android/widget/inline/InlinePresentationSpec.Builder 、https://developer.android.com/reference/androidx/autofill/inline/v1/InlineSuggestionUi 。
2026-10-27 10:57 JST: 物理Android端末のADB接続はゼロ。ユーザへBitwardenログイン済み端末の接続可否を非同期で質問した。並行してQAは別scratchで無害な固定値を返す実AutofillService APKを用意する。これはAndroid framework transportの証拠で、Bitwarden本体の互換性証拠とは区別する。AVD Medium_Phone_API_36.1は利用可能。API28/29のAVDは現時点で存在しない。
2026-10-27 10:59 JST: QAの独立scratch `/private/tmp/md-kbd-inline-autofill-qa` に、username/password入力欄とAndroidX v1 style確認後に固定値を返す実AutofillService APKを作成した。`./gradlew -p /private/tmp/md-kbd-inline-autofill-qa :app:assembleDebug` はBUILD SUCCESSFUL。APK 3,257,153 bytes、SHA-256 `826f7f3ebc80347648620e1ac52f33e5ea0d45e66e1eb7c620be32f08035cd85`。実動作はIMEビルド後にエミュレータで検証予定。Bitwardenの実serviceを代替した証拠とは扱わない。
2026-10-27 11:02 JST: 実装担当がappだけを`0401d4c`としてcommitした。候補50dp欄へ保護Viewを並べる、AndroidX v1 style Bundle交渉、editor/view切替で古いcallbackを無効化する内容。Focused `:app:testDebugUnitTest` 50件は失敗0、Directorの`:app:assembleDebug`はBUILD SUCCESSFUL。独立reviewとAndroidエミュレータの実service経路検証を開始した。
2026-10-27 11:03 JST: ユーザはBitwardenログイン済み端末をADB接続できないと回答。実Bitwarden候補のE2Eはこの環境では実施できない。Android公式APIとQA用AutofillServiceで契約・transportを検証し、Bitwarden本体の未検証をhuman review資料へ明示する。
2026-10-27 11:04 JST: API36.1・411dpの実Android framework経路でQAサービスのFillResponse 1 dataset・inline renderは観測したが、`0401d4c`のIME候補欄は空白でtapしても入力されなかった。画像 `/private/tmp/md-kbd-inline-autofill-qa/username-inline.png`。AC1/2未達として修正まで検証gateを進めない。独立reviewと実装担当へ原因調査を依頼した。
2026-10-27 11:06 JST: QA用サービスの候補が出ない一方、通常欄で実キー`q`をtapすると入力された（`/private/tmp/md-kbd-inline-autofill-qa/no-autofill-typed.png`）。候補欄の背景は描画されているがchipはない。原因はresponse受信、inflate callback、view追加のどこかに絞って調査する。AndroidX v1非宣言の`uiExtras`を理由にrequest自体を拒否する条件も独立reviewで指摘された。
2026-10-27 11:08 JST: 独立review（対象SHA `0401d4ca0778509bd8e36f2b064b0aeaac2dd770`）で同じAC1/2に結び付く2件を採用した。保護`InlineContentView`が持つremote描画サイズ945x99を`CandidateStripView`が`WRAP_CONTENT`へ上書きし、IME側intrinsic幅ゼロのためchipが見えなくなる。Android 11 AOSPの空`uiExtras`を現実装が拒否する点もAPI30の候補表示を妨げる。両方fix nowとし、テストdoubleを実Viewのサイズ契約へ近づける。
2026-10-27 11:09 JST: 実装担当のfix `3909a6f`で保護Viewの実寸保持、空`uiExtras`受付、全inflate失敗時の通常候補復帰を入れた。独立reviewはこの修正差分に限定し、QAは同じAndroid AutofillService経路で再実行する。
2026-10-27 11:11 JST: QAの固定値AutofillServiceで、Android 36.1・411dpに候補chip `QA Autofill qa@example.test` が表示され、実tapでusername=`qa@example.test`、passwordの固定テスト値がAndroid自動入力から入り、重複入力なし。通常欄へ切り替えるとchipはなく、実キー`q`で`q`が入力された。840dp幅でもchip表示を確認し、`wm size`を初期値へ戻した。証拠 `/private/tmp/md-kbd-inline-autofill-qa/fix-3909a6f-phone-username-2.png`、`fix-3909a6f-phone-after-fill.png`、`fix-3909a6f-phone-ordinary-focus.png`、`fix-3909a6f-tablet-840dp.png`。Bitwarden本体ではない。修正差分`0401d4c..3909a6f`の独立再reviewは元3件解消、追加Critical/Majorなし。
2026-10-27 11:12 JST: `b00318aaea62ede21a33df0a4c9ddcb19cfb27e8`で`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel --connected`実行。suite summary原文は`PASS: fast-checks`、`PASS: android unit, lint, apk`、`PASS: android connected (real Mozc)`、`Passed: 3 / 3`。XML集計はunit 325件・connected 30件でfailures/errors/skippedすべて0。fixtureスクリーンショットはapp変更最終SHA `3909a6f`時の採取で、`b00318a`との差分は文書のみ。
2026-10-27 11:14 JST: 独立Surface Observerは411/840dpのchipと自動入力後の消去を視認。通常欄に`q`を打ったというQA口頭報告はスクリーンショットだけでは入力先が一意に読めないため、QAへUI dumpまたは追加撮影を依頼した。この点を証拠確定までAC3/4の通常入力経路の実画面証拠とは扱わない。
2026-10-27 11:15 JST: 独立AC Verifierの`b00318a`判定はAC1/2/4がPARTIAL / NOT VERIFIED、AC3がVERIFIED。QA fixtureでの標準経路PASSはBitwarden本体の候補表示・tapを証明しない。API28/29 guardのunit testはあるがAndroid 9/10実OSでのIME入力・popup非阻害は未確認。この2箇所をhuman reviewに残す。全体テストのconnected suiteがIMEの選択をGboardに戻したため、追加QAはmd-kbd再導入後に行う。
2026-10-27 11:17 JST: connected suite後のGboard状態で採った`fix-3909a6f-ordinary-q.*`は無効な証拠として棄却。`b00318a` APKを再導入しmd-kbdをIME選択後、通常欄へfocusして実キー`q`をtap。`/private/tmp/md-kbd-inline-autofill-qa/b00318a-ordinary-q-mdkbd.png`に通常欄の`q`が見え、同名`.xml`の`content-desc="QA ordinary input"`は`focused=true`・`text=q`。エミュレータは終了しAVDだけ残した。
2026-10-27 11:18 JST: PDH-review -> PDH-verify。AC3のみ実質VERIFIEDとしticketでcheck、AC1/2/4は部分証拠のまま未check。固定QA値の証拠画像をticketの`evidence/`へ保存し、未確認を隠さないclose判断資料`review-board.md`を作成した。API30と旧OSの実機検証環境はなく、大きな追加system imageを導入せずhuman reviewへ渡す。
2026-10-27 11:22 JST: PDH-verify -> PDH-human-review。独立AC VerifierはAC1/2/4を部分・未確認、AC3を確認済みと判定。独立Surface ObserverはQAサービス経路の候補表示・入力を観察。判断資料の独立読者が挙げた4件（closeと公開の混同、エミュレータ幅の未記載、AC3の証拠範囲、SHA/環境の紐付け）をすべて訂正し再読でblockerなし。Bitwarden本体とAndroid 9/10実OSの確認なしで閉じるか、人間判断待ち。
2026-10-27 11:58 JST: 利用者の実Bitwarden試験ではChromeのログイン欄の下にBitwardenの候補ポップアップが出て、キーボードが閉じた。再タップ後はmd-kbdが表示されるが候補欄は空白。これは期待するinline候補表示を実証せず、AC1/2は未達・未検証のまま。Bitwardenの「自動入力候補の表示」がInlineかPopupか利用者へ確認中。公式文書はPopup時の欄上表示と、Inline時のキーボード表示を区別している。設定確認後も再現するなら実端末のIME request/responseを調査する。
2026-10-27 12:05 JST: 利用者がBitwardenの候補表示をInlineへ変更後、Chromeの会員番号欄にfocusした実機スクリーンショットで、md-kbdの候補帯へBitwardenの候補2件が表示された。AC1の実Bitwarden表示面を確認。候補tap後に会員番号・パスワードが入力されるかは未報告のためAC2は未確認。スクリーンショットには個人の会員番号が含まれるためticket内へ複製しない。
2026-10-27 12:10 JST: 利用者が候補の白背景を指摘。AndroidX v1に空のstyleだけを渡したことで既定の白面になっていた。初案ではIME requestに通常候補と同じ角丸face・明暗別の面色と文字色を指定。後続のRobolectric資源問題により角丸drawableを撤去し、最終版では明暗別の面色と文字色を指定。Android 36.1のQA AutofillServiceで411dp相当・ダークは暗色候補、840dp相当・ライトは白候補と対比の取れる文字を画面で確認。実Bitwardenの新style反映、候補tap後の入力は未確認。

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
| 1 | 0401d4c `CandidateStripView`の保護View寸法 | Critical | remote contentの実寸を`WRAP_CONTENT`へ捨てるため実候補が0幅になる。QA実経路で空欄を再現 | fix now | AC1/2を妨げる。providerの実`layoutParams`を保持しzero-intrinsic testと実serviceで再検証 |
| 2 | 0401d4c Android 11の空`uiExtras` | Critical | AndroidX v1広告がない場合にrequestを拒否する | fix now | API30のAC1/2を妨げる。v1 styleを常時構成し空Bundle testを更新 |
| 3 | 0401d4c inflate null callback | Major | 全候補inflate失敗後もinline activeで通常候補を抑止し続ける | fix now | AC3の復帰動作を妨げる。完了と有効Viewを数え、0件なら復帰 |

PDH-review-2: `3909a6f3b0db9f93b73725eb567e655cb0b7229c` の修正差分のみ独立再review。上記3件は解消、修正差分に新Critical/Majorなし。Critical 1は実AutofillServiceでも再現手順がPASS。Critical 2は空Bundle unit testで固定し、API30実機は未使用。Major 3は完了数・有効View数の判定testで固定。実BitwardenとAndroid 9/10の端末テストは未実施。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->
候補欄のAndroid inline Autofill優先表示、providerの保護Viewとその実寸、空`uiExtras`、API30未満のguard、入力欄切替による破棄を`technical-reference.md`の判断36へ記録した。視覚仕様は`docs/reference/sites-native-spec.txt`、ユーザ手順は`site/manual.html`へ追加した。ブラウザmockはAndroidのAutofillServiceへ接続できないため、このOS連携を擬似的に再現せず、specとmanualに制限を明記した。

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

2026-10-27 12:24 JST: 色styleの最終形は既存の明暗別`candidate_background`をAndroidX v1 `ViewStyle.setBackground(Icon.createWithResource(...))`へ指定（`RestrictedApi`はv1 style builder使用箇所だけ局所抑制）。追加drawableはRobolectricの絵文字テスト失敗を誘発したため撤去。固定値QA Android 36.1の411dp相当画面でダークは濃灰・ライトは白を確認し、チケットevidence画像を更新。最終実装SHA `8313d89`の`test-all.sh --parallel --connected`はfast-checksとconnectedはPASS、unitが既存`ImeHideBarTest`で断続失敗。unit/lint/APKを分けたGradle実行はPASS。実Bitwardenの候補tap後の入力はまだ未確認。

2026-10-27 12:29 JST: Flyerの24時間レビューサイト `https://dteqzpbq.aboutme.style/` に修正版APKを差し替え、元APK SHA-256 `4611213617df838878e9e00440563c49217267cecdb295d6b6acb9f23f399d4a`・38,965,643バイト、分割ファイルのFlyer側SHA-256はローカルと一致。旧分割2件を削除。Bitwardenの候補tap後の実機挙動は未確認。
