# Work Notes: 260912-002431-stabilize-ime-height-after-app-switch

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部providerを経由しないnative layout変更
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
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- Why/AC/固定候補欄と4行高/out-of-scopeを確認。既存寸法を変えず、入力先切替時の計測だけを安定化するため未確定product判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

- [x] API 36.1 AVDで修正前相当の通常表示を観察し、見えていた約131物理pxはdensity 420の固定50dp候補欄と一致した。追加offsetは実切替では常時再現しなかった。
- [x] `KeyboardView.onMeasure`へ前入力先由来を模した500px `EXACTLY`を与えると、旧実装はintrinsic 228pxでなく500pxを採用するrace条件をコードと回帰testで固定した。
- [x] 修正版APKでSettings検索欄と内蔵入力テストを5往復し、10表示すべてkeyboard背景上端scanlineが`y=1545`で一致した。証拠は`docs/verification/260912-002431-app-switch/`。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `KeyboardView.onMeasure`が親の一時的な`EXACTLY`高を採用せず、現在幅・row pitch・padding/bottom insetから固定4行intrinsic高を決定するよう変更した。
- `ImeService.onStartInputView`でgestureを取消してintrinsic layoutを再要求し、前入力先のhit target/measure状態を持ち越さないようにした。
- 5レイヤーへ過大初回measureを与えた後も高さ228pxと先頭key boundsが通常measureと一致するRobolectric回帰testを追加した。
- 重複検出 skip: Kotlin用`similarity-generic`が環境に導入されていないため。
- `scripts/test-all.sh --parallel`: fast-check、全Android unit、lint、debug APK buildが成功した。

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
| 1 | 親の小さい高さ制約 | Major | 初版がheight MeasureSpecを全て捨て、小さい利用可能高でもintrinsic高を返す | 採用・解消 | intrinsicを上限にし、AT_MOST/EXACTLYは小さいspecSizeを尊重。全5modeの160px計測とa11y boundsを回帰化 |

### Findings (PDH-review-2)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | 高さ制約・全5レイヤー | - | Critical/Majorなし | 解消済み | `16c85fc`で過大値と小さい制約を両方処理し、a11y boundsと実機10表示を再確認 |

## PDH-verify. 検証結果

- AC 1 VERIFIED: 固定50dp候補欄と正常なkey boundsを維持し、2アプリ間10表示で余分な空白・重なりなし。
- AC 2 VERIFIED: QWERTY/KANA/NUMBERS/SYMBOLS/VOICEで過大EXACTLYをintrinsic高へ制限し、小さいAT_MOST/EXACTLYは親高へ収めた。
- AC 3 VERIFIED: Settings検索欄と内蔵入力テストを5往復した保存10frameすべてでkeyboard背景上端`y=1545`。
- Surface Observer: `docs/verification/260912-002431-app-switch/`の10往復画像と5mode画像で候補欄・4行キー・下段操作にずれなし。
- Documentation: `technical-reference.md` Decision 17を更新。PDH配布物の更新は不要。

最終suite:

```text
$ scripts/test-all.sh --parallel
Parallel mode: logs in /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.2vpxIFvGx4
  Starting: fast-checks (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.2vpxIFvGx4/fast-checks.log)
  Starting: android unit, lint, apk (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.2vpxIFvGx4/android_unit,_lint,_apk.log)

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

- Decision 17へ、一時的な過大`EXACTLY`計測を採用せず、現在幅/inset由来の4行高をlifecycle開始時に再適用する契約を追記した。

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
