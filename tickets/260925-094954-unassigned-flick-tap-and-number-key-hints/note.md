# Work Notes: 260925-094954-unassigned-flick-tap-and-number-key-hints

## Status: PDH-implement (user-authorized gesture and release scope)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 入力のローカル gesture と静的サイトだけの変更で、外部 provider 経路はない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した
- [ ] ユーザ依頼: 未割当方向のフリックは見た目と追加振動を動かさず、通常タップとして入力する
- [ ] ユーザ依頼: テンキーの`-`・`.`キーへ割当済み方向の補助ラベルをEnterと同じ配置・選択表現で入れる
- [ ] ユーザ依頼: 修正版APKと製品サイトをレビュー後に公開する

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- 影響レイヤー: Android custom view UI・gesture routing、native unit / instrumentation tests、保存仕様、ブラウザmock、docs。IME service・Mozc JNI・変換は非変更。
- ユーザの二つの発話で未割当方向のtap fallback、移動時の表示と追加振動の抑止、テンキー`-`・`.`の方向ラベルが明示された。既存割当の変更は求められていない。
- 追加発話「公開までして」でリリース作業も承認された。ACは操作の観察可能な契約で、公開手順はChecklistで追跡する。Product BriefのAI-1〜AI-4に抵触せず、未決のプロダクト判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた
  - 現状`KeyboardView.pointerMove`は未割当方向でもhaptic、direction更新、label animation、timer取消を行い、release時の`dispatch`は`spec.value(direction)==null`で入力しない。`KeyboardViewTest`はQWERTY`.`の未割当方向を入力なしと期待する。モック`finish`にも未割当を入力なしとする分岐がある。実装でnative/mock両方を測り直す。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 実装前の未確認仮定: native `KeyboardView.pointerMove` とモック `pointermove` が未割当方向も選択扱いし、releaseで入力しないことを現行コード・既存テストで測定した。数値キーの既存割当と中心タップを`KeyboardLayouts`・モック定義で確認した。対象はAndroid custom view UI・unit/connected test・保存仕様・browser mock・manual。Mozc JNI、IME service、権限は変更しない。
- `e978bc2`: nativeで未割当方向をCENTERへ正規化し、方向変化hapticと選択表示を出さず中央動作を1回dispatchする。数値`-`・`.`の方向hintとattached production `MotionEvent` unit/instrumentationを更新。focused `:app:testDebugUnitTest --tests KeyboardViewTest --tests KeyboardLayoutsTest` PASS、emulator `:app:connectedDebugAndroidTest ...KeyboardViewVoicePunctuationTest` 8 tests PASS（412dp/840dp）。Robolectric ShadowViewでhaptic回数を直接数える手段はなく、CENTER保持とdown時の通常tap hapticを確認した。
- `19a3eef`: ブラウザモックの数値キー方向hint、選択状態、未割当の中央タップを実装。`site/mock.html`と`docs/reference/mock-source.html`をbyte一致させた。実ブラウザで`-`全方向、`.`全方向、中心復帰、取消、埋込モックを確認した。`similarity-generic`はHTMLを扱えず実行不能。
- `4c17f6e`: v0.15.22/code38の版番号、公開リンク、manual、release notesを用意。APK/site公開は全体検証後に実施する。
- `874b902`: review finding #1/#2を修正。未割当raw方向で長押しtimerを止め、専用数値hintと汎用down副ラベルの重複を除いた。focused unitとattached-view接続9 tests PASS。
- `ace7f3d`: review finding #3の配列以外のstrict/text/navigation/voice Backspace経路を中央tapへ修正。実ブラウザで元キー保持、割当方向、中心復帰、取消、backspaceを確認。ただし再reviewで18dp選択・10dp中心復帰のnative hysteresisとの差が見つかり、続けて修正中。
- `f685d87`: review finding #4を修正。mockのraw方向を保持し、native同等の18dp選択・10dp中心復帰とverticalOnly軸固定にした。
- `3085eee`: review finding #5を修正。QWERTY/記号BSとQWERTY`.`の上下だけに制限する誤判定を外した。独立reviewerが採用Critical #1〜#5の解消と新たなCritical/Majorなしを確認した。
- `59d9f87`: v0.15.22/code38のAPKとサイトを公開。GitHub Releaseから再取得した38,848,786 bytesがSHA-256 `5a4d23d75396fe00ec26108c1db2cffe3c8e3680f39536c539c2b227211253aa`でbyte一致。Pages commit `c886285a6204a35bf622b0a84e69b73c2ae86f69`がbuiltとなり、公開4 core filesがbyte一致。公開Chromeの412/840pxで`-`上/左、`.`上未割当を実操作し、overflowとmissing画像は0だった。
- 公開直後のAC裏取りでQWERTY英字の未割当左右へ18dp超移動すると`GestureInterpreter.move()`のverticalOnly horizontal経路がnullを返し、`KeyboardView.pointerMove()`のSelection側の`cancelTimer`を通らないと判明。保持するとアクセントpopupとLONG_PRESS hapticが出るためAC1未達。v0.15.22は既発行のまま残し、修正版をv0.15.23/code39として公開し直す。QWERTY英字の実MotionEventと中心長押し非退行を追加検証する。
- Surface Observerはv0.15.22 APKをAPI36エミュレーター412dpの実IMEへ入れ、QWERTY `e`を未割当左へ約80px動かして1秒保持するとアクセントpopupが出る反例を撮影した。`tmp/android-emulator-release-412dp-qwerty-e-left-hold.png`。同じAPKでテンキー`.`の未割当上下はそれぞれ`.`一文字だった。触覚の物理回数とFold実機は未観測。
- `8496338`: `GestureInterpreter`のverticalOnly水平18dp到達をraw Selectionとして一度だけ通知し、方向stateはCENTERを維持して`KeyboardView`の未割当timer取消分岐に到達させた。修正前は新規unit反例がFAIL、修正後focused unit PASS。attached production `MotionEvent`の新規ケースを含む接続テストはAPI36.1 emulatorで10 tests PASS（412/840dp、`a`左右24dpを450ms超保持してもaccentなし・`a`一回、中央保持はaccent維持）。Robolectricはhapticの最後の種類しか計測できず、実機振動履歴は別途Surface Observerへ依頼する。
- `3085eee`で全suiteを再実行し、fast-checks、Android unit/lint/APK、Android connected (real Mozc)の3区分すべてPASS。接続25 tests。物理端末の触覚回数は検証できず、追加haptic分岐が未割当で走らないことはコードとattached-view MotionEventで確認した。文書更新後の最終SHAでも再実行する。
- AC裏取りで旧版のBS/音声削除挙動がREADME、manual、reference/specに残ると判明した。実際の未割当=中央tap動作へ記述を修正した。
- `ace7f3d`時点の全suite結果（後続mock修正により最終SHAの証拠ではない）: `env ANDROID_HOME=/Users/masuidrive/Library/Android/sdk JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home bash scripts/test-all.sh --parallel --connected`。出力:
  ```text
  Summary
  PASS: fast-checks
  PASS: android unit, lint, apk
  PASS: android connected (real Mozc)
  Passed: 3 / 3
  ```
  connectedはemulator API 36.1で25 tests。skip表示はGradleの構成/ビルドタスクのみでテストskipではない。retry-passは観測されなかった。

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
| 1 | 長押しtimer | Critical | 未割当方向をCENTERに正規化してもraw方向が閾値越えした場合の長押しtimerが残り、アクセントpopup/BS repeat/追加振動が発生する | fix now | AC1の一回入力・追加振動なし・長押し維持に反する |
| 2 | native描画 | Critical | `five--`の汎用down副ラベル`,`と新規下hintが重複し、選択中も余分な`,`が残る | fix now | AC2のEnter型表示に反する |
| 3 | mock全キー | Critical | 配列キー以外のstrict/text/navigation経路で未割当が無入力またはslideになりnativeと異なる。正本仕様にBS旧記述も残る | fix now | AC3のモック同一動作に反する |

### Findings (PDH-review-2)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 4 | mockの閾値と中心復帰 | Critical | 割当方向を選択後、中心から11〜17dpの未割当方向へ動かすとmockだけ選択を解除する。nativeの18dp選択・10dp以内解除とverticalOnly軸固定に不一致 | fix now | AC3および割当済み方向・中心復帰の維持に反する |

### Findings (PDH-review-3)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 5 | mockのstrictキー判定 | Critical | QWERTY/記号のBSとQWERTY`.`を誤ってverticalOnly扱いし、下選択→右24dpまたは右24dp→下選択でnativeと逆の動作になる | fix now | AC3のnative/mock一致に反する |

### Findings (PDH-verify-1)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 6 | 英字アクセント長押し | Critical | QWERTY英字の未割当左右ではverticalOnly水平pathがnullを返し、長押しtimerが残る | fix now | AC1の選択表示・追加振動抑止に反する。native修正とattached-view反例テスト後に再公開する |

### PDH-review-4（8496338）

- 独立reviewerは新しいCritical/Majorなし、finding #6は解消と判定。verticalOnly水平18dp到達時にraw LEFT/RIGHTを一度だけ通知し、状態CENTERを維持する。`KeyboardView`は未割当分岐でtimerを止め、選択方向がCENTERのままなので追加hapticとlabel animationを出さない。
- 確定判断の対応: 未割当中央tap・追加振動抑止=`GestureInterpreter` raw通知+`KeyboardView` assigned guard。既存割当維持=`KeyboardLayouts`を変更せず新旧GestureInterpreterテストで確認。native/mock/manual一致=`site/mock.html`と保存正本のbyte一致、`site/manual.html`の表・文、公開実ブラウザとnative実MotionEventで確認。
- 非退行の前後入力: QWERTY `a`を中央で450ms超保持すると旧版・修正版ともaccentが開き先頭`à`を確定する。テンキー`-`上フリックは旧版・修正版とも`/`を一度確定する。変更対象の`a`左24dp保持だけ旧版はaccentが開いたが、修正版は開かず`a`を一度確定する。修正版のfocused unitとattached-viewテストで前後契約を固定した。
- 採用したCritical #1〜#6はすべてこのticketでfix nowとして修正した。非採用・先送り・record onlyのfindingは0件。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`の本件仕様を追加し、音声削除の古いtap限定記述を修正した。native/mock保存資料、visual spec、README、サイトmanualの旧BS説明も現在の未割当方向のtap fallbackへ合わせた。

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
