# Work Notes: 260924-021245-swap-enter-flick-directions

## Status: PDH-close (User authorized publication after review)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] ユーザ依頼: EnterのPasteとC-jの上下フリック方向を逆にする
- [x] ユーザ追加依頼: 非変換中Enterの待機中ラベルを上C-j・中央Enter・下pasteへ離して配置し、native/mock/仕様を揃える
- [x] ユーザ追加依頼: 記号レイヤー3行目のTabを一番右へ移す
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: ローカルIMEのキー割当変更で外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに最終差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- Product Briefの同一ジェスチャー体系による素早い操作へ接続する。ユーザが上下の新割当を明示しており、未確定のproduct判断はない。
- 変換中でないEnterの上Paste・下Ctrl+J、中央Enterと左右no-op、変換中のカタカナ方向維持を外部観察できるACにした。
- 影響層: Android app、IME service、custom view UI、unit tests、instrumentation/device tests、docs、browser mock。Mozc JNIは非該当。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた
  - 実装着手前はnative `enterKey`とmock `pasteNavigation`が上C-j・下Pasteで、描画・accessibility・unit/instrumentation・正本・manualにも同じ方向が記録されていた。
  - production KeyboardViewへtap・上下・未割当左右・threshold・中心復帰・cancelを送る既存testの有無を実装担当が確認し、不足時は一般化した回帰testを追加する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `4da658911d2793cb3c33ebb5311e9dba8e7aac4d`: 非変換中Enterを全対象レイヤーで上Paste・下Ctrl+Jへ入れ替え、action基準の選択ラベル描画、native/mockの実操作test、正本仕様・manual・技術文書を同期した。
- `181bfa93d8d8abcea156ad2bbc98a940e5d7fd8d`: API 36.1 emulatorで顕在化したfractional pixelの1px丸め差を許容し、attached `MotionEvent`の中心復帰点をhysteresis境界上の10dpから明確に内側の9dpへ移してtestを安定化した。境界値10dp自体は`GestureInterpreter` unit testが固定している。
- `34678f6d9fa75b286aa360f90fc4a9c02b75b3a7`: reviewerが検出した現行visual reference 2文書の旧down-Paste記述を、上Paste・下C-jとaction基準の視覚軌道へ同期した。
- `be297ac`: Enter待機中の上`C-j`・中央`Enter`・下`paste`をnative/mockに追加し、記号3行目のTabを最右端に移した。Tabのattached view実tap検証と412px/840pxのbrowser画像を追加した。
- `b33b784`: 正本仕様、technical reference、manual、README、ticket記録を新しい配置へ同期した。
- 実装前に、変換中Enterの左/上カタカナ変換を維持すること、PasteとCtrl+Jの既存action自体は変えないこと、対象がKANA/NUMBERS/QWERTY/SYMBOLS/VOICEであることを実コードと既存testから確認した。
- focused検証: `:app:testDebugUnitTest --tests KeyboardLayoutsTest --tests KeyboardViewTest :app:compileDebugAndroidTestKotlin` は成功した。`site/mock.html`と`docs/reference/mock-source.html`のbyte一致、`git diff --check`、HTML similarity checkも成功した。汎用similarity toolはKotlin非対応のためKotlin差分には適用していない。
- API 36.1 emulatorで`KeyboardViewVoicePunctuationTest#nonconvertingEnterUsesPasteUpAndControlJDownOnEveryProductionSurface`を実行し1/1 PASS。production `KeyboardView`へ412dp/840dp、5レイヤーでtap・上下・未割当左右・threshold未満・中心復帰・cancelの実`MotionEvent`を送った。
- browser mockを412pxと840pxで実ポインタ操作し、上フリック中は`paste-selected`と`paste`、下フリック中は`control-j-selected`と`C-j`、下フリック確定後は`Ctrl + J`を観察した。両幅でaccessibility labelは「上フリックでPaste、下フリックでCtrl+J」、840pxの`innerWidth`/`scrollWidth`は840/840、browser errorは0件だった。clipboard権限がないlocal browserではPaste確定後に「Pasteを利用できません」となるが、上方向のaction選択と実行到達は確認できた。
- 最終候補`181bfa9`で`ANDROID_HOME=... JAVA_HOME=... scripts/test-all.sh --parallel --connected`を実行し、fast-checks、android unit/lint/apk、android connected(real Mozc)の3/3 PASSを確認した。初回は既存emoji rail testの840dp幅が253px対254pxで失敗し、単独再実行でも再現したため上記`181bfa9`で丸め差と境界値を修正してから全件を再実行した。
- 追加差分のfocused unitは`KeyboardLayoutsTest` 18/18、`KeyboardViewTest` 57/57 PASS。412pxと840pxのbrowser mockで待機中ラベルが上/中央/下へ離れ、記号行のTabが最右端で、横overflowが0であることを確認した。`site/mock.html`と保存正本はbyte一致。撮影した`evidence/enter-idle-phone.png`、`evidence/symbol-tab-right-phone.png`、`evidence/symbol-tab-right-tablet.png`はbrowser mockであり、native/実機スクリーンショットではない。
- 最新SHA`b33b784`で`test-all.sh --parallel --connected`を2回実行。1回目は既存AVDの空き292MBによるAPK install失敗、2回目は7.2GB空きの一時userdataで接続24件中23件PASS、1件はその新しいAVDに`ja-JP`音声モデルがない既存`VoiceRecognitionAvailabilityTest`で失敗した。今回追加した記号Tabの412dp/840dp実`MotionEvent` tapとEnter全レイヤーtestはconnected XMLで各PASS。並列unitは変更外の`ImeHideBarTest`で1〜2件揺れたが、同SHAで全unit 285件、lint、debug APKを単独Gradle実行してPASSした。
- 記録更新後のSHA`5554729`で`test-all.sh --parallel --connected`を再実行し、fast-checks、android unit/lint/apk、android connected(real Mozc)の3/3 PASS。先の一時AVDでの音声model失敗もこの実行では再発しなかった。
- retry-passは計4件で、並列unitの`ImeHideBarTest` 3件（`pickerBodyShowsOnlyFourFullTouchRowsAndHeaderKeepsTen48dpCategories`、`emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway`、`privateEditorSwitchesToItsDedicatedEmptyPickerWithoutReusingPublicPicker`）と、一時AVD初回の`VoiceRecognitionAvailabilityTest.reportsInstalledJapaneseModelSupport` 1件。SHA`6dd5499`の3/3 PASSはこれらの初回失敗を隠さない。変更対象のEnter/Tab connected testは初回からPASSした。

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
| 1 | doc sweep | Critical | 現行visual reference 2文書がPasteをdown選択と記述し、上Paste・下C-jの実装と不一致 | 採用・修正 | `34678f6`でup/Paste・down/C-jとaction基準animationへ同期 |

### Findings (PDH-review-2)

対象SHA `34678f6`。前回Criticalは解消し、baseからのnative/mock/test、変換中Enter非退行、mock正本byte一致、ticket不変、commit cadenceを独立再確認した。Critical 0 / Major 0 / Minor 0。

修正前後で壊していない入力としてnative/mockの上Paste・下C-jを選び、`d3e097e..34678f6`が当該2文書だけであることと、修正後fast-checks 5/5 PASSを確認した。非採用findingはない。

### Findings (PDH-review-3: user amendment)

今回の追加指示に合わせ、レビュー対象を旧方向入替の再審査から、待機中ラベル・Tab位置と正本/稼働中ticketの整合へ切り替えた。同一SHA`b33b784`の独立reviewで次を検出した。

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | doc sweep | Critical | technical-reference 14項がEnter/Pasteを固定13dpと書き、新しいキー高依存移動と矛盾 | 採用・修正 | Enterだけキー高依存で下端pasteから中央へ移す仕様へ修正 |
| 2 | AC sweep | Critical | openの基礎IME ticket AC6が古いEnter下Pasteを要求し、現在の上Pasteと矛盾 | 採用・修正 | 利用者の後続指示による上PasteへAC6を更新し、supersedeの根拠を記録 |
| 3 | record | Major | noteの実装・review・verify結果が追加前SHAのままで、新しいAC1/4/5の証拠と全suite結果がない | 採用・修正 | 追加2commit、browser/connected/unitの最新結果、初回環境失敗と再実行3/3 PASSを追記し、ACを再判定 |

同じreviewerによる修正範囲の再確認では3件とも解消、Critical/Major 0。新規のコード変更はなく、未変更のEnter gesture testと今回追加したTab testが一時AVDのconnected XMLでそれぞれPASSした。

## PDH-verify. 検証結果

- AC 1 VERIFIED: KANA/NUMBERS/QWERTY/SYMBOLS/VOICEの上Paste・下Ctrl+Jと選択ラベルを、412dp/840dpのproduction `KeyboardView`実`MotionEvent`およびaction基準描画testで裏取りした。
- AC 2 VERIFIED: 同じattached viewでtap Enter、左右no-op、threshold未満、中心復帰、`ACTION_CANCEL`を裏取りした。
- AC 3 VERIFIED: 変換中の中央無変換/確定、左/上カタカナ、右/下no-opを412dp/840dpの実`MotionEvent`で裏取りした。
- AC 4 VERIFIED: mockと保存正本のbyte一致、正本仕様、technical reference、manual、および現行visual reference 2文書の上Paste・下C-j同期を裏取りした。
- 追加後のAC 1/4/5は`b33b784`のunit 75件、mock byte一致、browser 412px/840px、connected XMLの`nonconvertingEnterUsesPasteUpAndControlJDownOnEveryProductionSurface`と`numberPeriodAndSymbolSwitchesUseProductionMotionEventsAtPhoneAndTabletWidths`各PASSで再裏取りした。AC5のTabは実`KeyboardView`で412dp/840dpの右端位置とtap送信を検証。実機は未確認。
- 独立AC verifierは追加SHAのAC1〜4をVERIFIEDとし、AC5はfresh connected結果待ちとしていた。今回のconnected XMLで`numberPeriodAndSymbolSwitchesUseProductionMotionEventsAtPhoneAndTabletWidths`がPASSしたためAC5もVERIFIEDと再判定した。音声モデル欠落によるsuite未完了と、実機未確認はhuman reviewへ明示する。
- Surface Observer: 412dp/840dp・全5レイヤーのproduction `KeyboardView`実`MotionEvent`と、412px/840px browser mockの実pointerでgesture routing・選択表示・横overflowなしを観察した。外部editorでのclipboard貼付とCtrl+J受信は未観察だが、executorは本ticketで変更していない。
- 本ticketは通常文書と製品文書を更新し、generic PDH配布物は変更していないため`pdh-update`は不要。`technical-reference.md`も下記内容と突合済み。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`の非変換中Enter仕様を上Paste・下Ctrl+Jへ更新し、action基準で選択ラベルとアニメーションを決める実装を記録した。変換中のEnter仕様は変更していない。
- 追加指示では待機中ラベルの上下配置と、Paste選択時のキー高依存の下端から中央への移動を14項へ反映した。旧固定13dp記述はその他の通常キーに限定した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

2026-09-24: 上Paste・下C-j、中央Enter、左右no-op、変換中Enter非退行、全suite 3/3 PASS、独立再review 0 findings、AC1〜4 VERIFIEDを提示する。412px幅の同一モックで上フリック中の`evidence/enter-up-paste.png`と下フリック中の`evidence/enter-down-cj.png`を撮影した。利用者には通常入力でEnterを上フリックして貼付、下フリックしてC-j表示、中央タップでEnterが維持されることの確認を依頼し、close・main統合・pushは明示承認まで行わない。

2026-09-24追加: Enter待機中ラベルを上C-j・中央Enter・下pasteへ離し、記号3行目のTabを右端に配置した。`evidence/enter-idle-phone.png`、`evidence/symbol-tab-right-phone.png`、`evidence/symbol-tab-right-tablet.png`はブラウザモックの画像である。API36.1 AVDでは対象のproduction touch testが両幅でPASSしたが、物理端末での確認はない。`test-all.sh --parallel --connected`は最終候補SHAで3/3 PASS。追加差分の独立reviewでCritical/Major 0、AC1〜5 VERIFIED。close・main統合・pushは人間の明示承認待ち。

2026-09-24レビューへの回答: ユーザが「バイナリつくってサイトも更新してね。これはレビュー終了後に常にやるようにagent.mdかなにかに書いておいて」と指示した。直前に提示したclose・main統合・pushを含む成果の公開を明示しているため、このticketのcloseを承認した回答として扱う。APKとサイトの公開作業、およびプロジェクト規則の追記は後続の公開ticketで記録する。

- close判断ボードの独立reviewは「追加調査なしで決められる」と判定した。外部editorでの実受信まで確認済みに読める表現と、close前後の停止条件の曖昧さを修正して提示する。採らなかった指摘は0件で、4面構成は組み直していない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- API 36.1 emulatorの840dpでは、同一視覚幅のKANA railとemoji railがweight配分のfractional pixel丸めにより1pxずれる場合がある。またpx→dp変換したちょうど10dpの中心復帰点は浮動小数誤差でhysteresis境界外になり得る。production geometry変更ではなくtest入力の不安定性だったため、幅は±1px、attached testの復帰入力は9dpとした。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

- 2026-09-24: 操作は上Paste・下C-jの合意を維持し、待機中だけ上C-j・下pasteを表示する。ユーザが表示位置を明示したため、そのまま採用する。
- 2026-09-24: ユーザ指定どおり記号3行目のTabを右端へ移し、文字キーの割当は維持する。

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
