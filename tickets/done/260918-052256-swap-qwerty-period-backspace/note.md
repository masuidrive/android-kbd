# Work Notes: 260918-052256-swap-qwerty-period-backspace

## Status: PDH-close (Approved for publication)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内IMEのキー配置変更で外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
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

ユーザの「qwertyの.?キーと0.5wのバックスペースを交換して」という直接指示を、配置・幅・既存gestureの保持まで含むAC承認として扱う。QWERTY以外を変更しないため未確定のproduct判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた
  - 現行nativeはQWERTY 2行目が`C/A(0.5w)+a…l+BS(0.5w)`、3行目が`#!+z…m+,+.`で各10w。Backspaceはタップ削除・下フリックEsc、「.」はタップ`.`・下フリック`?`。
  - 現行mockも2行目のmodifierとBackspaceだけを5%幅、他キーを10%幅として同じ配置を描画する。
  - 記号レイヤーは独立したrow定義で、QWERTYのKeySpecだけを入れ替えれば配置を維持できる。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

実装前仮定: `text` helperはwidthを持たないため半幅句点には既存patternに沿った最小のwidth指定手段が必要。Backspace helperはwidth=1fでもタップ削除と下フリックEscを保持できる。native/mocked rowの合計幅をtestで測る。

2026-09-18: 変更前の`KeyboardLayouts.kt`とmockの`qwertyRows()`は、QWERTY 2行目右端に半幅Backspace、3行目右端に`.`を置いていた。これらの行は初期QWERTY実装`9d77f83e`由来であり、記号2行目の半幅Backspaceを維持する判断は`73165c9`でticket scopeとして固定されている。QWERTYだけを移動し、Symbolsのrow定義は変更しない。

2026-09-18: `text` helperへ既定1wのwidth引数を追加し、QWERTY 2行目末尾へ0.5wの`.`（tap `.`、down `?`）、3行目末尾へ1wの既存Backspace helper（tap削除、down Esc、left/up/right null）を配置した。layout testでQWERTY上2行の各10w、Symbols 2行目の半幅Backspaceと3行目の並び不変を固定した。attached production `KeyboardView`への`MotionEvent`で句点tap/down、Backspace tap/down、Backspace left/up/right no-opを確認した。

2026-09-18: 変更ファイルに対する`similarity-generic -t 0.7`はコマンドがPATHになくskip、`similarity-ts -t 0.7 site/mock.html docs/reference/mock-source.html`はHTMLを解析対象外としてexit 0だった。二つのHTMLは仕様上の保存正本同期であり、`cmp -s`はexit 0。Node静的layout契約検査もPASS。

2026-09-18: focused Gradleの初回は`KeyboardViewTest`のRect幅（Int）をFloat比較へ渡したテストコードのcompile errorで失敗した。`period.width().toFloat()`へ直した次の実行は、face幅はpaddingを含むため厳密な1:2ではない（旧期待16.5に対して実測13）と判明した。logical widthUnits=0.5/1.0はlayout testで固定し、production testは小さい句点faceと実touch経路を検証する形へ直した。retry実行の`./gradlew testDebugUnitTest --tests com.masuidrive.gestureime.keyboard.KeyboardLayoutsTest --tests com.masuidrive.gestureime.keyboard.KeyboardViewTest`はBUILD SUCCESSFUL（29 tasks, 2 executed, 27 up-to-date）。

2026-09-18: `agent-browser`でlocalhostのmockを実行した。phone幅ではQWERTY 2行目の`.`が19.59375px、3行目Backspaceが39.203125pxで、period tap=`.`、down flick=`?`、`asd`入力後のBackspace tap=`as`を確認した。tablet viewport 840pxではkeyboard幅840px、`.`=41pxとBackspace=82pxのいずれもviewport内だった。browser console errorsは0件。

2026-09-18: nativeも`KeyboardViewTest`で412px/840pxの両幅におけるQWERTY 2・3行の左右端がview内に収まり、半幅`.` faceがBackspace faceより小さいことを追加で確認した。最終focused commandはBUILD SUCCESSFUL（29 tasks up-to-date、先行する同一SHAの実行では2 executed, 27 up-to-date）。

2026-09-18: 論理commitは`510a79e [260918-052256-swap-qwerty-period-backspace] feat(qwerty): swap period and backspace`、`668b163 [260918-052256-swap-qwerty-period-backspace] docs(mock): mirror qwerty key swap`。

2026-09-18: review findingの反例として、変更前`82c47b9`では未影響のattached `KeyboardView`のSpace tapが`[KeyAction.CommitText(" ")]`を返す`KeyboardViewTest.space tap commits one space`をPASSで記録した。追加したproduction `MotionEvent` lifecycle testは、`.`のleft/rightが`verticalOnly`によりcenter tap `CommitText(".")`へ戻る不具合を検出したため、文字キーをvertical-onlyにする条件をup/downの両方を持つものだけに狭めた。これにより同じsingle-down形状の`,`も未割当のside/upをno-opとして扱うが、tapとdownの出力は維持する。修正後、同じSpace testを`--rerun-tasks`で再実行し、同じ`[KeyAction.CommitText(" ")]`契約をPASSした。

2026-09-18: `KeyboardViewTest.qwerty swapped targets retain full production gesture lifecycle`をshared `performGesture` helperで追加した。対象は`.`/Backspaceのthreshold未満、down選択後center復帰、`.`のleft/up/right no-op、両キーの`ACTION_CANCEL`後に残らないこと、Backspace center長押しのrepeatのみとrelease後の停止である。`KeyboardViewTest` focusedとkeyboard package全体を`--rerun-tasks`でPASSした。

2026-09-18: Surface Observerのmock反例では412pxでBackspace left/up/rightが`finish()`からtap削除へfallthroughした。最初のstrict方向ガードも`.` leftを隣の`l`へキー乗換えしてしまう反例を実ポインタで得たため、QWERTY専用`.`/Backspaceにstrict未割当方向stateを保持し、centerへ戻ればそのstateを消すようにした。SymbolsのBackspace helperは変更していない。412px実browserで30pxの`.` left/up/rightは空入力のまま、Backspace left/up/rightはseed `as`のまま、down→centerは`.`と`a`（`as`から1削除）を確認し、console errorsは0件だった。`cmp -s site/mock.html docs/reference/mock-source.html`はPASS。`similarity-generic`はPATHになくskip、`similarity-ts -t 0.70 --extensions html`はHTML内scriptを関数として解析せずduplicateなしでexit 0だった。

2026-09-18: review修正の論理commitは`44e4703 [260918-052256-swap-qwerty-period-backspace] fix(qwerty): preserve unassigned flick directions`、`de9009b [260918-052256-swap-qwerty-period-backspace] fix(mock): preserve qwerty unassigned flicks`。

2026-09-18: review-2の修正前反例として、412px実browserでQWERTY `.`を17px下へ動かすと空欄になり、nativeの17px center/tap契約と一致しなかった。strict keyにaxis lockと別の`strictFlickSelected`を置き、18pxでのみ選択済みにし、10px以下へ戻ったときだけ選択・axisをclearするようにした。strictの未選択11〜17pxは元のキーを保持してtapへ進め、選択済みでalt未選択の方向だけno-op、periodのdown選択は`?`出力へ進める。

2026-09-18: cache-bustingした412px実browserで、`.`の17px down=`.`、18px down=`?`、18px選択後17px=`?`、18px選択後10px中心復帰=`.`を確認した。Backspaceは17px downで`as`→`a`、18px down→17pxでEsc action、18px選択後10px中心復帰で`as`→`a`を確認した。通常`q` tapとSpace tapは`q `、Symbolsへ切替後の既存Backspace tapは`q `→`q`で不変、console errorsは0件だった。`cmp -s site/mock.html docs/reference/mock-source.html`はPASS。

2026-09-18: review-2修正の論理commitは`2d1a2b7 [260918-052256-swap-qwerty-period-backspace] fix(mock): align strict flick threshold`。

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
| 1 | `[test追従 / attached MotionEvent]` | Major | 入れ替え後の実キーに対するgesture回帰testが必須経路を網羅していない | 未解消 | `KeyboardViewTest.kt:82-104`は`.`のtap/downとBackspaceのtap/down/left/up/rightだけを送る。対象キーでのthreshold未満、選択後のcenter復帰、`ACTION_CANCEL`、`.`のleft/up/right、Backspace center長押しrepeatとrepeat後releaseを通さないため、たとえば「down選択後にcenterへ戻っても`?`/Escが送られる」「移動後のBackspaceがrepeatしない／releaseで余分なtap削除を送る」退行でもPASSする。`AGENTS.md`のgesture-routing必須検査に従い、同じattached production `KeyboardView`へこれらの実`MotionEvent`列とlooper時間経過を追加する。修正範囲は原則`KeyboardViewTest.kt`のみで、追加testが実装不具合を示した場合だけrouting側を直す。 |

対象SHA: `47b020ee4ffe691e22f0b32a68b8e12d41099fce`（base `b3736fbc7f616f6589e2a09bb74e0cfd16933fe9`）。Criticalなし、Major 1件、Minorなし。

- AC 1: `KeyboardLayouts.kt:20-28`でQWERTY 2行目末尾を`.5w`の`.`/down `?`へ変更し、`KeyboardLayoutsTest.kt:41-53`とattached `KeyboardViewTest.kt:82-96`がKeySpecとtap/downを確認する。mockは`site/mock.html:115-119,737-741,787-792,874-880`、操作仕様は`docs/reference/sites-native-spec.txt:39-47`で一致する。
- AC 2: `KeyboardLayouts.kt:28,99-108`で3行目末尾を1w Backspace（tap削除、down Esc、left/up/right null）とし、`KeyboardLayoutsTest.kt:54-58`、attached `KeyboardViewTest.kt:98-104`が基本経路を確認する。ただしfinding 1のgesture lifecycle検査が不足する。
- AC 3: QWERTY 1行目は10個の既定1w、2・3行目は`KeyboardLayoutsTest.kt:48-49`で各10wを固定する。高さ・gapを担う`KeyboardView`は未変更。Symbolsは独立row定義のままで、同test `60-67`と既存Symbols操作testが半幅Backspace、3行目順序、tap/downを固定する。
- AC 4: nativeは`KeyboardViewTest.kt:107-120`で412/840pxの右端と相対幅を確認する。mockと保存正本はbyte一致し、phone/tablet実ブラウザ実測とgesture結果が実装ログにある。specも上記配置へ更新済み。
- Design Decisions / Out-of-scope: QWERTYの2 KeySpecとその共有`text` helperのwidth引数だけをnativeで変更し、Symbolsを含む他layerのrow定義、入力処理、公開物には変更を広げていない。ticket contractは開始commitから変更されていない。
- Commit cadence: `510a79e`（native＋tests）、`668b163`（mock＋spec）、`47b020e`（evidence）の3論理commitで、mega-commitなし。
- Review検証: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel`はfast-checks、Android unit/lint/APKの2/2 PASS（2026-09-18）。このPASSはfinding 1の未追加event列を補わない。

### Reviewer / Surface Observer response (PDH-review-1)

- Finding 1は`KeyboardViewTest.kt`のattached production `MotionEvent` lifecycle testで解消した。新規testはthreshold未満、center復帰、cancel、`.`のleft/up/right、Backspace long-press repeat/releaseを送る。testが示した`.` left/rightのcenter tapへのrouting退行は`KeyboardView.kt`を最小修正して解消した。
- Surface Observerのmock Majorは、QWERTY専用strict方向stateと`finish()`のno-op分岐で解消した。412px実browserで`.`のleft/up/right=no input、Backspaceのleft/up/right=`as`維持を確認した。保存正本はbyte-identicalで、Symbols rowは変更していない。

### Findings (PDH-review-2)

対象SHA: `9ea5669bbbc548b73298b0cd7396c07b6459072d`。Criticalなし、Major 1件、Minorなし。

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | `[対称関係 / native-mock threshold]` | Major | mockのQWERTY strict方向がnativeより6px早くtapを抑止する | 未解消 | nativeは`GestureInterpreter`のselection閾値18dp未満をcenterとして扱い、追加test `KeyboardViewTest.kt:106-115`も17pxの`.`を`.`、Backspaceをtap削除と固定する。一方mockの`updateKeyFlick()`は`site/mock.html:232-240`で12pxから`flickAxis`を設定し、18px未満で`altSelected=false`でも`finish()`の`strictFlickDirections && flickAxis`分岐（同1157行）へ入りno-opにする。したがって空欄の`.`を17px下または横へ動かすとnativeは`.`、mockは空欄のまま、`as`上のBackspaceを17px動かすとnativeは`a`、mockは`as`のままとなる。30px left/up/rightの元Surface Majorは解消したが、AC 4のgesture threshold/hysteresis parityは未達。strict用に「18pxで選択済み」をaxis lockとは別stateで保持し、10px以内のcenter復帰でだけclearする。修正範囲は`site/mock.html`とbyte-identicalな`docs/reference/mock-source.html`、17px/18pxと選択後17px/10px復帰のbrowser検証である。 |

- 前回review Major: 解消。`KeyboardViewTest.kt:100-141`はattached production `KeyboardView`へthreshold未満、down→center、period left/up/right、cancel、Backspace long-press repeat/releaseを送る。追加testが露呈したperiod side誤発火も`KeyboardView.kt:1018-1023`で修正済み。
- 追加Surface Major: 30pxのQWERTY period/Backspace left/up/rightがtapへfallthroughする事象は、QWERTY専用strict stateと`finish()` no-opで解消。ただしfinding 1の12〜17px境界が残る。
- 未影響面: nativeの条件変更でSpaceは`KeyKind.SPACE`、通常lettersはup/down両方あり、Symbols文字はup/downなし、Symbols Backspaceは`KeyKind.BACKSPACE`のため既存分岐のまま。QWERTY commaだけperiodと同じsingle-down形状としてnull方向をno-opへ揃えるがtap/down KeySpecは不変。mockのstrict flagはQWERTY専用period/Backspaceだけに付き、Space、letters、`flickBackspace()`を使うSymbolsには波及しない。保存正本はbyte一致する。
- 修正前後証拠: note 70-74行に、未影響Spaceの修正前後同一出力、native testが検出したperiod side誤発火、mock実browserの修正前tap fallthroughと修正後30px no-opが記録されている。
- Commit cadence: `44e4703`（native＋MotionEvent tests）、`de9009b`（mock＋保存正本）、`9ea5669`（evidence）の3論理commitで、mega-commitなし。
- Review検証: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel`はfast-checks、Android unit/lint/APKの2/2 PASS（2026-09-18）。finding 1はbrowser境界testが存在しないためこのsuiteでは検出されない。

### Reviewer response (PDH-review-2)

- Finding 1はstrict選択stateをaxis lockから分け、18pxで確定・10px以下でclearするようにして解消した。412px実browserで17px tap、18px down action、選択後17px維持、10px中心復帰を`.`とBackspaceで確認した。
- strict flagのない通常letters、Space、Symbols Backspaceは同じbrowser操作で不変を確認し、保存正本はbyte-identicalのままである。

### Findings (PDH-review-3)

対象SHA: `933d7e558fe88987753a9ee5ee234abaca168e25`。Criticalなし、Majorなし、Minorなし。

- 前回Major（mockの12〜17px境界不一致）: 解消。`site/mock.html:230-250`はQWERTY専用strict keyについて、未選択17pxでは`strictFlickSelected=false`のままtapへ進み、18pxで選択を開始する。選択後17pxでは10pxのhysteresisを超えるためdown選択を維持し、10pxでは選択・axis・altをclearしてcenter tapへ戻る。`finish():1164-1168`は選択済み未割当方向だけをno-opとし、未選択17pxを抑止しない。noteのcache-busting済み412px実browser証拠は`.`で17px=`.`、18px=`?`、18→17px=`?`、18→10px=`.`、Backspaceでも17px=tap削除、18→17px=Esc、18→10px=tap削除となり、nativeの18dp選択・10dp復帰と一致する。
- 前々回Major（attached MotionEvent lifecycle）: 解消を維持。`KeyboardViewTest.kt:100-141`はproduction `KeyboardView`に17px threshold未満、24px down→center、periodのleft/up/right、両キーのcancel後tap、Backspaceのlong-press repeat/release停止を送る。`KeyboardView.kt:1018-1023`のsingle-down QWERTY key routingによりperiod/Backspaceの未割当方向はcenterへ誤fallbackしない。
- 追加Surface Major（mockのBackspace left/up/rightがtap削除）: 解消を維持。strict選択後にaltが選ばれていない方向は`finish():1165`でno-opとなり、30pxのperiod/Backspace left/up/rightを実browserで無入力・無削除とした証拠がある。center復帰ではstrict stateをclearするためtap操作へ戻る。
- AC 1 / AC 2: `KeyboardLayouts.kt:25-29,99-108`で0.5w period（tap `.`、down `?`）と1w Backspace（tap削除、down Esc、left/up/right null）を固定し、KeySpec testとattached MotionEvent testが実経路を検証する。mockも上記4境界および未割当3方向で同じ出力を持つ。
- AC 3: `KeyboardLayoutsTest.kt:41-67`はQWERTY 2・3行目の10w、両キーの幅・action、Symbols 2行目の0.5w Backspaceと3行目順序を固定する。nativeの変更条件はSpace、up/downを持つ通常letters、Symbols文字、Symbols Backspaceへ適用されず、mockの`strictFlickDirections`もQWERTY period/Backspaceだけに付く。実browserでも通常`q`＋Spaceが`q `、Symbols Backspaceが`q `→`q`で不変を確認済み。
- AC 4: nativeの412/840px attached view testは左右端をview内に固定し、mockのphone/tablet実測も横overflowなし。`site/mock.html`と`docs/reference/mock-source.html`はbyte-identicalで、`docs/reference/sites-native-spec.txt:39-47`の幅・配置・操作と一致する。
- Commit cadence: `2d1a2b7`はmockと保存正本のthreshold修正、`933d7e5`は検証証拠の記録で分離され、修正範囲は前回findingに対応する。ticket contract、Design Decisions、Out-of-scopeに変更なし。
- Review検証: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel`はfast-checks、Android unit/lint/APKの2/2 PASS（2026-09-18）。`cmp -s site/mock.html docs/reference/mock-source.html`もPASS。

## PDH-verify. AC 裏取り / Surface Observer

対象は実装SHA `933d7e558fe88987753a9ee5ee234abaca168e25`（最終review記録を含む確認時HEAD `aceba589ef856002085ca2a1beb62e0b5c22bd4e`）。AC 1〜4はすべて **VERIFIED**。Critical / Major / Minorの未解消はない。

- **AC 1 — VERIFIED:** QWERTY 2行目右端は0.5wの`.`で、tap `.`、下フリック`?`。`KeyboardLayoutsTest`が幅とactionを固定し、attached production `KeyboardView`の`MotionEvent` testが実touch targetのtap/downを通す。API 36.1 AVDの412dp相当実IMEでも画面座標tapで`.`、下フリックで`?`を入力した。
- **AC 2 — VERIFIED:** QWERTY 3行目右端は1w Backspaceで、tap削除、下フリックEsc、left/up/rightはno-op。production testはtap/down/未割当3方向に加え、17px threshold内、18px超の選択、center復帰、cancel、long-press repeat/release停止を実`MotionEvent`で確認する。API 36.1 AVDの412dp相当実IMEでも、tapで1文字削除、downで文字列を変えずfocus解除、left/up/rightで文字列不変を観察した。
- **AC 3 — VERIFIED:** QWERTY 1行目は既存の1wキー10個を維持し、native layout testは変更対象の2・3行目を各10w、Symbols 2行目の0.5w Backspaceと3行目順序を固定する。高さ・row gapの実装差分はない。実browserのSymbolsでも412px/840pxともrow幅内に収まり、Backspace tapは`asd`→`as`、downは文字列を維持して`Esc`を通知した。QWERTY専用strict stateはSymbolsの`flickBackspace()`へ付かない。
- **AC 4 — VERIFIED:** native、mock、`docs/reference/sites-native-spec.txt`はQWERTYの幅・配置・操作で一致し、`site/mock.html`と`docs/reference/mock-source.html`は`cmp -s` PASS。実browser 412pxではQWERTY 2行目`.`=19.59375px、3行目Backspace=39.203125px、両row右端402px、document幅412px。840px Tabletでは`.`=41px、Backspace=82px、両row右端830px、document幅840pxで横overflowなし。

Surface Observerはcache-busting後の実browser pointer入力で、QWERTY period / Backspace双方について17px=tap、18px=下方向選択、18→17px=選択維持、18→10pxおよびcenterへの復帰=tap、left/up/right=no-opを確認した。具体的な出力はperiodが`.` / `?` / `?` / `.` / 無入力、Backspaceが1文字削除 / Esc / Esc / 1文字削除 / 文字列不変だった。browser console errorはなく、phone/tabletとも横overflowはなかった。

Native Surface evidenceは`/tmp/qwerty-swap-phone-native.png`（773×1680、412dp相当）と`/tmp/qwerty-swap-840dp-native.png`（1575×1680、840dp相当）。いずれもAPI 36.1の実IMEで、2行目右端の半幅`.?`、3行目右端の全幅Backspace、各rowの横収まりを目視した。画像は一時証拠でrepositoryへは追加しない。

Focused再確認は`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests com.masuidrive.gestureime.keyboard.KeyboardLayoutsTest --tests com.masuidrive.gestureime.keyboard.KeyboardViewTest`がBUILD SUCCESSFUL。root実行の最終`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel --connected`は実装SHA `933d7e5`でfast-checks、Android unit/lint/APK、API 36.1 connectedの3/3 PASSだった。その後はreview記録のみでproduction/mock/test差分はない。

ドキュメント要否を確認し、操作仕様は`docs/reference/sites-native-spec.txt`、browser保存正本は`docs/reference/mock-source.html`へ同期済み。generic PDH配布物を更新する理由はない。`technical-reference.md`は行内キー配置を重複記載しないため更新不要で、下記「Technical reference 更新」と一致する。human review / closeは未実施のまま残す。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

2026-09-18: 該当なし。配置と操作の正本は`docs/reference/sites-native-spec.txt`であり、実装構造を説明する`technical-reference.md`にはQWERTYの行内配置を重複記載していない。

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
2026-09-18: QWERTYの変更内容、最終3/3 suite、API 36.1実IMEとbrowser mockの確認結果、review APKのfull pathを提示し、ユーザへ実機確認とclose可否の判断を依頼する。

2026-09-18: ユーザの「公開」を、提示済みのQWERTY変更に対するclose承認と、main統合・push・次版APKおよび製品ページ公開の指示として受領した。
