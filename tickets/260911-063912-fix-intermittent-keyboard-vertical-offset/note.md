# Work Notes: 260911-063912-fix-intermittent-keyboard-vertical-offset

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: IME local layoutだけの変更で外部providerを使用しない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] 実機feedback: 候補なしの初期表示から最初のかな候補表示へ移ってもIME root総高とキー上端・下端を動かさない
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- ユーザの実画面報告と「後で処理」の指示を、再現後に修正する承認として記録した。
- WhyはProduct Briefの安定した外/内画面入力へ接続し、ACはscreen boundsとIME lifecycleで観察できる。
- `[PDH-open] -> [PDH-ticket-review] -> [PDH-ticket-human-review] -> [PDH-implement]`。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

- 添付画像ではかなkey cluster上にcandidate相当の空きがあり、保存かなlayerと旧QWERTYで4行intrinsic高も16dp異なっていた。前ticketでlayer間height差を解消した。
- 修正版前のAPKをAPI 36.1 AVDへ導入し通常欄を初回focusすると、candidateの「許可」だけが画面下端に現れ、KeyboardView全体が画面外へclipされる状態を観察した。
- 実装測定: onCreateInputViewの縦LinearLayoutでKeyboardViewが`height=0, weight=1`。IME windowがwrap/at-most測定する経路ではweighted childのintrinsic 228dpが親desired heightへ安定して寄与せず、candidate 50dpだけのwindowが成立し得る。
- 仮定: KeyboardViewをWRAP_CONTENTにして自身のonMeasure 228/256dpをroot desired heightへ必ず含めれば、host appの初回measure modeに依存しない。
- 反例基準: candidate 50dp、KeyboardView intrinsic高、last layer復元、bottom inset、voice control lifecycleを維持する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

[2026/09/13 11:56 JST] PDH-human-review -> PDH-implement — v0.15.3実機の同一Chrome入力欄で、候補なしの初期表示ではIME上端が低く、最初のかな入力で候補が現れると上端が約1行分上へ移動した。既存AC 1/3を未達と再判定した。候補View自体のvisibilityは現行コードで通常欄なら常時VISIBLEなので、候補内容更新に伴うrequestLayout、初回のwindow/inset計測、root WRAP_CONTENTの順序を実Viewで測り、候補あり・なしのroot measured heightを同一に固定する。

[2026/09/13 12:19 JST] 原因は初回measure後に遅延到着したsystem bottom insetだった。`KeyboardView.onMeasure()`はpaddingBottomをdesired heightに含むため、最初の候補描画が起こす次のlayoutでIME rootが上へ伸びていた。IME windowがすでにsystem navigation領域を所有しているため、IMEが生成する`KeyboardView`だけ`setOwnsSystemBottomInset(false)`とした。単体`KeyboardView`の従来動作は維持した。`e147481`で、412/840pxの初回measure→bottom inset 31px遅延到着→最初の候補描画の順に、root高・keyboard高・first key下端が不変の回帰testを追加した。候補内容とEnter geometryを固定する先行変更`c9e7125`は根本原因ではなく将来のEnter変更を隠すため、独立reviewを受け`0048c24`でrevertした。

[2026/09/13 12:19 JST] API 36.1 AVDの通常入力欄でかなlayerを表示し、候補なしと「さ」入力後の候補5件表示を比較した。どちらもInputMethod window frameは`[0,1545][1080,2400]`で完全一致し、候補欄上端・1行目key上端・最下段key下端も画像上で不変だった。

[2026/09/13 16:29 JST] v0.15.7の実機画像ではwindow下端とnavigation領域は安定していた一方、4行のkey pitchが高い「大」ではなく未選択fallbackの「標準」へ解決されていた。`c31ede9`で未保存・旧版upgrade・不正値のfallbackを「大」に変更し、明示保存済みの小・標準・大は維持した。412/840pxのかなDual Flickでinput viewを連続再生成しても248dpのキー領域を保持する回帰と、設定画面で明示した標準を再生成後も保持する回帰を追加した。
[2026/09/13 17:13 JST] ユーザ確認で、添付画像は高さ未選択の初期状態ではなく「大」を使用している状態からアプリ切替後だけ4行が縮んだ症状だと判明した。v0.15.8のfresh preferences検証は別経路であり、添付画像の問題を再現・解決した証拠にならない。AC 5を明示保存済みプリセットの切替時保持へ戻し、StatusをPDH-implementへ差し戻した。SharedPreferences値、`KeyboardUiState.heightPreset`、`KeyboardView`実測高、IME root/window高、density/configurationを同じ切替順で観測する。

[2026/09/13 17:39 JST] 保存値と`KeyboardUiState.heightPreset`がLargeのままでも、入力先切替時に親が前の短いIME frameを`EXACTLY`で渡すと、wideの4行が本来の62dp row pitchから約45dp/rowへ圧縮される状態を回帰testで再現した。添付画像も正常時の約163px pitchに対して約117px pitchで、プリセット間の10%差では説明できない。`1e3e9f2`でIME内の`KeyboardView`をintrinsic高で測り、too-short exact rootを候補欄と4行の子合計高へ戻した。初期値と後続listenerのnavigation inset種別もvisibility非依存のnavigation barへ統一し、600dp以上のLargeを従来の62dpへ復元した。v0.15.8で変更した未設定fallbackは原因と無関係だったためStandardへ戻した。

- 重複検出 skip: `similarity-generic`が実行環境のPATHに導入されていないため。変更は既存class内の測定分岐と単一のprivate root classに限定し、同型実装がないことを`rg`で確認した。

[2026/09/13 17:48 JST] 独立reviewで、Androidの実hostは返却rootを`WRAP_CONTENT`で保持するため、短いexact hostからrootへ届くのは`AT_MOST`であり、直接rootへ`EXACTLY`を渡した初稿testは本番経路を証明しないCriticalを採用した。`b2ba867`でtoo-short `AT_MOST`もintrinsic合計へ戻し、回帰をAOSPと同じexact FrameLayout host→WRAP_CONTENT rootへ変更した。あわせて最初のmeasure前にwidth specから絵文字・音声overlay高を同期し、wide Largeの絵文字pickerが初回だけ6px短くAz行へ重なる経路を修正した。focused testと全unitは成功した。

[2026/09/13 18:04 JST] 再reviewで、rootだけintrinsic高へ広げても短いexact hostがclipするCriticalを採用した。`e157bb3`でintrinsic超過時に実IME Windowを`MATCH_PARENT × WRAP_CONTENT`へ戻し、decorへ再layoutを要求するproduction経路を追加した。回帰testは初回clip、relayout要求、WindowManager相当の再測定後のhost/root同高、最下段accessibility keyの可視まで確認する。再reviewはCritical/Major/Minor 0、release blockerなし。最終` scripts/test-all.sh --parallel --connected`はfast-checks、全unit/lint/APK、実Mozc connectedの3/3 PASS。

[2026/09/13 18:04 JST] API 36.1 AVDを1768x2208・420dpiにし、設定でStandardを明示して短いInputMethod window `Requested h=792`を先に表示した。IMEを閉じてLargeを明示保存し、Settings検索という別アプリへ切り替えると初回から`Requested h=866`、frame `[0,1342][1768,2208]`へ復元し、prefsは`LARGE`のままだった。`docs/verification/v0.15.9-height-wide-standard-to-large-app-switch-final.png`で4行と最下段の可視を確認した。同じ866pxのまま絵文字を初回表示し、`v0.15.9-emoji-wide-large-first-open-final.png`でpickerがAz/BS control行へ重ならないことを確認した。

- `0647b58`: `KeyboardView`を`height=0, weight=1`から`WRAP_CONTENT`へ変更し、IMEの`AT_MOST`計測でもintrinsic高をroot desired heightへ含めるようにした。
- `9dd8a10`: private editorでcandidate stripを`GONE`にしていた別の50dp移動を`INVISIBLE`へ変更し、通常→password→通常のroot/keyboard高不変を固定した。
- `b7e551c`: private editorから始まるlifecycleでもinput view生成時点からstripを`INVISIBLE`にし、候補内容を描画せず50dpを保持するtestを追加した。
- Robolectricで候補欄50px、keyboard 228px、root 278pxを固定し、日本語候補・英字候補・音声Recording・PermissionRequiredの各内容でroot高が変わらない回帰testを追加した。
- API 36.1 AVDの修正前はcandidateの「許可」だけが下端へ出てkey clusterがclipされた。修正後は候補欄とQWERTY 4行が全表示され、初回表示とhide/showのkeyboard crop MD5はいずれも`eed456f4787b50f7cc3da06b21444638`、hide/show後と入力欄切替後の全画面PNGも同一だった。
- `scripts/test-all.sh`: fast-check 5件、全unit、lint、debug APK buildが成功。`connectedDebugAndroidTest`: API 36.1 AVD 7件成功。

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
| 1 | private editor切替 | Major | `GONE`でrootが50dp縮み、key上端が動く | 採用・解消 | `INVISIBLE`へ変更し通常→password→通常を回帰test化 |
| 2 | private editor初回生成 | Minor | `onStartInput`が先行すると新規stripが既定VISIBLE | 採用・解消 | `onCreateInputView`でもprivate visibilityを設定 |

- review-2: `0647b58` + `9dd8a10`で前Major解消、新規Critical/Majorなし。Minorも`b7e551c`で解消した。
- AC4は`KeyboardView.onSizeChanged`がwidth/height変更時にtargetを再構築し、`updateBottomInset`がpadding更新後にrebuild/requestLayoutする既存経路とWRAP_CONTENT rootが整合することを独立reviewで確認した。

### Findings (PDH-review-3: v0.15.3実機差し戻し)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | 候補更新のrequestLayout抑止 | Minor | 候補内容とEnter geometryの固定は根本原因でなく将来変更を隠す | 採用・解消 | `0048c24`で先行変更`c9e7125`をrevertし、遅延insetの所有者だけを修正 |
| 2 | technical reference | Minor | decision 18の「KeyboardViewがinsetを加える」は新契約と矛盾 | 採用・解消 | `89af79a`でIME window所有と単体Viewの従来動作を区別 |
| 3 | v0.15.4公開導線 | Major | release作成前のAPK URLは404 | 採用・未解消 | APK・site・文書はlocal確定済み。GitHub push/releaseへの自動承認reviewが明示承認の再取得を求めたため公開待ち |

- v0.15.4のコード最終reviewはCritical/Major/Minor 0件だったが、`ImeService`が生成するViewだけbottom inset所有を無効にする判断は実機の最下段侵入を見逃した。
- v0.15.4最終版の`scripts/test-all.sh --parallel`は2/2 PASS（unit 232件、lint、APK）、API 36.1 AVDの`connectedDebugAndroidTest`は13/13 PASS。当時の候補なし/ありInputMethod frameはともに`[0,1671][1080,2400]`だったが、最下段がnavigation barへ潜る実機観察を見逃した。
- v0.15.4の「IME生成時はbottom inset所有を無効化する」修正は誤りだった。候補なし初期表示が低く、最下段がnavigation barへ潜った。`deb7a80`でnavigation insetを初回measure前に4行の下へseedする方式へ訂正した。最新`app-debug.apk`の実画面では候補なし/ありともInputMethod frameが`[0,1545][1080,2400]`で一致し、4行目とEnterはnavigation barの上にある。
- v0.15.5最終コード`327d13c`で全unit 232件・lint・APK buildは2/2 PASS、API 36.1 connected testは13/13 PASS。最終`app-debug.apk`を再導入し、`docs/verification/v0.15.5-height-before-candidates.png`と`docs/verification/v0.15.5-height-after-candidates.png`を目視した。候補なし・「さ」候補あり・IME hide/show後はいずれもInputMethod frameが`[0,1545][1080,2400]`で、4行目とEnterはnavigation barの上にある。
- `af942fa`はAPI 30以上のinitial inset解決を訂正した。current window metricsが取得できる場合は、hardware navigationの0を含めて最優先する。取得不能な場合だけdecor inset、双方に有効値がなければlegacy navigation resourceへfallbackするため、回転・navigation mode・Fold再構成で古いdecor値を初回高さへ持ち込まない。
- v0.15.6最終コードで`scripts/test-all.sh --parallel`は2/2 PASS、API 36.1 AVDの`connectedDebugAndroidTest`は13/13 PASS。生成直後の`gesture-ime-v0.15.6.apk`を再導入し、`docs/verification/v0.15.6-height-before-candidates.png`、`v0.15.6-height-after-candidates.png`、`v0.15.6-height-after-reshow.png`、`v0.15.6-height-after-app-switch.png`、`v0.15.6-height-dark-after-reconfigure.png`を目視した。1080x2400では全状態がInputMethod frame `[0,1545][1080,2400]`で一致した。
- Fold相当の1768x2208へ実行中に再構成し、`docs/verification/v0.15.6-height-fold-wide.png`と`v0.15.6-height-fold-wide-after-candidates.png`を目視した。Dual Flickの候補なし・候補ありはともにInputMethod frame `[0,1332][1768,2208]`で、4行目とEnterはsystem navigation barの上に完全表示された。
- 独立reviewでv0.15.5のstale decor Major解消を確認し、新規Critical/Majorなし、release blockerなし。API 28でdecor未到着時にlegacy navigation resourceを読むproduction branchの実端末証拠がない点は、pure resolver反例testで値の優先順位を固定したうえで非阻害Minorとして記録した。
- 独立AC verifierはAC 1〜4をすべてVERIFIEDとした。phoneの候補前後・hide/show・app switch・Dark再構成、Fold相当幅のDual Flick候補前後、候補/音声状態と全layerのgeometry testを根拠に採用した。API 28 legacy branchの実画面未取得は上記Minorと同じ扱いで、AC未達やrelease blockerではない。
- v0.15.8 reviewはfresh preferencesの既定Largeと単純なinput view再生成だけを確認しており、明示保存済みLargeでの実アプリ切替症状を再現していないため、AC 5の根拠から除外した。
- v0.15.9 review-4の初稿は、returned rootの高さだけをassertしてexact hostのclipを見ていないCriticalを採用した。`e157bb3`で実WindowのWRAP_CONTENT再layoutと、host/root/最下段可視までの回帰を追加した再reviewはCritical/Major/Minor 0、release blockerなし。
- 独立AC verifierはAC 5をVERIFIEDとした。Standardの792px表示後にLargeを明示保存し、別アプリ初回focusで866pxへ戻った74px差が、wide Standard 228dp→Large 256dpの28dp×2.625densityと一致する。Surface Observerも候補欄、4行、navigation safe area、絵文字初回の固定control非重複に違和感なしと判定した。
- v0.15.9はAndroid commit `d35bc95`をGitHub ReleaseへAPK単体で公開した。再取得した38,785,034 bytesとSHA-256 `f31699e9029930d36c50c33cc384dd49ef9ed1bd8742b64e3a95042740ddaa94`はlocal APKと一致した。公式siteはmasuidrive.jp `b791a51`のPages run `34749323470`が成功し、公開core 4ファイルのbyte一致、412/840pxの横overflowなし、readonly editorへのmock key入力、日本語Dual Flick 4行を確認した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md` Design decision 18を訂正し、system navigation bottom insetを`KeyboardView`の初回measure前に4行の下へseedし、API 30以上のcurrent metrics（0を含む）を最優先する契約を記録した。
- Design decision 16を訂正し、未保存・不正値はStandard、600dp以上で明示選択したLargeは従来の62dp、明示保存済みの全presetはアプリ切替後も維持する契約を記録した。
- Design decision 18へ、exact hostからWRAP_CONTENT rootへ届くtoo-short `AT_MOST`を子のintrinsic合計高へ戻し、IME WindowをWRAP_CONTENTで再layoutする。overlayを初回measure前にwidth同期し、初期値と後続listenerで同じnavigation bar insetを使う契約を記録した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- 確認手順: 任意アプリで通常欄をfocusし、IMEをhide/showする。別入力欄とpassword欄へ切り替えても候補欄50dpと4行key clusterの上端・下端が動かないことを確認する。
- AC 5確認手順: 設定画面で「大」を一度明示選択して保存値を確認する。同じIME serviceのまま入力テスト→Chrome等の別アプリ→入力テストと切り替え、各初回focus、IME hide/show、Fold相当の幅変更後も「大」の4行高が維持されることを確認する。
- ユーザの明示close承認まではticketを閉じない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
