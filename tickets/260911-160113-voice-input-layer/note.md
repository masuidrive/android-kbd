# Work Notes: 260911-160113-voice-input-layer

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: Android端末内SpeechRecognizerだけを使い外部provider pathがない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘修正前後とも通常QWERTYの入力・候補欄・4行高を維持した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み。AVDでQWERTYから左フリックし、同一候補面の非対応表示、固定4行高、左下キャンセルを確認
- [x] PDH-verify: ドキュメント更新の要否を確認済み。technical-referenceとsite/manual.htmlを更新
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
2026-09-12: ユーザが固定4行、状態見出しなし、左下キャンセル、通常変換候補と同じUIを明示した。既存の端末内完結方針と一致し、追加判断待ちはない。

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
- [x] AVDで通常QWERTYと音声面の上端・下端が一致し、音声面は4行高、左下キャンセル、候補欄の「非対応」が通常候補faceと同一であることを確認。実発話partialはAVDに日本語モデルがないため実機確認待ち。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 2026-09-12: 既存の押下保持経路から、左フリック成立時に4行高のVOICE面へ遷移する経路へ変更。VOICE面左下は取消、上/右/下は日本語/QWERTY/テンキー切替とした。
- 2026-09-12: SpeechRecognizerの最終候補を重複除去した可変長リストで保持し、通常CandidateStripへ同じfaceで表示。描画snapshot tokenを経由した候補tapだけが選択候補を1回確定し、直前レイヤーへ戻る。
- 2026-09-12: 認識開始/認識中は余分な状態見出しや旧右端取消を表示せず、途中結果だけを候補面へ表示する。permission/unavailableの既存案内は維持。
- 2026-09-12: 関連Robolectric 86件PASS、`scripts/test-all.sh --parallel` fast-checks / android unit・lint・apk PASS。重複検出は`similarity-generic`未導入のためskip。
- 実装commit: `3192e59`。ライフサイクルと途中候補の操作性修正commit: `c0d2603`。

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
| 1 | IME lifecycle | Major | hide/show後にVOICE modeが残る | 修正 | 終了経路でsession取消・候補消去・復帰mode反映を統一し、回帰testを追加 |
| 2 | partial候補 | Major | 途中結果が選択可能に見え、error後に残る | 修正 | partialをdisabled/non-focusableにし、terminal/error/idleで消去。finalだけ選択可能にした |
| 3 | 再review | - | Critical/Majorなし | 採用findingなし | `c0d2603`と追加testを独立review済み |

## Technical reference 更新
決定10/17/20へ、VOICE面の復帰操作、固定4行高、partial/finalの共通候補UI、stale結果破棄を反映した。

<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
APKを入力方法として選び、通常欄で左下レイヤーキーを左へフリックする。音声面の高さ、左下キャンセル、候補欄の途中・最終結果、候補tap確定、上下右のlayer切替を確認する。

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
