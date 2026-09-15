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
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
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
- 2026-09-15 review修正: 数字・電話・日時classを`IME_FLAG_FORCE_ASCII`より先に判定し、数字用途を常に`NUMBERS`にした。新規editorだけを自動選択し、同一editorの`onStartInput(..., restarting=true)`は手動選択した現在modeを維持する。password欄のprivate emoji testは、初期QWERTYを確認してから手動でEMOJIへ切り替え、専用private pickerとmaskを検証する契約へ更新した。
- 2026-09-15 review focused: 変更経路だけの8件は`--rerun-tasks`で`BUILD SUCCESSFUL in 5s`, `29 actionable tasks: 29 executed`。`ImeHideBarTest`・selector・lifecycle全47件の初回は変更外の`pickerGeometryRefreshSettlesAfterAnExternalWidthChange`が`expected 717 but was 783`で1件失敗し、同じ全47件の再実行は`BUILD SUCCESSFUL in 6s`, `29 actionable tasks: 29 executed`となった。この1件はretry-passとしてrootの最終full suiteで再確認する。
- 2026-09-15 壊していない側の前後記録: 通常text＋保存済み`SYMBOLS`は変更前focused 22件と修正後focused 47件の両方で`SYMBOLS`、email address＋保存済み`KANA`も両方で`QWERTY`。数値初回`NUMBERS`→手動`SYMBOLS`→同一editor restart後`SYMBOLS`→finish後の新規数値editor`NUMBERS`を新しいlifecycle testで固定した。
- 2026-09-15 review fix commit: `fbb1f0c`（numeric class優先、restart時の手動mode維持、private picker testの新初期mode対応、technical-reference整合）。
- 2026-09-15 AC verifier再現調査: selector/lifecycle/activityと同時実行した`ImeHideBarTest.pickerGeometryRefreshSettlesAfterAnExternalWidthChange`で、412px用paddingを保持したbodyが840pxへ広がり`expected 717 but was 783`になる失敗が2回報告された。こちらの同一4 class再現は修正前に51件成功1回だったが、AndroidXがpostしたpicker再構築をRobolectric main looperでdrainしても、端末のChoreographerが行う次の親measure/layout traversalは自動実行されないため、callback順で最終assert前のtraversal回数が変わることを確認した。
- 2026-09-15 test同期修正: 幅変更testはAndroidXのposted処理をdrainした後、pickerの厳密な`railRight..contentRight`境界と`isLayoutRequested=false`が成立するまで親のexact measure/layoutを最大8 traversal進める。bodyのleft/width/right、7列cell幅、rail proxy、settled後に追加layoutがないという既存assertは維持した。全descendantの`isLayoutRequested`を見る初案はRecyclerView内部の継続要求で8回後も停止判定できず失敗したため不採用とした。
- 2026-09-15 回帰確認: `ImeHideBarTest`、`EditorKeyboardModeSelectorTest`、`ImeServiceVoiceLifecycleTest`、`ImeTestActivitySafeAreaTest`の51件を同じ4 class構成で`--rerun-tasks`実行し、修正後2回連続で`BUILD SUCCESSFUL in 7s`、各`29 actionable tasks: 29 executed`。変更外のcategory遷移、空Recent、scroll、mask/rail、private picker、通常textの保存mode、numeric初回・restart lifecycleを含む全件が両方で成功した。
- 2026-09-15 layout test stabilization commit: `97024da`（Robolectricで端末相当の要求済み親traversalを明示し、厳密な最終geometry assertionを維持）。
- 2026-09-15 全unit再発とstate調査: `./gradlew testDebugUnitTest --rerun-tasks`の269件で幅変更testが再び`expected bodyWidth 717 but was 783`となった。8 traversal各回を計測すると、picker/親は840px、親paddingは最新geometryのleft 113/right 10、body位置left 113まで更新済みなのに、body measured widthだけ旧783pxでright 896だった。各回のlayout requestはroot/picker=`false`、body親/body=`true`で、`pickerDesiredWidths`やlayout signatureが412pxへ戻ったのではなく、ViewRootImplへattachしていない直接measureのRobolectric rootで子の`requestLayout()`が祖先へ伝播せず、同じexact measure specがrootのmeasure cacheに短絡されていた。
- 2026-09-15 最終同期修正: helperの各親traversal前にunattached rootへ`forceLayout()`を行い、実AndroidでViewRootImpl/Choreographerが要求済み次frameを測り直す条件を再現した。productionのpadding更新・observer・幅世代には不整合がなく、製品コードは変更していない。失敗時と同じ全unit 269件を`--rerun-tasks`で2回連続実行し、`BUILD SUCCESSFUL in 14s`と`BUILD SUCCESSFUL in 13s`、各`29 actionable tasks: 29 executed`。commit `89cddd7`。

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
| 1 | 回帰test | Major | password editor開始直後もEMOJI表示を期待する既存private picker testがAC 2のQWERTY自動選択と衝突 | 採用・修正済み | password開始直後のQWERTYとpicker非表示を確認後、手動EMOJI切替でprivate picker分離を検証するよう変更 |
| 2 | lifecycle | Major | 同一editorの`restarting=true`で手動選択modeが自動modeへ戻る | 採用・修正済み | `editorKeyboardMode`があるrestartは現在値を維持し、finish後の新規editorだけ再分類するtestを追加 |
| 3 | 分類優先度 | Major | NUMBER等と`IME_FLAG_FORCE_ASCII`併用時にQWERTYとなり、数字用途のテンキー要求を外す | 採用・修正済み | 数字・電話・日時classを先に`NUMBERS`へ分類し、非numericだけFORCE_ASCIIをQWERTYへ反映 |
| 4 | test安定性 | Minor | picker幅変更testが複合/全unit実行で再発し、旧body measured widthを保持する | 採用・修正済み | unattached Robolectric rootでは子のlayout要求が祖先へ伝播せずmeasure cacheが使われることをstate計測で特定。root `forceLayout()`で端末の次frame traversalを再現し、最終geometry assertionを弱めず全269件を2回連続成功 |

### Findings (PDH-review-2)

- reviewerは修正後HEAD `d0c9b3d`でCritical/Majorなしと判定した。前回Major 3件はproduction/test/technical-referenceへ反映済み。
- selector・lifecycle・入力テスト・private pickerのfocused suiteは成功し、picker幅変更testも独立2回成功した。Minorのretry-passは最終full suiteで再確認する。

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
