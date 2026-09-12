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
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

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
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

実装後に、キャンセル横の表示、候補1回確定、次recognizer開始、2回連続確定、キャンセル・editor切替・別レイヤー切替後の旧結果破棄、固定4行高、native/mockの対応を測る。API 36 emulatorに日本語端末内モデルが無いため、controller/service結合testで実callback系列を再現し、nativeでは非対応時にも4行高とキャンセル横表示が崩れないことを観察する。実発話は対応実機でのhuman review項目として残す。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

[2026/09/12 21:42 JST] 実装前調査: `VoiceRecognitionController.confirm()` は preview を無効化して Idle を通知し、`ImeService.commitVoiceCandidate()` は現在その後に元レイヤーへ戻す。候補確定後の同editor token再開始は service 側で commit 成功を確認してから controller を start する。controller の generation は start/cancel/confirm で旧callbackを破棄できる。表示は固定4行の `KeyboardView` 最下段で、通常認識状態だけへ追加する。private・editor/layer/IME境界と Unavailable は既存停止経路を維持する。git log/blame を確認済み。仮定: recognizer の `start()` を同一 editor token で呼ぶことで新generationが作られ、前session callbackは controller と service token guard の双方で拒否される。

[2026/09/12 21:54 JST] 実装: `ImeService`に連続音声session generationを置き、最終候補は`confirm()`の同期Idle callbackで候補を消した後、editor token・layer・private・generationを再確認して成功した`commitText`のときだけ同じtokenのrecognizerを開始するよう変更した。Cancel/別layer/onStartInput/IME終了はgenerationを無効化してcontrollerをcancelする。`KeyboardView`はCancel右の既存empty領域へ非actionの「認識中」を描画し、4行・Cancel/flick/tap/accessibility key nodeを変えない。公開mockもgeneration付きtimerで同じ2周フロー、Cancel後の旧timer破棄を再現する。native commit: `3f26b69`。

[2026/09/12 21:54 JST] 検証: focused Gradle `VoiceRecognitionControllerTest`、`ImeServiceVoiceHoldTest`、`KeyboardViewTest` PASS。controllerはconfirm→同token再startと旧listener結果の破棄、serviceは2回連続確定、Cancel/editor切替/error後のcommit/restart拒否、viewは認識中描画と非action・不変geometryを固定した。`scripts/test-all.sh --parallel` は fast-checks と Android unit/lint/APK の2/2 PASS。browserでは`site/mock.html`を412/840相当で実pointer swipeし、候補→1回commit→2周目候補→Cancel/QWERTY復帰を確認した。`node --check`、mock source mirror一致、fast-checksもPASS。

[2026/09/12 21:57 JST] 追加回帰: 同一previewのrapid double tapはcommit/restart各1回だけ、mutex待ち中にCancelしたqueued candidate tapはcommit/restartしないことをservice testで固定した。認識中表示はstandard presetの412dpと840dpの双方でCancel右へ描画され、Cancel keyのbounds・4行228dp・virtual key数が変わらないことをlayout testで確認した。focused 3 classは再実行してPASS。モックもMobile 412dpとTablet 840dpで実pointer swipeを観察した。

[2026/09/12 22:08 JST] review修正: 認識中表示をactive時だけの非clickable TalkBack virtual nodeとして公開し、active遷移ごとに`TYPE_ANNOUNCEMENT`を1回だけ送る。partial更新・同値再設定では送らず、inactive/errorでnodeを消す。Cancelのnode順・bounds・actionsは維持した。候補確定をeditorが拒否した反例では、confirm済みrecognizerを再起動せず元文字レイヤーへ戻す。queued candidate後のCancelに加えSwitchLayer、private/NO_PERSONALIZED、finish input view/input/destroy後の旧callback拒否、SMALL/STANDARD/LARGEの412/840dp表示、VOICE Cancelの右/下flickをservice/view testへ追加した。focused `VoiceRecognitionControllerTest`、`ImeServiceVoiceHoldTest`、`KeyboardViewTest` PASS。

[2026/09/12 22:19 JST] review再修正: virtual statusのboundsを先にhit-testしてtouch explorationのhover対象にした。active終了またはVOICE離脱の前にhover exitとaccessibility focus clearを送り、遅延したstatus ID照会には非可視だがcontent/boundsを持つnodeを返してExploreByTouchHelperのpopulate例外を防ぐ。status中央hover、focus後のinactive/mode離脱、Cancel nodeの非click性と順序を`KeyboardViewTest`で確認し、focused `KeyboardViewTest`と`ImeServiceVoiceHoldTest` PASS。

[2026/09/12 22:24 JST] review追補: hover終了はsynthetic eventではなく`dispatchHoverEvent(ACTION_HOVER_EXIT)`へ通してExploreByTouchHelper内部のhover IDもclearする。消えたstatusの遅延照会はhost外のnonempty boundsと`isVisibleToUser=false`で安全に処理し、次hoverで余分なexitを送らないことを固定した。full suiteで再現した`ImeHideBarTest`の158px/166px差は、AndroidX初回cell計算用の8dp provisional body heightを最終body heightとして要求した誤契約だった。初期overscanはcell attachまで保持し、最終的にはwrapper-owned viewportへ戻す。最終bodyとclipはviewport、picker bottomはcontrol topで一致することをtestへ記録した。

[2026/09/12 22:31 JST] 検証: 最初のfull runは`ImeServiceEnglishSuggestionTest.emojiCommitFlushesJapaneseEnglishAndSlashCompositionThenUpdatesRecentOnlyAfterSuccessfulCommit`でAndroidX `EmojiPickerBodyAdapter`のlayout loopに入り停止した。thread dumpで`EmojiPickerItems.getSize()`から繰り返すRecyclerView layoutを確認し、finalized bodyをglobal-layout bindが再度provisional化していたことを原因とした。bodyごとのprepared（初回一度）とpending（first cell attachまで）を分離して修正後、問題test単独は`--no-daemon`で12秒 PASS、focused 4対象は8秒 PASS、`scripts/test-all.sh --parallel`はfast-checks 5 checks PASSおよびAndroid unit/lint/APK 22秒 PASS。

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

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

[2026/09/12 21:54 JST] `technical-reference.md` decision 10/21、native/reference spec、README、manual、demo、公開mockを連続認識・Cancel右状態表示・停止境界へ同期した。

[2026/09/12 22:08 JST] decision 10をservice session generationとcontroller recognizer generationの実際の責務、active限定TalkBack状態nodeへ訂正した。

[2026/09/12 22:34 JST] リリース文書準備: `technical-reference.md` decision 10/21と突合し、README、製品紹介、操作モック説明、マニュアルのv0.13.0変更一覧、`docs/v0.13-release-notes.md`へ「認識中」、候補1回確定後の即時再認識、Cancelまでの継続、操作mockの同等フローを記載した。全APK導線は未圧縮の`gesture-ime-v0.13.0.apk`のままとし、site配下にAPK/ZIPを置かないことを確認した。PDH-review / PDH-verify / human reviewの完了判定はこの文書準備では更新しない。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

[2026/09/12 23:06 JST] v0.13.0をGitHub ReleasesへAPK単体で公開し、Hanger Sitesの製品ページ・操作mock・manualも更新した。human reviewでは対応実機の通常入力欄で、音声レイヤー進入後にCancel横へ「認識中」が出ること、候補を1件選ぶと1回だけ入力されて次の認識が始まること、2回以上繰り返した後にCancelで停止し旧候補が復活しないことを確認する。物理Fold/TalkBackは利用可能なら合わせて確認する。公開APKの再download hash一致、AVD/mock/自動testの証拠はPDH-verify節に記録済み。ユーザの明示close承認まではticketを閉じない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
現行は音声候補をタップすると`confirm`後に元レイヤーへ戻る。ユーザはキャンセルまで音声レイヤーを維持し、候補確定後すぐ次の認識を始めること、キャンセルの横へ認識中表示を置くことを明示した。既存のeditor tokenとrecognizer generationで、別入力欄への誤確定と旧callbackを拒否できる。固定4行、端末内認識、private欄の禁止、別レイヤーへのフリックは維持するため、未確定のproduct判断はない。

独立AC読み手は、停止条件と「キャンセルまで」の矛盾、エラー時の認識中誤表示、「すぐ」の観察不能を指摘した。通常session中とerror状態を分け、候補確定後に旧候補が消えて新recognizer状態へ戻ること、2周の連続入力、private・editor・IME・layer境界で旧callbackと候補tapが入力も再開始もしないこと、412/840dpの固定4行とtouch targetをACへ明記した。更新後はユーザ指示を変えず観察可能にしたためticket-human-review承認として扱う。
