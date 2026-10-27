# Work Notes: 261027-065123-make-ca-tap-tab

## Status: PDH-implement (Implementation)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [ ] ユーザ依頼: C/A キーの中央に T を表示し、中央タップを Tab にする。
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [ ] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [ ] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [ ] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
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
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

ユーザが「C/Aキーは真ん中にTを書いてtabにして」と明示依頼。AC 1 はその動作、AC 2 は既存の1キー修飾を維持、AC 3 はデモで同じ操作を確認する。Product Brief の「同じジェスチャー体系で素早く操作する」に整合。影響層: Android app、custom view UI、unit tests、instrumentation/device tests、docs。IME service の既存 Tab 送信経路を使用し、Mozc JNI は対象外。公開 surface はキーボードキー、ブラウザ操作デモ、説明文。未確定のユーザ判断・依存 ticket はない。ユーザの明示依頼を AC 承認として扱う。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

既存 `KeyboardLayouts.modifier()` は center が null、`KeyboardView.dispatch` は選択中の中央 tap だけ解除する。`KeyAction.Tab` は `TextInputController.sendTab()` で Android KEYCODE_TAB を送る。実装後は attached production KeyboardView の tap と上下・未割当方向を MotionEvent で測る。実装前に固定値を決めるための外部測定はない。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

既存の `modifier()` は中央 action がなく、`KeyboardView.dispatch` は非選択の中央タップを捨てていた。中央を `KeyAction.Tab` にし、保留中の中央タップだけは既存の解除を維持。描画では上C／中央T／下A、選択中は従来のA/C単独表示。ブラウザ mock では押したまま別キーを使った modifier pointer の release を Tab として誤入力しない。

実装前の `git log` と該当行の `git blame` を確認。`similarity-generic --language kotlin -t 0.7` はこの配布版で Kotlin 非対応、`similarity-ts -t 0.7 site/mock.html` は HTML 内JSを抽出しないため重複検出は適用不可。既存 helper と同じパターンを目視照合した。

Focused checks: `./gradlew :app:testDebugUnitTest --tests 'com.masuidrive.gestureime.keyboard.KeyboardLayoutsTest' --tests 'com.masuidrive.gestureime.keyboard.KeyboardViewTest' :app:compileDebugAndroidTestKotlin --console=plain` → `BUILD SUCCESSFUL in 7s`。初回 sandbox では Gradle wrapper cache lock にアクセスできず、権限を上げて同じコマンドを実行した。Android emulator `Medium_Phone_API_36.1` で `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.masuidrive.gestureime.keyboard.KeyboardViewVoicePunctuationTest#modifierCenterTabAndVerticalFlicksUseProductionMotionEventsAtPhoneAndTabletWidths --console=plain` → `Finished 1 tests on Medium_Phone_API_36.1(AVD) - 16`、`BUILD SUCCESSFUL in 9s`。412dp・840dp相当でQWERTY／記号の両方へproduction KeyboardViewのMotionEventを送った。tap、上／下、左／右未割当、閾値未満、中心復帰、cancel、修飾解除を確認。物理端末は未接続。

ブラウザは `agent-browser` でローカル製品ページの iframe と `mock.html` を確認。412px幅のMobile設定で中央C/T/Aが表示され横はみ出しなし（scrollWidth=innerWidth=412）、実クリック後の入力欄 value は `"\\t"`。上フリック後はAlt選択を表示し、続けて中央tapすると解除して value は `"\\t"` のまま。デスクトップのTablet設定でもC/T/A表示と中央クリック後のvalue `"\\t"` を確認。

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
