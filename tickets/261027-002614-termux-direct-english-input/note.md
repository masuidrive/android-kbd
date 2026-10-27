# Work Notes: 261027-002614-termux-direct-english-input

## Status: PDH-ticket-review (2026-10-27 09:27 JST; ticket contract review)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [ ] Termux など指定したアプリの端末入力欄で、英字・かなの未確定文字を候補欄で確認できるようにする。
- [ ] 設定画面でプレビュー対象アプリを追加・解除でき、Termux を初期対象にする。
- [ ] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [ ] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [ ] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
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
PDH-open -> PDH-ticket-review — Why・AC・影響レイヤーを ticket に記入した。

- Product Brief の Who は「ターミナル」を明示しており、入力状態を確認しながら打つ価値に接続する。
- AC 1〜4 は Termux の端末画面または通常テキスト欄で、利用者が入力して観察できる結果を記した。
- Termux `TerminalView` の現行 `InputConnection` は `TYPE_NULL` または文字単位入力で `TYPE_TEXT_FLAG_NO_SUGGESTIONS` を指定し、`setComposingText` の途中状態を端末画面には描かない。`finishComposingText` / `commitText` で端末へ送る。上流の composing preview 提案は 2026-10-27 時点で Open: https://github.com/termux/termux-app/blob/master/terminal-view/src/main/java/com/termux/view/TerminalView.java および https://github.com/termux/termux-app/pull/5242 。
- 現在の md-kbd は `ImeService.shouldBufferEnglish()` と `TextInputController.appendComposing()` が英数字を buffer に保持し、かなも `setComposingText()` で送る。Termux 特性を判定する分岐はない。
- 本 ticket の consumer surface は実 Android IME、Termux の端末画面、候補エリア、通常テキスト欄、公開 mock、設定説明。
- 未確認: 利用者の実機 Termux のバージョンと設定。実装後に端末実画面で英字・かなの視認性と重複を確認する。
- 独立 AC 読み手は What 冒頭と AC 1〜4 のみから、Termux での英字即時表示、かな読みのキーボード上の表示・変換、重複のない確定、通常欄の維持を復元できた。What の「確定前の入力状態」と AC 1 の直接確定の関係は表現上やや曖昧との指摘を記録した。
- 2026-10-27: ユーザが「そういうアプリのときだけ、候補欄に入力した内容」「設定画面でアプリ指定」を提案。英字を直接確定する旧案を撤回し、英語候補・日本語変換を維持したまま、指定アプリの端末型入力欄に限って候補欄へ未確定文字を表示する AC に書き直した。上記の初回 AC 読み手結果は旧案の記録であり、改訂 AC の承認・検証には使わない。
- 改訂 AC の独立読み手は AC 1〜4 から対象アプリ選択、英字・かなの未確定プレビュー、候補操作と一度だけの確定、通常欄・機密欄の扱いを復元できた。AC 3 の「入力した文字」が候補か原文か曖昧だったため、英語候補・原文、日本語選択結果を明記して修正した。
- Android の `EditorInfo.packageName` は編集元アプリ名、`inputType` は欄の種別を公開する。`TYPE_TEXT_FLAG_NO_SUGGESTIONS` は辞書候補不要を意味するだけで表示不可を意味しないため、単独の表示不可判定には使わない: https://developer.android.com/reference/android/view/inputmethod/EditorInfo および https://developer.android.com/reference/android/text/InputType 。
- ブラウザ標準の `input` / `textarea` / `contenteditable` は未確定文字を編集欄に描画し、Web ページへ composition と input イベントを通知する。未確定中の DOM も更新されうるため、ページが確定前の文字を読む可能性はある。これは候補欄プレビューを必要とする端末型欄とは別: https://www.w3.org/TR/input-events-2/ 。
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
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた。Termux 現行コードの `TYPE_NULL` / `TYPE_TEXT_FLAG_NO_SUGGESTIONS`、`finishComposingText` / `commitText`、md-kbd の `setComposingText` 経路を上記ソースで確認した。Android の `EditorInfo.packageName` と `inputType` の役割を公式資料で確認した。実機 Termux での表示と重複は実装後の検証対象。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

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
