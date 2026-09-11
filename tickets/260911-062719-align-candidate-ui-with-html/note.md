# Work Notes: 260911-062719-align-candidate-ui-with-html

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内Viewと静的mockだけで外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み - native View geometry testと実ブラウザのmock 1文字prefix/CSS/tapを観測
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- `[PDH-open] -> [PDH-ticket-review]` — ユーザが日本語/英字候補UIのHTML不一致を指摘し、続けて`demo.html`のmock更新を明示した。
- Product Briefの日本語変換・英字候補とHTML fidelityへ接続し、AC 1〜5はnative Canvas/View寸法とbrowser DOMで観察できる。
- 候補生成や順位は変えず、共通face描画とmockの英字候補表示だけをscopeにする。
- `[PDH-ticket-review] -> [PDH-ticket-human-review] -> [PDH-implement]` — 会話内の直接指示をticket承認として記録した。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

- HTML正本: candidate buttonはmin-width 82px、height 34px、padding 0 14px、gap 5px、radius 7px、shadow 0 1px、font 15px。barはheight 42px、padding 0 3px 8px。
- native現状: font 15dpと左右18dp paddingだけで、固定height/min-width/gap/radius/shadowなし。Dark未選択`#29292c`・選択`#61d2ff`も正本のkey/selected色と異なる。
- mock現状: CSS寸法は正本相当だが日本語候補しか描画せず、英字補完候補を確認できない。
- 仮定: native strip全高50dpはHTML keyboard top 8px + candidate bar 42pxに相当するため維持し、内部paddingを左右3/上8/下8dpとしてbutton 34dpを確保する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 反例基準: 候補tap token、選択候補へのscroll、Light/Dark runtime refresh、private欄の候補抑止を維持する。
- `c9d5963`: CandidateStripViewを左右3/上8/下8dp、candidate faceを34dp/min82dp/pad14/gap5/R7/shadow1へ変更。Dark通常/選択色をHTML key/selected paletteへ一致させ、token/scroll経路を維持。
- `b7a4ae8`: `site/mock.html`へ英字候補toggle、prefix候補、tap置換を追加。日本語候補と同じcandidate buttonを使用。
- `4dc04a6`: review修正として1文字prefix、native同等case保持、fontScale 2.0固定サイズ、音声取消face geometry testを追加。
- Full: `scripts/test-all.sh` → fast-check 5件、unit 129件、lint、APK build成功。
- Browser: `c`で`compact/clear/candidate`を表示し、先頭tapで`compact`へ置換。`C`では`COMPACT/CLEAR/CANDIDATE`となりcaseを保持。computed CSSは34px/min82px/pad14px/gap5px/R7/shadow1px/font15px。

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
| 1 | mock prefix | Major | mockは2文字から、nativeは1文字から候補を出す | 採用・修正 | 1文字から候補化し、browserで`c`を確認 |
| 2 | text unit | Major | ACは15spだが実装はHTML 15px相当の15dp固定 | 判断を明確化 | HTML fidelityと既存key fit方針に従いACを15dp相当へ明文化しfontScale 2.0を固定 |
| 3 | case preservation | Minor | mockの大文字prefixがlowercase候補になる | 採用・修正 | nativeと同じ全大文字/typed prefix保持を追加 |
| 4 | voice geometry evidence | Minor | 共通faceを使う音声取消の寸法testがない | 採用・修正 | Recording時取消の82×34dp/R7をassert |

- `[PDH-review-2]` Terra限定再reviewでMajor 2件・Minor 2件の解消と新規Critical/Majorなしを確認。
- 壊していない側の反例: Dark runtime切替後も描画済みcandidate token 26をtapすると同じeventが1回届き、face色だけHTML Darkへ変わるtestを維持。
- `[PDH-review] -> [PDH-verify] -> [PDH-human-review]` — full suite、独立review、native geometry、browser mock観察が成功。closeは明示承認まで保留する。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`へcandidate face寸法、固定文字サイズ、strip余白、Light/Dark palette契約を追記した。

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
