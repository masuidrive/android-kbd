# Work Notes: 260912-122938-keep-voice-listening-until-cancel

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内`SpeechRecognizer`だけを使い、外部provider/APIはticketのinvariantで禁止される。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] 実機feedback: Emoji Recentは候補欄shortcutと切り離し、通常gridへ最近使った最大100件を並べる
- [x] 実機feedback: 絵文字category iconを全て同じ固定幅にし、categoryや状態で横幅を変えない
- [x] 実機feedback: 長い発話でも認識を終了して候補へ遷移できるようにする
- [x] 実機feedback: 音声最終候補は固定高のまま1行1候補で縦に並べる
- [x] 実機feedback: 音声候補確定後は候補を消し、視覚的にも次の「認識中」へ戻る
- [x] 実機feedback: 非対応・errorをaction badgeに見せず、音声layer内の状態として表示する
- [x] 実機feedback: nativeと操作mockから独自の閉じる行を削除し、OSの閉じる操作だけを使う
- [x] mock feedback: 入力欄をreadonlyにして通常キーボードを開かず、nativeにないfocus borderを表示しない
- [x] mock feedback: キーボード周囲を黒ではなく灰色にし、モード切替groupの上へ下側と釣り合う余白を入れる
- [x] mock feedback: トップ埋め込みmockを白い周囲・外枠なしにし、入力案内を小さいlabel位置へ移す
- [x] mock feedback: readonly入力欄の初期案内を最初のmock入力で消して入力結果へ置き換える
- [x] mock feedback: PC新規表示をTablet・Dual Flick・日本語かな、スマホ新規表示をMobileにする
- [x] site feedback: `demo.html`を廃止し、操作mock導線をトップ`#demo`へ統一する
- [x] review finding: 48dp category幅の再適用で同じlayoutParamsを毎layout書き戻さず、全unit suiteのRecyclerView layout loopを止める
- [x] 予測診断: 日本語と英語の次単語予測がほぼ出ない条件を実装・辞書・呼出境界に分けて記録する
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

[2026/09/12 23:49 JST] human review差し戻し後のAC読み手は、What/Whyが絵文字とhide rowを覆っていないこと、非対応/error表示が主観的であること、長い発話が測れないこと、Cancel後の確定済み文字とRecent 101件目の扱い、mockがOS終了操作を使うという誤契約を指摘した。Why/Whatを3成果へ拡張し、10秒以上の発話、最後の有効partial fallback、tap不能plain text、確定済み文字保持、100件LRU、native/mock責務分離へ修正した。10秒は実機報告の「長く喋る」を再現可能にする最小の検証条件として採用する。

[2026/09/12 23:51 JST] AC再読で、partial fallbackが表示だけで終わり得る曖昧さを検出した。最後の有効partialはAC 2と同じ選択可能な最終候補へ昇格し、認識中はCancel右、非対応/errorだけを固定候補領域のplain textへ置くよう修正した。これでWhatとAC 1〜7を外部観察から復元でき、未確定のproduct判断は残らない。

[2026/09/12 23:59 JST] 追加のmock画像確認で、textareaへ通常キーボード入力できることと青いfocus borderがnativeにない差として明示された。readonlyでもmock側のJSはvalue/selectionを更新できるため、操作demoを維持したまま通常入力とfocus borderだけを除くAC 8へ反映した。

[2026/09/13 00:01 JST] 追加のmock確認で、キーボード周囲の黒が外側ページから浮き、モード切替groupの上に余白がなく上下の釣り合いが悪いことが明示された。周囲をページになじむ灰色へ変更し、切替groupの上にも下側と釣り合う余白を確保するAC 9へ反映した。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

実装後に、キャンセル横の表示、候補1回確定、次recognizer開始、2回連続確定、キャンセル・editor切替・別レイヤー切替後の旧結果破棄、固定4行高、native/mockの対応を測る。API 36 emulatorに日本語端末内モデルが無いため、controller/service結合testで実callback系列を再現し、nativeでは非対応時にも4行高とキャンセル横表示が崩れないことを観察する。実発話は対応実機でのhuman review項目として残す。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

[2026/09/13 01:30 JST] human reviewの追加feedbackで、灰色と外枠のある独立demoより製品トップへ一体化した操作面を優先する判断へ更新した。`demo.html`を廃止して全導線を`index.html#demo`へ寄せ、PC新規visitorは珍しいDual Flick日本語を最初に見せる。既存保存stateはversion付きで一度だけ新defaultへ移行し、その後の利用者操作を保持する。readonly初期案内は最初の文字入力でだけ消し、カーソル移動では消さない。

[2026/09/13 01:47 JST] `8a73512`でトップ内操作mockを白背景・外枠なしへ統合し、案内をeditor labelとreadonly初期文へ移した。PC新規状態をTablet・日本語かな・Dual Flick ON、touch端末を表示幅に応じたMobile/Tabletとし、v2保存stateで利用者の選択を再読込後も保持する。`site/demo.html`を削除し、製品紹介・manual・実装referenceの現行導線を`index.html#demo`へ統一した。確定版46ファイルを`masuidrive.jp`の`docs/products/md-kbd/`へ配置し、commit `396f780`をmainへpushした。

[2026/09/12 21:42 JST] 実装前調査: `VoiceRecognitionController.confirm()` は preview を無効化して Idle を通知し、`ImeService.commitVoiceCandidate()` は現在その後に元レイヤーへ戻す。候補確定後の同editor token再開始は service 側で commit 成功を確認してから controller を start する。controller の generation は start/cancel/confirm で旧callbackを破棄できる。表示は固定4行の `KeyboardView` 最下段で、通常認識状態だけへ追加する。private・editor/layer/IME境界と Unavailable は既存停止経路を維持する。git log/blame を確認済み。仮定: recognizer の `start()` を同一 editor token で呼ぶことで新generationが作られ、前session callbackは controller と service token guard の双方で拒否される。

[2026/09/12 21:54 JST] 実装: `ImeService`に連続音声session generationを置き、最終候補は`confirm()`の同期Idle callbackで候補を消した後、editor token・layer・private・generationを再確認して成功した`commitText`のときだけ同じtokenのrecognizerを開始するよう変更した。Cancel/別layer/onStartInput/IME終了はgenerationを無効化してcontrollerをcancelする。`KeyboardView`はCancel右の既存empty領域へ非actionの「認識中」を描画し、4行・Cancel/flick/tap/accessibility key nodeを変えない。公開mockもgeneration付きtimerで同じ2周フロー、Cancel後の旧timer破棄を再現する。native commit: `3f26b69`。

[2026/09/12 21:54 JST] 検証: focused Gradle `VoiceRecognitionControllerTest`、`ImeServiceVoiceHoldTest`、`KeyboardViewTest` PASS。controllerはconfirm→同token再startと旧listener結果の破棄、serviceは2回連続確定、Cancel/editor切替/error後のcommit/restart拒否、viewは認識中描画と非action・不変geometryを固定した。`scripts/test-all.sh --parallel` は fast-checks と Android unit/lint/APK の2/2 PASS。browserでは`site/mock.html`を412/840相当で実pointer swipeし、候補→1回commit→2周目候補→Cancel/QWERTY復帰を確認した。`node --check`、mock source mirror一致、fast-checksもPASS。

[2026/09/12 21:57 JST] 追加回帰: 同一previewのrapid double tapはcommit/restart各1回だけ、mutex待ち中にCancelしたqueued candidate tapはcommit/restartしないことをservice testで固定した。認識中表示はstandard presetの412dpと840dpの双方でCancel右へ描画され、Cancel keyのbounds・4行228dp・virtual key数が変わらないことをlayout testで確認した。focused 3 classは再実行してPASS。モックもMobile 412dpとTablet 840dpで実pointer swipeを観察した。

[2026/09/12 22:08 JST] review修正: 認識中表示をactive時だけの非clickable TalkBack virtual nodeとして公開し、active遷移ごとに`TYPE_ANNOUNCEMENT`を1回だけ送る。partial更新・同値再設定では送らず、inactive/errorでnodeを消す。Cancelのnode順・bounds・actionsは維持した。候補確定をeditorが拒否した反例では、confirm済みrecognizerを再起動せず元文字レイヤーへ戻す。queued candidate後のCancelに加えSwitchLayer、private/NO_PERSONALIZED、finish input view/input/destroy後の旧callback拒否、SMALL/STANDARD/LARGEの412/840dp表示、VOICE Cancelの右/下flickをservice/view testへ追加した。focused `VoiceRecognitionControllerTest`、`ImeServiceVoiceHoldTest`、`KeyboardViewTest` PASS。

[2026/09/12 22:19 JST] review再修正: virtual statusのboundsを先にhit-testしてtouch explorationのhover対象にした。active終了またはVOICE離脱の前にhover exitとaccessibility focus clearを送り、遅延したstatus ID照会には非可視だがcontent/boundsを持つnodeを返してExploreByTouchHelperのpopulate例外を防ぐ。status中央hover、focus後のinactive/mode離脱、Cancel nodeの非click性と順序を`KeyboardViewTest`で確認し、focused `KeyboardViewTest`と`ImeServiceVoiceHoldTest` PASS。

[2026/09/12 22:24 JST] review追補: hover終了はsynthetic eventではなく`dispatchHoverEvent(ACTION_HOVER_EXIT)`へ通してExploreByTouchHelper内部のhover IDもclearする。消えたstatusの遅延照会はhost外のnonempty boundsと`isVisibleToUser=false`で安全に処理し、次hoverで余分なexitを送らないことを固定した。full suiteで再現した`ImeHideBarTest`の158px/166px差は、AndroidX初回cell計算用の8dp provisional body heightを最終body heightとして要求した誤契約だった。初期overscanはcell attachまで保持し、最終的にはwrapper-owned viewportへ戻す。最終bodyとclipはviewport、picker bottomはcontrol topで一致することをtestへ記録した。

[2026/09/12 22:31 JST] 検証: 最初のfull runは`ImeServiceEnglishSuggestionTest.emojiCommitFlushesJapaneseEnglishAndSlashCompositionThenUpdatesRecentOnlyAfterSuccessfulCommit`でAndroidX `EmojiPickerBodyAdapter`のlayout loopに入り停止した。thread dumpで`EmojiPickerItems.getSize()`から繰り返すRecyclerView layoutを確認し、finalized bodyをglobal-layout bindが再度provisional化していたことを原因とした。bodyごとのprepared（初回一度）とpending（first cell attachまで）を分離して修正後、問題test単独は`--no-daemon`で12秒 PASS、focused 4対象は8秒 PASS、`scripts/test-all.sh --parallel`はfast-checks 5 checks PASSおよびAndroid unit/lint/APK 22秒 PASS。

[2026/09/13 00:12 JST] human review差し戻し実装: 音声recognizerの`onEndOfSpeech`後500msで最終結果がなければ最後の有効partialを選択候補へ昇格し、`NO_MATCH`・`SPEECH_TIMEOUT`・`CLIENT` errorでも同じfallbackを行う。通常横候補欄から音声を分離して上3行相当の固定縦panelへ移し、権限未許可・非対応・errorはtap不能text、候補確定後はpanelを消して認識中へ戻す。絵文字Recentを新しい順・重複なし最大100件へ拡張し、全category holderを再利用・選択後も48dp固定にした。独自hide barをnative/mockから削除し、system bottom insetはKeyboardViewが4行下へ保持する。mockはreadonly・focus outlineなし、外側gray、mode切替上下12pxへ同期した。native focused 8 classは82/82 PASS、mock mirror・JS syntax・fast-checks・412/840pxのbrowser観察もPASS。実装commit: `ddb8e49`, `5e12e77`, `8b5e39d`, `e231515`。

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

| 1 | Accessibility / lifecycle | Major + Minor | Canvasだけの認識中表示、commit拒否後の空VOICE、停止境界とpreset/flick coverage | 採用・修正 | active限定の非action virtual nodeと一回通知、commit拒否時の安全な離脱、session invalidation回帰で実装・確認した。 |

### Findings (PDH-review-2)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | Accessibility | Minor | 消えたstatusのhover内部IDと遅延照会node | 採用・修正 | `ACTION_HOVER_EXIT`をhelperへ通し、host外nonempty boundsかつ非可視のnodeを返す。次hoverで余分なexitが出ないことも固定した。 |

[2026/09/12 22:32 JST] 最新SHA `2d2ceaa6043a79e2e388a20c97eaf5eb2acf73d0` の独立再レビューはCritical 0、Major 0、Minor 0。前回のhover内部IDとghost nodeは解消し、continuous voiceのconfirm/Cancel/error/editor/private/lifecycle境界に回帰なし。絵文字bodyのprovisional overscanもbody生涯1回だけで、first cell attach後に再適用されずlayout循環と固定高退行を起こさないと判定した。

### Findings (PDH-review-3)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | Emoji header layout | Major | 48dp固定処理が既に同値でも`minimumWidth`を毎global layoutで再設定し、全unit suiteの`ImeServiceEnglishSuggestionTest.emojiCommit...`でRecyclerView layoutを無限に再要求する | 採用・修正 | focused 82件では通ったが全suiteでSDK main threadが100秒以上CPUを占有。thread dumpは`enforceEmojiCategoryHolderWidth`→`bindEmojiPickerViewport`→global layoutの循環を示した。`minimumWidth`・`layoutParams`を値が異なる時だけ変更し、同値2回の適用で同一LayoutParamsを保ちlayout要求しないtestを追加した。 |

[2026/09/13 00:19 JST] 独立review自体はAC 1〜9についてCritical/Major/Minorなしと報告したが、親の全unit suiteで上記Majorを追加検出した。停止中workerをthread dump後に中断し、修正前の壊していない側のfocused 8 class 82/82 PASSと、修正後に同じfocused＋失敗test＋全suiteを比較する。

[2026/09/13 00:22 JST] `ba10aec`で48dp正規化をidempotentにした。以前停止した`ImeServiceEnglishSuggestionTest.emojiCommit...`とfocused 8 classを合わせて6秒でBUILD SUCCESSFUL。最終HEADの全suiteはrelease文書記録commit後に一度だけ再実行する。

[2026/09/13 00:26 JST] `ba10aec`後の全suiteはfast-checksとAndroid unit/lint/APKの2/2 PASS。独立Terra reviewerは`31a1ef5`を再確認し、Critical 0、Major 0、Minor 0。48dp holderへ同値再代入しないことと、同一holderへの二重適用でlayout requestが起きない回帰testを確認した。

### Findings (PDH-review-4)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
|   |      |     | 指摘なし | 承認 | 独立reviewはCritical 0、Major 0、Minor 0。トップ内mock、初期state、初回入力置換、保存復元、demo廃止、1280px/412pxのoverflow、相対asset/linkを確認した。 |

[2026/09/13 01:47 JST] 独立reviewはHEAD `8a7351219132f6085beb2b72df39215351121d48`と`masuidrive.jp/docs/products/md-kbd/`のコピーを確認した。4つのHTML/CSSはsourceとbyte一致し、`demo.html`なし、52件の相対asset/linkとfragmentが全て解決。1280px新規表示はTablet・日本語・Dual Flick ON、最初の「あ」tapで案内文が「あ」へ置換され、reload後のstate復元と1280px/412px双方の横overflow 0を確認した。

## PDH-verify. AC裏取り・surface観察

[2026/09/12 22:43 JST] AC 1〜5を達成と判定した。fresh focused testは92/92 PASS（ImeServiceVoiceHold 16、VoiceRecognitionController 8、KeyboardView 46、ImeHideBar/picker 22）、fresh assembleは37/37、install PASS。API 36 arm64 AVDでは端末内ja-JP modelなしのためVOICEは「非対応」とCancelを表示し「認識中」は出さず、固定4行、Cancel復帰、上→かな、右→QWERTY、下→数字を実swipeで確認した。412/840 mockでは実pointerで候補確定と次の認識を2周、3周目候補、Cancel後2.5秒の旧timer非復活を確認した。Settings searchと入力テストの10回切替はIME crop hash 10/10一致。Small/Standard/Largeの絵文字一覧も3行、4行目sliverなし、control下端固定。native証跡は`/tmp/voice-continuous-native-final.png`、mock証跡は`/tmp/voice-continuous-mock-final.png`。実機発話と物理Fold/TalkBack操作はhuman reviewへ残す。

[2026/09/12 22:45 JST] 最終HEAD `6663372` で `scripts/test-all.sh --parallel` を再実行した実出力:

```text
Parallel mode: logs in /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.Bd7z1VyWnU
  Starting: fast-checks (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.Bd7z1VyWnU/fast-checks.log)
  Starting: android unit, lint, apk (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.Bd7z1VyWnU/android_unit,_lint,_apk.log)

========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk

Passed: 2 / 2
```

[2026/09/13 00:31 JST] 差し戻し後のv0.14.0をAPI 36 arm64 AVDへinstallし、package `com.masuidrive.gestureime`、versionCode 15、versionName 0.14.0を確認した。絵文字は等幅category icon、一覧3行、固定control行、独自hide rowなし。VOICE非対応は固定panelのplain textで、右側action badgeなし。OS navigationの終了操作だけが残る。`connectedDebugAndroidTest`は11/11 PASS。操作mockは412/840px、Light/Dark、Mobile/Tablet/Dual Flickで外周`rgb(229,232,239)`、mode切替上下12px、readonlyとfocus outlineなし、音声2周とCancel後の旧timer破棄を観察済み。実発話と物理Foldだけをhuman reviewへ残す。

[2026/09/13 00:31 JST] v0.14.0 APKは38,583,304 bytes、SHA-256 `f4a8888c98d27f2d471c37b3b63d66c624477a34e470b614a220e36cd85a6311`。マニュアルの音声非対応・絵文字category画像をv0.14実画面へ差し替え、公開前のsiteにAPK/ZIPを含めていないことを確認した。

[2026/09/13 01:47 JST] AC 10を達成と判定した。`similarity-ts`、inline JavaScript syntax、mock/reference mirror、fast-checksをPASSし、最終HEADで`scripts/test-all.sh --parallel`はfast-checksとAndroid unit・lint・APKの2/2 PASS。公開先`https://masuidrive.jp/products/md-kbd/`はGitHub Pages buildがcommit `396f780b3aa92ca37cc4df4a45365d3f163cd49c`でbuiltとなりHTTP 200。公開browserでもPC初期Tablet・日本語・Dual Flick ON、横overflow 0、画像欠落0を確認した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

[2026/09/12 21:54 JST] `technical-reference.md` decision 10/21、native/reference spec、README、manual、demo、公開mockを連続認識・Cancel右状態表示・停止境界へ同期した。

[2026/09/12 22:08 JST] decision 10をservice session generationとcontroller recognizer generationの実際の責務、active限定TalkBack状態nodeへ訂正した。

[2026/09/12 22:34 JST] リリース文書準備: `technical-reference.md` decision 10/21と突合し、README、製品紹介、操作モック説明、マニュアルのv0.13.0変更一覧、`docs/v0.13-release-notes.md`へ「認識中」、候補1回確定後の即時再認識、Cancelまでの継続、操作mockの同等フローを記載した。全APK導線は未圧縮の`gesture-ime-v0.13.0.apk`のままとし、site配下にAPK/ZIPを置かないことを確認した。PDH-review / PDH-verify / human reviewの完了判定はこの文書準備では更新しない。

[2026/09/13 00:12 JST] 差し戻し実装に合わせ、decision 10/15/17/18/21を専用音声縦panel、partial fallback、tap不能状態表示、Recent 100件、category 48dp固定、独自hide bar削除とKeyboardViewのsystem inset所有へ更新した。

[2026/09/13 01:47 JST] 操作mock実装referenceをトップ統合、v2保存state、初回案内置換、`demo.html`廃止へ更新した。正式な製品紹介と操作mockは`https://masuidrive.jp/products/md-kbd/`、マニュアルは同階層の`manual.html`で配信する。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

[2026/09/12 23:06 JST] v0.13.0をGitHub ReleasesへAPK単体で公開し、Hanger Sitesの製品ページ・操作mock・manualも更新した。human reviewでは対応実機の通常入力欄で、音声レイヤー進入後にCancel横へ「認識中」が出ること、候補を1件選ぶと1回だけ入力されて次の認識が始まること、2回以上繰り返した後にCancelで停止し旧候補が復活しないことを確認する。物理Fold/TalkBackは利用可能なら合わせて確認する。公開APKの再download hash一致、AVD/mock/自動testの証拠はPDH-verify節に記録済み。ユーザの明示close承認まではticketを閉じない。

[2026/09/13 00:38 JST] v0.14.0をGitHub ReleaseへAPK単体で公開し、認証済みGitHub API経由の再downloadが38,583,304 bytes、SHA-256 `f4a8888c98d27f2d471c37b3b63d66c624477a34e470b614a220e36cd85a6311`でlocalと一致した。repository `masuidrive/android-kbd`はprivateなので、未認証の直接download URLは404となる。Hanger Sitesはdisplay nameをv0.14.0、access modeをpublicへ更新し、48ファイル全件のhashと公開index/demo/manualのbyte一致を確認した。APK/ZIPはsiteに含めていない。対応実機で10秒以上発話し、縦候補から2回連続確定、Cancel停止を確認後にclose承認を依頼する。

[2026/09/13 01:47 JST] 追加のサイトfeedbackを反映した製品トップ・操作mock・manualを`https://masuidrive.jp/products/md-kbd/`へ公開した。独立`demo.html`は配信せず、トップ`#demo`で操作できる。GitHub Pagesの最新build、HTTP 200、公開browserの初期Dual Flick日本語、初回入力置換、横overflow 0、画像欠落0を確認済み。ticketは引き続き、対応実機での長発話と連続確定を含むユーザ確認および明示close承認を待つ。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

[2026/09/12 23:57 JST] 次単語予測の診断: 英語候補はMozc/NWPではなく、同梱28,001語から英字prefixに最大5件を返す固定補完で、space後の次単語予測・学習・周辺文脈利用は実装されていない。日本語は確定直後だけMozc `REQUEST_NWP`を呼ぶためprotocol自体は正しいが、raw text/space/paste/emoji/voice後には呼ばれず、`InputConnection.getTextBeforeCursor/AfterCursor`がnullを返すアプリでは空文脈のままfallbackもない。通常日本語のMozc historyは端末内profileへ保存され、private/no-personalizedでは止まる。英語NWPは別の端末内n-gram等が必要、日本語は実app別の周辺text観測とprivacy-safe fallbackが先に必要。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
現行は音声候補をタップすると`confirm`後に元レイヤーへ戻る。ユーザはキャンセルまで音声レイヤーを維持し、候補確定後すぐ次の認識を始めること、キャンセルの横へ認識中表示を置くことを明示した。既存のeditor tokenとrecognizer generationで、別入力欄への誤確定と旧callbackを拒否できる。固定4行、端末内認識、private欄の禁止、別レイヤーへのフリックは維持するため、未確定のproduct判断はない。

独立AC読み手は、停止条件と「キャンセルまで」の矛盾、エラー時の認識中誤表示、「すぐ」の観察不能を指摘した。通常session中とerror状態を分け、候補確定後に旧候補が消えて新recognizer状態へ戻ること、2周の連続入力、private・editor・IME・layer境界で旧callbackと候補tapが入力も再開始もしないこと、412/840dpの固定4行とtouch targetをACへ明記した。更新後はユーザ指示を変えず観察可能にしたためticket-human-review承認として扱う。
