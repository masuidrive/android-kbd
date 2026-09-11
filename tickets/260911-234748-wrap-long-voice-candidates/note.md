# Work Notes: 260911-234748-wrap-long-voice-candidates

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内SpeechRecognizerと静的ブラウザmockだけの変更で外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [-] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した - skip: 独立reviewで修正対象のCritical/Majorがなかった
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
2026-09-12: ユーザが音声候補の横幅超過時の改行を明示した。固定IME高と通常候補非退行をACに含め、追加判断待ちはない。
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

2026-09-12: Mobile 390pxでは長文候補幅378px <= 候補欄384px、2行、keyboard高270px、document横overflow 0。Tablet 840px/Darkでは長文候補幅814px <= 候補欄820px、文字行y=60/75の2行、keyboard高298px、document横overflow 0。通常時と音声時で各modeのkeyboard高は同一だった。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

2026-09-12:
- 実装commit: `c1ba5c2`。
- 仮定確認: 候補欄は固定50dp、上下paddingを除くfaceは34dpだった。音声だけ13sp・15dp line-height相当、font paddingなし、最大2行とし、通常候補の15sp/1行を変更しない。
- `CandidatePresentation`で音声表示を型として伝達し、途中結果と最終候補だけ可視幅以下・最大2行・末尾省略にした。選択可否は既存の`selectable`を維持する。
- ブラウザmockも`.voice-candidate`だけ同じ固定34px face内で2行clampし、通常候補CSSを維持した。
- 重複検出 skip: `similarity-generic` が環境に未導入。
- focused Robolectric PASS。Chrome実DOMでTabletの音声候補face高34px/white-space normal、Mobile phone 412pxでscrollWidth=clientWidth=412を確認。

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
| 1 | Android / mock | - | Critical/Majorなし | 解消済み | VOICE限定presentation、幅再計測、固定高、通常候補非退行、tap/a11y経路を独立reviewで確認 |

2026-09-12: base branch `origin/features/260911-055701-configure-slash-command-candidates` がHEADの祖先であることを確認。独立reviewはCritical/Majorなし。

## PDH-verify. 検証結果

- AC 1 VERIFIED: partial/finalはともに`CandidatePresentation.VOICE`を通り、最大2行・末尾省略になる。390px/840pxの実DOMで可視幅内の2行を観察した。
- AC 2 VERIFIED: defaultの`SINGLE_LINE`は1行・省略なしを維持し、focused regressionと全suiteがPASSした。
- AC 3 VERIFIED: 候補欄50dpとface34dpは不変。Mobile/Tabletそれぞれ通常時と音声時のkeyboard高が一致した。
- AC 4 VERIFIED: mockは`.voice-candidate`だけ2行clamp。390px Lightと840px Dark/Tabletでdocument横overflow 0を観察した。
- Surface Observer: Mobile/Tabletで長文候補を2行で読め、候補表示によるkeyboardの移動や視覚的なjumpはなかった。3行目以降の省略はticketの確定判断どおり。
- Documentation: `technical-reference.md`の決定20を更新。PDH配布物自体の更新は不要。

最終suite:

```text
$ scripts/test-all.sh --parallel
Parallel mode: logs in /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.l7CQsa6sd8
  Starting: fast-checks (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.l7CQsa6sd8/fast-checks.log)
  Starting: android unit, lint, apk (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.l7CQsa6sd8/android_unit,_lint,_apk.log)

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

決定20へ音声候補だけの最大2行・末尾省略と、通常候補の1行維持を追記した。

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
