# Work Notes: 260911-063048-unify-four-row-keyboard-heights

## Status: PDH-human-review (Verified; awaiting close approval)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: native geometryだけで外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み - native accessibility bounds/Canvasで外・内・Dualのgeometryを観測
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- ユーザが4行の高さ差を指摘し、続けて「キーの隙間」と原因箇所を明示した。
- 外/内ともkey face高は45/52dpで一致し、差はQWERTY pitch-gap=55-10/62-10、かな=51-6/58-6にある。
- 縦gapだけを10dpへ揃え、横gap6dp、face高、action/layoutを維持するため未確定判断はない。
- `[PDH-open] -> [PDH-ticket-review] -> [PDH-ticket-human-review] -> [PDH-implement]` — 直接指示を承認として開始。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

- 変更前: かな縦gapは外/内とも6dp、QWERTYは10dp。4行pitch合計で16dp差がある。
- 変更後は外55dp/内62dp pitchと10dp gapをQWERTY/SYMBOLS/KANAで共有し、かなface高45/52dpをassertする。
- 反例基準: 横gap6dp、縦長Enterが2 face+1 gap、Dual左右key非重複、tap/flick actionを維持する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `KeyboardView`のKANAをQWERTY/SYMBOLSと同じrow gap 10dp、外55dp/内62dp pitchへ統合。face高はpitch-gapで45/52dpのまま。
- 狭幅412dp/広幅840dpについてQWERTY/SYMBOLS/KANAのmeasured height、face高、次行gapが一致するtestを追加。既存Dual/Enter overlapとaction testも対象suiteで成功。
- Targeted `KeyboardViewTest` → 32件成功。
- `a4186d0`: KANAのgap/pitch実装と外/内3layout統合test。
- `f02fefe`: review追補として840dp Dual KANAの52dp face/10dp gapを直接固定。
- Full `scripts/test-all.sh` → fast-check 5件、unit 130件、lint、APK build成功。

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
| 1 | Dual geometry evidence | Minor | 新規統合testはwide非DualだけでAC2のDual 52dp/10dpを直接assertしない | 採用・修正 | 840dp Dual KANAを有効化しface高と次行gapを直接固定 |
| 2 | constrained parent | Minor | 親がintrinsic height未満をEXACTLY指定すると既存min計算でfaceが縮む | 後続ticketで調査 | 添付報告の`260911-063912-fix-intermittent-keyboard-vertical-offset`がIME親高・再計測を扱う。本ticketはレイヤー間gap分岐の統一に限定 |

- `[PDH-review-2]` Terra限定再reviewでDual evidence追補と新規Critical/Majorなしを確認。
- 壊していない側の反例: NUMBERS/CURSORは分岐外で従来pitch 57/64・gap6を保持し、全layer Enter/Paste offset testも成功。
- `[PDH-review] -> [PDH-verify] -> [PDH-human-review]` — full suite、独立review、geometry観測が成功。closeは明示承認まで保留する。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`へ3種4行layoutの共通pitch/gap、face高、Dual/Enter適用を追記した。

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
