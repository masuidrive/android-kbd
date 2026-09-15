# Work Notes: 260915-064628-select-layer-from-editor-input-type

## Status: PDH-implement (Implementation)

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
- [ ] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: Android標準のローカル`EditorInfo`だけを読む変更で、外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [ ] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
- 2026-09-15: ユーザの「数字を要求しているときはテンキーレイヤー」「同じようなのもサポート」という明示依頼を、Android標準の入力class/variationで観察できる契約へ整理した。
- Product Briefの高速なレイヤー移動と整合し、入力文字列・入力種別を外部送信しないためAI-1〜AI-4を維持する。
- `imeOptions`のaction、専用電話配列、初期大文字などは入力レイヤー選択と独立したためout-of-scopeへ分離した。未確定の製品判断と依存チケットはない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）
  - 現状確認: `ImeService.onStartInput` は入力種別を見ず、保存済みレイヤーを常に復元する。
  - Android標準契約: `InputType` のclass/variationで数字・電話・日時・URI・メール・各パスワードを判別でき、`EditorInfo.IME_FLAG_FORCE_ASCII`でASCII要求を判別できる。
  - 既存テンキー確認: 数字に加えて `-`、`+`、`.`、`,`、`/`、`*` を入力できるため、符号付き数・小数へ既存配列を使える。
  - 実装後計測: 各入力種別の単体テストと、412dp/840dp相当で入力欄切替・入力ビュー再生成を観察し、AC未達なら公開を止める。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 2026-09-15 実装前の未検証仮定: `InputType.TYPE_MASK_CLASS`と`TYPE_MASK_VARIATION`でclass/variationを独立に判定できること、`IME_FLAG_FORCE_ASCII`がclassより優先してQWERTYを要求すること、同一editor中の手動切替はinput view再生成でも維持すべきこと、入力テスト画面へ標準`EditText`を追加すればAndroidがその`EditorInfo`をIMEへ渡すこと。Android SDK定数と既存`TextInputController.isPasswordField`、`ImeService`のcallback順を確認してから実装する。
- 2026-09-15 調査結果: `onStartInput`は保存済みmodeを無条件復元し、`onCreateInputView`も同じ復元を行うため、入力種別による選択を一方だけへ追加するとcallback順によって上書きされる。現在editor用modeを保存値と分離し、`onStartInput`/`onStartInputView`で同じ純粋選択関数を使い、`onCreateInputView`は現在editor用modeがあればそれを優先する。手動`SwitchLayer`は両方を更新する。
- 2026-09-15 Android SDKの`InputType`定数と既存private判定を照合し、数字classは符号・小数flagと数字password variationを含めて`NUMBERS`、電話・日時classも`NUMBERS`、URI・email address・web email address・文字/可視/web password variationと`IME_FLAG_FORCE_ASCII`は`QWERTY`、通常/名前/住所/message/未指定/未知は保存済みmodeへfallbackする純粋関数にした。`IME_FLAG_FORCE_ASCII`はclassより優先する。
- 2026-09-15 現在editor用modeを`editorKeyboardMode`へ保持した。`onStartInput`で毎回自動選択し、後続`onCreateInputView`で保持値を優先する。`onStartInputView`が先に来た場合だけ同じ選択を補完する。手動`SwitchLayer`と音声取消時の復帰は現在値も更新し、`onFinishInput`でeditor固有値を破棄する。
- 2026-09-15 入力テスト画面へ符号・小数付き数字fieldとemail address fieldを追加し、通常text/passwordと合わせてAndroid標準`inputType`を観察できるようにした。README、製品manual、technical-referenceの初期layer説明を同じ分類へ更新した。
- 2026-09-15 focused test初回は22件中2件失敗した。`closingVoiceLayerRestoresItsPreviousModeAndClearsCandidates`は音声復帰先を現在editor用modeへ戻していなかったためQWERTYへ再適用され、`manualLayerChoicePersistsAcrossRecreationAndBecomesOrdinaryEditorFallback`は非同期`SwitchLayer`完了前に保存値を読んでいた。前者は`leaveVoiceLayer`で復帰先を同期し、後者はproduction変更なしでmain looper完了を待つtestへ直した。
- 2026-09-15 修正後focused test: `./gradlew testDebugUnitTest --tests 'com.masuidrive.gestureime.EditorKeyboardModeSelectorTest' --tests 'com.masuidrive.gestureime.ImeServiceVoiceLifecycleTest' --tests 'com.masuidrive.gestureime.ImeTestActivitySafeAreaTest' --rerun-tasks` → `BUILD SUCCESSFUL in 6s`, `29 actionable tasks: 29 executed`、22件成功。412px/840pxの標準presetはkeyboard 228px/root 278pxを維持した。
- 2026-09-15 重複検出: `similarity-generic`が環境にinstallされていないためskip。新規純粋関数は1ファイル、testのcallback順3ケースは異なる状態遷移を固定し、共通化対象になるproduction重複は目視で見つからなかった。
- 2026-09-15 logical commits: `4bf2b8b`（純粋選択関数、IME lifecycle、単体・回帰test）、`009002b`（入力テストfieldと利用者/技術文書）。

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

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md` Design decision 32へ、入力class/variationのlayer分類、`IME_FLAG_FORCE_ASCII`の優先、自動選択と保存値の分離、手動切替だけが保存する契約を追加した。

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
