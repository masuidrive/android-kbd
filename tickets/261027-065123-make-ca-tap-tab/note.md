# Work Notes: 261027-065123-make-ca-tap-tab

## Status: PDH-human-review (Human review)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] ユーザ依頼: C/A キーの中央に T を表示し、中央タップを Tab にする。
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: オフラインIME内のキーイベントとローカルHTMLだけで、外部providerは呼ばない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
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

論理commit: `739d168` native実装とunit／production MotionEventテスト、`a4dc33b` mock・仕様・手順・進捗。AC・不変条件・Out-of-scopeは実装中に変更なし。

Initial implementation SHA `a4dc33bbb7f6d13a2d36a09fdb4447fe625367c5` で `scripts/test-all.sh --parallel --connected` を1回実行。後述のreview指摘でAndroidのaccessibility実装を変更したため、最終SHAで再実行する。スイート自身の要約は次のとおり。

```
========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk
  PASS: android connected (real Mozc)

Passed: 3 / 3
```

詳細: fast-checks `5 checks passed (2127 ms)`、Android build `BUILD SUCCESSFUL in 28s`、接続試験 `Finished 31 tests on Medium_Phone_API_36.1(AVD) - 16`、`BUILD SUCCESSFUL in 43s`。接続試験のログは全件0 skipped／0 failed。Focused Android test とブラウザ操作は一時確認、Gradle unit とconnected testは恒久 application-test。

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
| 1 | TalkBack / modifier state | Major | 修飾選択中も中央タップをTabと読み上げ、実際の解除と一致しない。取消時の状態通知も欠ける | 採用・修正済み | AC 1/2 の利用者操作へ直接影響する。`8e63c3f` と `9c43701` で状態依存の説明・操作ラベル・通知を揃えた |

反例固定: 修正前の `KeyboardViewTest.accessibility exposes individual keys and activates focused key` は通常qキーについて説明に「タップ q」を含み、ACTION_CLICKで `CommitText("q")` を送った (`BUILD SUCCESSFUL in 3s`)。修正後の同じ `KeyboardViewTest` class は `BUILD SUCCESSFUL in 11s` で、通常qキーの結果を保持したまま modifier の未選択／選択中説明とaction labelを確認した。

再review: `9c43701` に対する元finding限定の再確認で No Critical/Major。`cancelActiveGestures()` からも `updateModifier(null)` を通し、選択→cancel→通常の説明・非選択・Tab action labelへ戻るテストが `BUILD SUCCESSFUL in 10s`。

全スイートの2回目はSHA `8e63c3f` で `fast-checks` と `android connected (real Mozc)` がPASS、`android unit, lint, apk` がFAIL。`ImeHideBarTest.emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway` が `ImeHideBarTest.kt:572` の `requireNotNull(header)` で `IllegalArgumentException`。現行版の単独実行1回目もFAIL、base ref `919d73b` を `git archive` で展開して同じ単独Gradle stepを実行した結果は `BUILD SUCCESSFUL in 9s`。その後、現行版の同じ単独テスト再実行は `BUILD SUCCESSFUL in 3s`。このテストは retry-pass 1件として最終報告に残す。今回の差分に絵文字実装の変更はないが、base成功だけをもって失敗を免責しない。最終SHAの全スイートを再実行する。

最終実装 SHA `9c4370113e68197f507141f2d30f17cc3f95a4e1` で `scripts/test-all.sh --parallel --connected` を再実行した。スイート自身の要約:

```
========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk
  PASS: android connected (real Mozc)

Passed: 3 / 3
```

fast-checks `5 checks passed (2140 ms)`、Android unit/lint/apk `BUILD SUCCESSFUL in 24s`、connected `Finished 31 tests on Medium_Phone_API_36.1(AVD) - 16` / `BUILD SUCCESSFUL in 46s`。最終実行では0 failed / 0 skipped。初回失敗の `ImeHideBarTest` は再実行でのみ通過した1件として保持する。

Surface Observer は最終 SHA `9c43701` の APK を Android 16・412dp 相当エミュレータへ再インストールして観察。QWERTY／記号の双方で C/T/A 表示、中央 T タップで通常入力欄から数値欄へフォーカス移動、上フリックで青い A、下フリックで青い C、Alt 選択中の中央タップで解除して Tab を誤送信しないことを確認。表示崩れ・重なりなし。証拠は `tmp/surface-final-qwerty.png`、`tmp/surface-symbols.png`、`tmp/surface-final-alt.png`、`tmp/surface-final-ctrl.png`。物理端末・840dpの実画面・TalkBack実聴は未観察（840dpタッチ経路とアクセシビリティ情報は接続試験で確認）。検証後にエミュレータを停止。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

`technical-reference.md` の QWERTY/Symbols 設計決定19を、C/A中央T/Tab・上下の次1キー修飾・保留中中央解除の現行動作へ更新。ほかの設計決定は変更していない。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

実装・レビュー・AC裏取り・Surface Observer を最終 SHA `9c43701` で完了。採用した Major 1件は修正済み、未対応 finding は0件。ユーザへ提示する差分・テスト・確認手順は `close-gate.md` にまとめる。承認後に ticket close と通常のAPK・サイト公開へ進む。明示承認前は close しない。

close gate 独立確認で、復旧元、外部キー操作契約、保存データ変更なしの根拠が不足と指摘された。固定復旧元 `919d73b8cc661a8f89a9a9298766664f19531138` と現在の archive を一時領域へ展開し、差分の逆適用後に対象11ファイルの byte 一致を確認。board に公開後の戻し方、入力先アプリの範囲、後方互換性、エラー契約と生成物、差分列挙の根拠を補った。独立再review は pass。

ユーザのAPK URL依頼を受け、最終実装SHA `9c43701` で作ったdebug APKを既存の24時間Flyer確認ページ `https://dteqzpbq.aboutme.style/` に掲載。ファイルは `app/build/outputs/apk/debug/app-debug.apk`、38,920,031 bytes、SHA-256 `ab83bfe6d686ac4ce5a31208c1d2fd47a14998a8fd762aa4ab81821e64906eb3`、package `com.masuidrive.gestureime`、version `0.15.32` / code 48。Flyer側の新しいpart1/2のhashはそれぞれ `e1149e3528848f30a7308a10b69e4fb7916324f65217e31476f4e2f090485b48` / `fe829ed2ae1c66519e35ce9b7caa3e462ec27edcc40c016aaa064095cabef07f` でローカルと一致し、結合hashも元APKと一致。旧レビューAPKのpart1/2は削除。サイト期限は `2026-10-28T07:24:47.941Z`（JST 16:24）。Flyer確認配布は正式リリースではなく、ticketクローズ承認は未取得。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
