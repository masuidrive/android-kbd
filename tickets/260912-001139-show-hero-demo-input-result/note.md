# Work Notes: 260912-001139-show-hero-demo-input-result

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 静的ブラウザmockだけの変更で外部provider経路がない
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
2026-09-12: 既存textareaを正本としてheroでも表示する契約が明確で、操作ロジックや単独mockは対象外のため追加判断なし。
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

2026-09-12: heroの初期値、キー入力・削除・かな候補・音声候補確定、Light/Dark背景、Mobile/Tablet overflow、単独mock編集可否を実ブラウザで確認する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

2026-09-12:
- hero専用の視覚非表示指定を、112pxの編集領域と78pxの枠線なしtextareaへ置換した。単独mockの164px/124px編集領域は維持。
- heroの同一textareaで初期文「ここは入力できるよ」から、かな入力、削除、かな候補確定、音声候補確定まで反映されることをChromeで確認。
- Tablet 840pxとMobile 412pxでphoneの`scrollWidth == clientWidth`。Light背景`rgb(243,244,247)`、Dark背景`rgb(28,28,30)`、border `0px none`、console error 0を確認。
- 重複検出 skip: hero用CSS 2宣言だけの変更で構造変更なし。

2026-09-12 follow-up:
- hero初期化に残っていた`tabIndex=-1`と`aria-hidden=true`を削除し、表示されたtextareaを通常の編集欄としてaccessibility treeとTab順へ戻した。
- Chrome DOM snapshotに`textbox "入力を試す"`が現れ、Lightボタン後のTab移動でtextareaがactive、`tabIndex=0`、`aria-hidden`なしを確認。直接「直接編集」へ置換後、mockの「あ」キーで「直接編集あ」へ更新され、console error 0。

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
| 1 | Accessibility | Major | 表示したhero textareaに旧`aria-hidden`/`tabIndex=-1`が残った | 解消済み | `64a3555`で両指定を除去し、Tab・snapshot・直接編集を再確認 |

## PDH-verify. 検証結果

- AC 1 VERIFIED: heroで初期文「ここは入力できるよ」を表示し、同じtextareaへ結果が続く。
- AC 2 VERIFIED: キー入力・削除・かな候補・音声候補確定を実ブラウザで通し、同じtextareaの値が更新された。
- AC 3 VERIFIED: textareaはborder 0。Mobile 390px/LightとTablet 840px/Darkでphoneとdocumentの横overflow 0。
- AC 4 VERIFIED: 単独mockは空でfocus可能な124pxの従来textareaを維持。
- Surface Observer: hero入力欄はaccessibility treeに`textbox "入力を試す"`として現れ、`tabIndex=0`、`aria-hidden`なし。直接編集後もmockキー入力が同じ欄へ続いた。
- Documentation: Android実装決定に変更がないため`technical-reference.md`更新なし。PDH配布物の更新も不要。

最終suite:

```text
$ scripts/test-all.sh --parallel
Parallel mode: logs in /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.ftiBILnEfk
  Starting: fast-checks (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.ftiBILnEfk/fast-checks.log)
  Starting: android unit, lint, apk (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.ftiBILnEfk/android_unit,_lint,_apk.log)

========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk

Passed: 2 / 2
```

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

該当なし。ブラウザmockの既存textarea表示範囲だけの変更で、Android実装決定は変わらない。

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
