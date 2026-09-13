# Work Notes: 260913-112709-select-exact-tapped-conversion-candidate

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内Mozcだけの変更で外部providerを使用しない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み - CandidateStripの表示token/index testと実Mozcのexact candidate境界testを突合した
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
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
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

- 実Mozcへreading「にほんね」を渡し、候補の表示index・candidate ID・確定文字列を記録する。
- 候補View単体ではrender時のindexを正しく通知する既存testがあるため、Mozcのselection/submit境界を優先して調べる。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `0a63529`: 変換候補を`SUBMIT_CANDIDATE`でcandidate IDどおりに確定し、複数文節でpreeditが残る場合だけ後続を`SUBMIT`する。実Mozc再現testを追加した。
- `scripts/test-all.sh --parallel --connected`: fast-checks、全unit/lint/APK、実Mozc connectedの3/3 PASS。

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
| 1 | candidate identity | Minor | CandidateStripからImeServiceまでの一体instrumentation testがない | 非阻害 | Viewのtoken/index単体testと実Mozcの「2本ね」ID確定testで報告された境界の両側を固定済み |
| 2 | 複数文節 | Minor | 後方候補を選ぶ複数文節のexact regressionがない | 非阻害 | 長い複数文節、単一文節非重複、学習・履歴削除を含む実Mozc全9 testが成功し、今回の単一文節誤選択を直接固定済み |

- 独立reviewはCritical/Major 0。表示indexとMozc candidate IDの対応、および`SUBMIT_CANDIDATE`結果を返す実装を確認した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- Design decision 31へ`SUBMIT_CANDIDATE`によるexact candidate確定と、preeditが残る場合だけ後続を確定する契約を追記した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- 確認手順: 「にほんね」を変換し、横スクロール後の「2本ね」をタップして、入力欄へ「2本ね」そのものが確定することを確認する。
- ユーザの明示close承認まではticketを閉じない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
- ユーザの実機画像と「2本ねを選択しても日本猫が選択される」という報告を、再現・修正の承認として記録した。
- WhyはProduct Briefのタップ数削減と迷わない入力へ接続し、ACは表示候補と確定文字列の一致で観察できる。
- 高さ問題は既存ticketで扱い、本ticketは候補identityだけに限定する。
[2026/09/13 20:32 JST] API 36.1 AVDの実Mozcでreading「にほんね」を渡し、表示候補「2本ね」はindex 8・candidate ID 9だったが、`commit(8)`が「二本ね」を返す失敗を再現した。CandidateStripViewはrender時indexを正しく通知する既存testがある。`SELECT_CANDIDATE`のoutput resultがcommand前にfocusされていた別候補を返すのに、その値を確定文字列の先頭としていたことが原因。タップ候補のcandidate IDでMozcを選択した後、確定文字列はそのcandidate自身のvalueと後続segmentのSUBMIT resultから構成する。

[2026/09/13 20:35 JST] 20:32の「candidate自身のvalueを連結する」案は、SELECT後のSUBMIT resultも残りではなく別候補を返し、単一文節を二重確定するため棄却した。Mozc公式protoでは`SELECT_CANDIDATE`は候補windowを閉じる選択、`SUBMIT_CANDIDATE`はcandidate IDの候補を確定するcommandと定義される。変換でも`SUBMIT_CANDIDATE`を使い、そのoutputにpreeditが残る複数文節だけSUBMITで後続を確定する実装へ変更した。API 36.1 AVDの実Mozc全9 testで「2本ね」の完全一致、単一文節の非重複、長文節保持、学習と履歴削除を確認した。
