# Work Notes: 261012-083009-voice-candidate-cancel-and-silence

## Status: PDH-human-review (Awaiting user device review)

## Checklist
- [x] 音声認識の候補一覧へ「キャンセル」を追加する。
- [x] 無音が続いた後の発話で冒頭が欠けるか、現行動作と原因を調べる（250ms再開始待機とonReadyForSpeech前の受付不可の可能性を確認。連続録音を追加せず、準備中を表示して利用者に区別する）。
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み（既存の連続認識指定と今回の候補キャンセル依頼）
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: Android端末内の音声認識だけを使い外部provider/APIを使用しない。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザがレビュー結果と未観察範囲の提示後に「リリースして」と指示し、クローズを明示承認した。実機での確認実施は未申告

## PDH-ticket-review. Ticket contract check
ユーザの既存指定「候補選択してもすぐ次の音声認識に入ってほしい。キャンセルを押すまで」と、今回の「候補にキャンセルって選択肢が欲しい」を合わせ、候補内キャンセルは現在の候補だけを破棄して連続認識を続け、下段のキャンセルだけがレイヤーを終了する、と解釈した。影響レイヤー: Android app、IME service、custom view UI、unit tests、instrumentation/device tests、docs。Mozc JNI は変更しない。無音後に認識準備前の発話を確実に取得する保証は Android SpeechRecognizer にはないため、準備中と準備完了を表示で区別し、再起動間隔も実測して縮められる範囲を確認する。
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
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた。現行 `scheduleSilenceRestart()` は250ms遅延の開始前から `Recognizing` を表示し、`onReadyForSpeech` で `Recording` になる。候補の縦UIと連続再開経路は既存。実装後、候補取消と再開の状態順序をmock recognizerとIME connected検査で観測し、準備前に「認識中」が残るならAC3未達として止める。

## PDH-implement. 実装ログ
AC読み手は What と AC 全件だけから「候補だけ破棄して次の認識へ、下段キャンセルで終了、無音後の準備中と受付可能を区別」を復元でき、本質的な曖昧さはないと判定。既存の明示依頼により ticket-human-review の実装許可を満たす。
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

### 候補破棄と認識準備表示
- `VoiceRecognitionController` に `Starting` と ready gate を加えた。開始・無音再開始は `onReadyForSpeech` を受けるまで準備中であり、RMS 波形も準備完了前には送らない。端末 API が準備前の発話を採取できる保証はないため、250ms の既存再開始待機を短縮したと主張しない。
- 候補一覧の先頭に選択可能な「キャンセル」を追加した。これは candidate/editor token が一致する visible preview だけを controller で破棄して、同じ voice layer から新しい recognizer を開始する。下段のキャンセルは従来どおりレイヤー終了のまま分離した。generation を進めてから再開始するので、取消前 listener の callback は UI を復活させない。
- キーボードの status slot は `準備中` / `認識中` / `処理中` / `候補を選択` を `VoiceBackendState` から明示的に受ける。`KeyboardViewTest` で準備中の描画・accessibility label と認識中への遷移を検査した。
- focused test: `:app:testDebugUnitTest --tests VoiceRecognitionControllerTest --tests VoicePanelViewTest --tests ImeServiceVoiceHoldTest --tests KeyboardViewTest` は 2026/10/12 に `BUILD SUCCESSFUL`（114 tests）。今回の controller/IME/UI 状態と先頭キャンセル、cancel 後の support/ready 再開、stale callback、無音再開始を確認した。device/emulator は本 ticket 実装中は使用しなかったため、実機観察と全スイートは Director の後続 stage。
- `similarity-generic --language kotlin --threshold .7` は Kotlin を未対応として終了した（`Language 'kotlin' is not supported`）。対象コードが Kotlin のためこの規則の機械検査は skip。
- logical commit: `[261012-083009-voice-candidate-cancel-and-silence] feat(voice): add preview cancel and readiness status`。

### 接続検査の候補行とendpoint状態の修正
- `VoicePanelViewDifferenceTest` は先頭のCancelを候補文字列として `Spanned` にcastしていたため、実Viewの先頭行をCancelとして検査・clickし、残る候補行だけへ差分span検査を適用した。focused connected testでCancel callbackと各候補の差分色を同時に確認した。
- `onEndOfSpeech` 後にpartialが残ると旧実装では `Partial` を通知し続け、statusが「認識中」、波形も継続した。`speechEnded` gateでendpoint・遅延partial・`stop()`後を `Recognizing`（keyboardでは「処理中」）へ固定し、RMSを停止する。一方、保持したlatest partialはfinal結果なし時の既存500ms fallback候補に使う。
- `KeyboardView.setVoiceSessionActive(true)` の初期値を「準備中」にした。`ImeService` が同じmain loopで具体的statusへ設定する前にTalkBackが「認識中」とannounceする経路をなくし、LISTENINGだけで波形更新を受けるようにした。
- focused checks（2026/10/12）: unit/Robolectric `:app:testDebugUnitTest --tests VoiceRecognitionControllerTest --tests ImeServiceVoiceHoldTest --tests KeyboardViewTest --tests VoicePanelViewTest` は `BUILD SUCCESSFUL`。connected `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.masuidrive.gestureime.ui.VoicePanelViewDifferenceTest` は Medium_Phone_API_36.1 で1 test `BUILD SUCCESSFUL`。

### readiness boundary と状態遷移announceの再修正
- `onReadyForSpeech` 前に届くpartialはcontroller内でfallback候補として保存するが、`Starting` のままにして候補UI・「認識中」status・波形へは出さない。ready後のpartialから初めて表示する。これで表示上の受付可能境界を `onReadyForSpeech` に揃えた。
- `KeyboardView.setVoiceSessionStatus()` は有効なstatus変更ごとにvoice status virtual nodeのannouncementを送る。開始時の「準備中」に続き、readyの「認識中」もTalkBackへ通知される。同一statusの再設定は従来どおり無通知。
- focused unit/Robolectric（2026/10/12）: `:app:testDebugUnitTest --tests VoiceRecognitionControllerTest --tests KeyboardViewTest --tests ImeServiceVoiceHoldTest --tests VoicePanelViewTest` は `BUILD SUCCESSFUL`。準備前partialの非表示、ready後partial、準備中→認識中の2回のannounceを検査した。

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
| 1 | native status/TalkBack | Major | ready後の通知不足とspeech endpoint後の認識中残留 | いま直した | `1421430`と`eb5dcdd`でstatus遷移と通知を修正、focused/全スイートと再reviewで確認。 |
| 2 | ready前partial | Minor | onReady前のpartialで認識中へ進む例外経路 | いま直した | `eb5dcdd`でStarting維持と逆順callback testを追加。 |
| 3 | mock/doc parity | Minor | 候補取消の説明文と仕様の状態文言がnativeと不一致 | いま直した | `4d9384c`でmock二重正本と仕様を一致させた。 |

## PDH-verify. 証拠と限界
- 最終 native SHA `eb5dcdd649202e2c6bc3765097215ce7c2e929e4` の `scripts/test-all.sh --parallel --connected` は fast-checks / android unit, lint, apk / connected real Mozc の3/3 PASS。unit 298件、API 36 emulator connected 27件、failure/error/skip 0。初回は `ANDROID_HOME` 未設定でAndroid区分が実行不能、SDK指定後の並列実行は既知の `ImeHideBarTest.emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway` が1件失敗し、同test単独PASS、同SHA逐次全スイート3/3 PASS。レビュー修正後の最終SHAでは並列3/3 PASSした。
- 独立reviewは最終SHAでCritical/Major/Minor 0。AC裏取りAgentはAC 1〜3を最終SHAでVERIFIED。candidate取消、レイヤー滞在と再認識、旧callback拒否、無音再起動のready gate、TalkBack通知をproduction controller/service/viewと恒久テストで確認した。
- Surface ObserverはAPI 36 emulatorの412dp/840dpで実IMEの音声レイヤーと下段キャンセル終了を観察。端末内日本語音声モデルが無いため、実認識の候補一覧・無音後の状態遷移・候補取消は実音声では未観察。物理端末/Foldも未観察。画像 `tests/voice-unavailable-412dp.png`、`tests/voice-unavailable-840dp.png`。ブラウザmockの候補取消画面は `tests/voice-candidate-mock-412px.png` で、Android実認識の証拠ではない。
- 複数候補と取消は模擬認識器を介したAndroid実Viewのconnected試験とブラウザmockで別々に確認した。候補の先頭から次の認識へ戻る流れはservice unit試験で確認した。実機固有のcallback timing、準備中の発話冒頭を拾えるかは未確認であり、保証していない。
- `technical-reference.md`、`docs/reference/sites-native-spec.txt`、`site/manual.html`、browser mockを変更。`site/mock.html`と`docs/reference/mock-source.html`はbyte-identical。PDH配布物は変更不要。保存形式、外部API、権限追加なし。

## Technical reference 更新
`technical-reference.md` にonReadyForSpeechを境界とする準備・認識・処理の状態遷移と、候補内キャンセル後の再認識・古いcallback拒否を追記。`docs/reference/sites-native-spec.txt`、`site/manual.html`、browser mockも同じ区別へ更新。
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
2026/10/12、画像Pasteと音声候補取消の差分・テスト結果・端末内日本語音声モデル不足による実音声未観察を提示した後、ユーザが「リリースして」と指示。両ticketのcloseおよび公開の明示承認として扱う。ユーザ自身の実機確認は報告されていない。
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

### 実装前の仮定と測定
- `SpeechRecognizer` は `onReadyForSpeech` より前の発話を受け付ける保証を持たない。現行 controller は無音再起動の250ms待機中から `Recognizing` を通知し、`onReadyForSpeech` を受けても同じ表示だった。そこで準備中と受付可能を別状態にし、RMS入力も ready 後だけ採用する。
- 候補内キャンセルは既存の候補 tap と同じ `candidateUiToken` / editor token の検査を通し、候補だけを破棄して新規 recognizer を開始する。古い listener callback は controller generation で拒否される既存方式を用いる。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
