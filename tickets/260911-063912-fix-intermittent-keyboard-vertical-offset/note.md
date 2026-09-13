# Work Notes: 260911-063912-fix-intermittent-keyboard-vertical-offset

## Status: PDH-implement

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [ ] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [ ] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [ ] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: IME local layoutだけの変更で外部providerを使用しない
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [ ] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] 実機feedback: 候補なしの初期表示から最初のかな候補表示へ移ってもIME root総高とキー上端・下端を動かさない
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
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

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md` Design decision 18を訂正し、system navigation bottom insetを`KeyboardView`の初回measure前に4行の下へseedする契約を記録した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- 確認手順: 任意アプリで通常欄をfocusし、IMEをhide/showする。別入力欄とpassword欄へ切り替えても候補欄50dpと4行key clusterの上端・下端が動かないことを確認する。
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
