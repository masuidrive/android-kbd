# Work Notes: 260911-053738-support-native-light-mode

## Status: PDH-implement (In progress)

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

- `[PDH-open] -> [PDH-ticket-review]` — ユーザが native Light Mode 未対応なら次に実装するよう明示した。
- Why は Product Brief の Done にあるテーマ検証へ接続する。
- AC は端末テーマ切替時の表示と入力非退行として利用者が観察できる。
- 独自設定を増やさず Android `uiMode` へ追従するため、未確定の製品判断はない。
- `[PDH-ticket-review] -> [PDH-ticket-human-review]` — ユーザの明示指示を Light Mode 対応の承認として記録した。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた

- 現状測定: Theme は `Theme.AppCompat.DayNight.NoActionBar` だが、`KeyboardView`、`CandidateStripView`、`KeyboardPopupController` が Dark 色を直接指定している。
- 正本測定: `docs/reference/mock-source.html` の `light-dark(...)` に Light/Dark の対になる配色が定義されている。
- 実装後測定: unit/connected test と Light/Dark 双方のスクリーンショットで AC 1〜3を確認する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `[PDH-ticket-human-review] -> [PDH-implement]` — ユーザ承認済みACとRequired Probesを確認した。
- 実装前の仮定: Androidの`values`/`values-night`はIMEサービスとActivityの双方で端末`uiMode`に従う。Robolectricのqualifier切替で色選択を測り、実機相当AVDでもLight/Darkを確認する。
- 実装前の仮定: テーマ変更時にIME input viewが再生成されない場合がある。Custom Viewの`onConfigurationChanged`で既存Viewを再描画し、候補の子Viewも再生成する。
- 反例の基準: Darkテーマ時の既存色（背景`#29292c`、通常キー`#414144`、特殊キー`#303034`、選択キー`#a8ceff`）を変更前出力として固定する。
- `values`/`values-night`へHTML正本のLight配色と既存Dark配色を定義し、KeyboardView、CandidateStripView、KeyboardPopupRenderViewの描画をresource参照へ変更した。
- `onConfigurationChanged`でKeyboardViewと表示中popupをinvalidateし、CandidateStripViewは子Viewを再renderする。
- 重複検出 skip: `similarity-generic`が環境にinstallされていないため。色resource参照は既存3描画classへ直接適用し、新規utilityは追加していない。
- Targeted tests: `./gradlew testDebugUnitTest --tests '...KeyboardThemeTest' --tests '...KeyboardPopupControllerTest' --tests '...CandidateStripViewTest'` → BUILD SUCCESSFUL。
- Full test 1回目: 既存KeyboardViewTest 3件がDark固定色を期待してfail。Light既定値へ期待を更新し、Dark配色はKeyboardThemeTestで6色を固定した。
- Full test 2回目: `scripts/test-all.sh` → fast-check 5件、unit 125件、lint、debug APK buildすべて成功。
- AVD: API 36.1でLight/Darkへ切替後、Gesture IMEを再表示。`docs/verification/light-mode-keyboard.png`と`dark-mode-keyboard.png`に実画面を保存した。
- Connected tests: `./gradlew connectedDebugAndroidTest` → Popup_API_36_1で7件成功。

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
