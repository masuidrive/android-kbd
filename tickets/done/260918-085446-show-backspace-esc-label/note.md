# Work Notes: 260918-085446-show-backspace-esc-label

## Status: PDH-close (Approved by push instruction)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部providerを使わないAndroid描画と静的mockの変更
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

2026-09-18: ユーザはBackspaceの既存下フリックEsc actionを変更せず、通常表示の補助ラベルとフリック中表示を他のキーへ揃えることを明示した。表記は既存の直接キーに合わせて`Esc`とする。native custom view、browser mock、正本仕様、manualがconsumer surfaceで、未確定判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた
  - nativeのBackspaceはdown actionにEscapeと`ESC`を持つが、描画側のsecondary対象からBACKSPACEが除外されている。
  - mockは`gestureAlt`用のclassと下フリックCSSを持つが、DOM生成が`key.alt`だけを見ていたためidle補助ラベル要素がなく、`key.alt || key.gestureAlt`への修正が必要だった。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

2026-09-18: `KeyboardLayouts.backspace`のdown labelを`Esc`へ統一し、`KeyboardView`でdownを持つBackspaceをsecondary label描画と既存90ms下フリック拡大animationの対象へ加えた。未割当の上・左右では拡大を起こさず、下方向だけを対象とした。mockの既存idle/selected CSSを活かして`gestureAlt`表記を`Esc`へ揃え、正本mockをbyte同期した。新規testはユーザ指示どおり追加しない。

2026-09-18: mockのDOM生成は`gestureAlt`をclass判定に使う一方、`.key-alt`要素を`key.alt`の場合しか生成していなかったため、`key.alt || key.gestureAlt`で生成するよう修正した。実browser 412pxでidle `Esc`、aria label、下フリック時class、`matrix(1.7, 0, 0, 1.7, 0, 13)`、main opacity 0、横overflowなしを確認した。

2026-09-18: commit `8a3b0d9`。既存focused `KeyboardLayoutsTest`・`KeyboardViewTest`と`lintDebug`はBUILD SUCCESSFUL。最終SHAで`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel`を実行し、fast-checksとAndroid unit/lint/APKの2/2 PASSを確認した。

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
| 1 | native/mock gesture parity | Major | Symbols mockのBackspaceだけstrict方向判定がなく、上・左右がtap削除へfallbackする | 採用・修正 | `strictFlickDirections: true`をQWERTYと同様に指定 |
| 2 | center-return sweep | Major | `text`を持たないBackspaceの主ラベルを`key.text`で更新し、中心復帰時に`undefined`になる | 採用・修正 | `key.text ?? key.label`から主ラベルを復元 |
| 3 | idle label geometry | Minor | native idle `Esc`だけ10spで、mock・他の補助ラベルの11相当と異なる | 採用・修正 | Backspaceのsecondary sizeも11spへ統一 |

2026-09-18: 修正後の412px実browserで、Symbols Backspaceはidle `⌫`＋`Esc`、上フリック後も入力`q`を維持、下フリック中だけ`flick-selected`になり入力を維持、下から中心へ戻すと`⌫`＋`Esc`へ戻ってtap削除として`qq`から`q`になること、横overflowなしを確認した。

### Findings (PDH-review-2)

対象SHA `7ec7e7f`。前回のMajor 2件・Minor 1件はすべて解消し、残存Critical 0 / Major 0 / Minor 0。Symbols/QWERTYのstrict方向、中心復帰`⌫`、native idle `Esc` 11sp、mock正本byte一致を独立確認した。

## PDH-verify. 検証結果

2026-09-18: 最終SHA `7ec7e7f`で`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel`を再実行し、fast-checksとAndroid unit/lint/APKの2/2 PASSを確認した。新規testはユーザ指示どおり追加していない。

2026-09-18: nativeはQWERTY・Symbolsのdown付きBackspaceだけがidle secondaryと下方向animationを使い、`KeyAction.Escape`を維持することをコードと既存suiteで確認した。browser mockは412px実操作でidle、下選択、上no-op、中心復帰、tap削除、横収まりを観察した。実機スクリーンショットの追加検証は、明白ならtest不要というユーザ指示に従い行っていない。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

2026-09-18: `technical-reference.md`のQWERTY＋Symbols節へBackspaceのidle `Esc`補助ラベルと下フリック中央拡大を追記し、`docs/reference/sites-native-spec.txt`、native、mock、manualを同じ表記へ揃えた。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

2026-09-18: 実装内容、既存suite 2/2 PASS、独立再review 0 findingsを提示後、ユーザが「push」と明示したため、本ticketのclose、main統合、GitHub pushを承認したと判断した。APK・製品サイトの再公開は今回の指示に含めない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
