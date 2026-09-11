# Work Notes: 260911-053738-support-native-light-mode

## Status: PDH-verify (Verified; awaiting publication evidence)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内描画だけの変更で外部provider経路がない
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
- [x] ユーザ追加依頼: レイヤーキー左スワイプを音声入力へ変更するticket `260911-055701-assign-layer-left-swipe-to-voice` を作成
- [x] ユーザ追加依頼: 最大6件の`/`候補を設定するticket `260911-055701-configure-slash-command-candidates` を作成
- [x] ユーザ追加依頼: QWERTY下スワイプ補助ラベルを縦中央へ動かすticket `260911-055701-center-qwerty-down-swipe-label` を作成
- [x] ユーザ追加依頼: 音声状態UIをキーと揃え、認識途中テキストを候補・確定エリアへ表示するticket `260911-060238-align-voice-status-ui-and-show-partials` を作成

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
- 論理commit: `cadd7d0` theme resource実装、`9922a61` Light/Dark描画test、`7203300` runtime refreshとreview修正。
- 最終full suite: versionCode 7 / versionName 0.7.0で`scripts/test-all.sh` → fast-check 5件、unit 127件、lint、debug APK buildすべて成功。
- 配布APK: 34,853,696 bytes、SHA-256 `e0b58e00fd4b73c49d94bdceb2d7f7fcaa9e1f2d0d5679684b31b7a229259847`。

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
| 1 | HTML candidate palette | Major | Lightの未選択候補が正本の白ではなくstrip背景色 | 採用・修正 | `candidate_background`を追加しLightは`#fff`、AC 2に従いDarkは既存`#29292c`を保持 |
| 2 | theme transition test | Minor | 表示済み候補とpopupのruntime切替テストがない | 採用・修正 | 同一Viewへqualifier変更をdispatchし、候補tokenとpopup再描画を検証 |

- 修正後対象test: CandidateStripViewTest / KeyboardPopupControllerTest → BUILD SUCCESSFUL。

### Findings (PDH-review-2)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | popup runtime route | Minor | `KeyboardView.onConfigurationChanged`からpopup controllerまでの経路を直接通す回帰testがない | deferred | 実装hookとpopup再描画testを確認。AVDではtheme変更でActivityが再生成されIMEが閉じ、再focus後に新themeで表示されるため製品blockerではない |

- Terraによる限定再reviewで追加Critical/Majorなし。
- 壊していない側の反例: Darkの通常キー`#414144`、特殊キー`#303034`、選択キー`#a8ceff`を変更前後で固定し、Dark AVD screenshotでも維持を確認した。
- `[PDH-review] -> [PDH-verify]` — 採用したMajorを解消し、残るMinorはruntime再生成を伴うtest到達範囲として分類した。

## PDH-verify. AC evidence

- AC 1: Light AVDでkeyboard、candidate strip、popup resource、設定previewのLight paletteをunit/connected testと`light-mode-keyboard.png`、`light-mode-settings-preview.png`で確認。
- AC 2: Dark AVDで既存paletteを`dark-mode-keyboard.png`とDark resource assertionで確認。
- AC 3: theme切替後にIMEを再focusしQWERTYを表示。unit 127件とconnected 7件で候補token、popup draw、既存入力動作の回帰なし。
- Surface Observer: API 36.1 AVDとローカル製品紹介/マニュアルを実ブラウザで観察。Light/Dark比較画像、v0.7導線、画像欠落0、横overflowなし。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`のnative描画契約をDark固定から`values`/`values-night`による端末`uiMode`自動追従へ更新した。

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
