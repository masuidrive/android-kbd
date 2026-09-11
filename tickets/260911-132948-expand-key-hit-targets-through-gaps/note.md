# Work Notes: 260911-132948-expand-key-hit-targets-through-gaps

## Status: PDH-human-review

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: Android Custom View内の端末ローカルhit testだけを変更し、外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [-] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した - skip: 独立reviewでCritical/Majorの指摘なし
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
Product Briefの誤操作を避けて片手・両手で快適に使う目的へ接続する。ACは座標と入力結果で観察できる。描画矩形を維持し、論理セルをtapへ割り当て、既存gesture契約を維持する判断は確定している。外部依存と未確定判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた: 412dp QWERTYの見えるキー面だけでは論理セル内有効面積が概算約70%。修正後は隣接キー間の横6dp・縦10dpを中点で100%割り当てた。`KeyboardViewTest` 38件で横・縦・EMPTY・rowSpan Enter・Dual Flick・描画矩形不変を確認した。

## PDH-implement. 実装ログ
- `e046d0e9b64c6f829ac272dfdd1a41eb18e26c7d`: `HitTarget`へ描画用`bounds`と入力用`tapBounds`を分離し、ACTION_DOWNとaccessibilityを共通の`hitTargetIndexAt`へ統合した。
- 横は論理セル境界、縦はrow gapの中点までtap矩形を広げた。複数行keyは占有row末尾を使い、外周余白は広げていない。
- `KeyKind.EMPTY`は共通lookupで除外した。`GestureInterpreter`と`GestureThresholds`は変更していない。
- focused `KeyboardViewTest`: 38 tests / 0 failures。全検証 `scripts/test-all.sh --connected`: fast-checks 5、unit、lint、APK、connected実Mozc 9件が成功。
- v0.9.1 APKをAPI 36.1 AVDへ導入してQWERTYを表示し、キー形状・横6dp/縦10dp gap・ラベル配置・4行高がv0.9.0から変わっていないことをスクリーンショットで観察した。gapの左右・上下入力結果は座標指定unit testで裏取りした。

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
| 1 | 独立Terra review | — | Critical/Majorなし | 採用事項なし | 描画矩形不変、gap中点分割、EMPTY、rowSpan、Dual、gesture閾値維持を実装とtestで確認 |

## Technical reference 更新
Design decision 25へ、描画矩形とtap矩形の分離、gap中点分割、ACTION_DOWN/accessibility共通lookup、EMPTYと18dp閾値維持を追記した。READMEとv0.9.1マニュアル・製品ページも更新した。

## PDH-human-review. 人間レビュー
v0.9.1を端末へ更新し、QWERTYまたは日本語レイヤーでキー間の見える隙間の左右・上下をタップする。最寄りの文字が抜けずに入力され、通常のキー中央からのフリック方向と必要距離が以前と同じことを確認する。

ユーザのクローズ承認待ち。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
