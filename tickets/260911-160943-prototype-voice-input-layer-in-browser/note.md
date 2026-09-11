# Work Notes: 260911-160943-prototype-voice-input-layer-in-browser

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: timerだけのローカルmockで外部provider pathがない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘修正前後とも通常入力欄の既存文字`x`をEscapeで保持し、候補欄42pxと横overflow 0を確認
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み。実ブラウザでleft pointer flickから候補確定とcancelを確認
- [x] PDH-verify: ドキュメント更新の要否を確認済み。site/demo.htmlの操作説明を更新
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->
ユーザがブラウザmock作成を明示承認し、途中で「上部候補欄と分けず一体」「候補選択かCancel」「通常候補欄は常時高さを確保」「状態行不要」へACを具体化した。実マイクを使わないためnativeのprivacy invariantへ影響しない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] left flick直後の認識中表示、1.5秒後の3候補、候補tap exact 1回確定、cancel入力不変、元layer復帰、候補欄42px維持、390/840pxの横overflow 0とiframe実高一致を測定。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->
- `6f3f20a`: 左フリック開始、途中結果、複数候補、送信・cancelを持つ専用面の初版と通常候補欄常設化、視覚feedback行削除。
- `fdb6d53`: 音声候補をカード内へ統合し、送信を廃止。候補tap即確定またはcancelの最終操作へ変更。
- `2f0e9c8`: Light入力面を薄いgrayへ変更し、最下段UIを削除。`masuidrive-kbd`表示とトップ内demo jumpを追加。
- `03293a6`: 削除済hide action参照を除去し、390pxのdemo jump余白を固定navより10px以上確保。
- `scripts/test-all.sh --parallel`でfast-checks、Android unit/lint/apkがPASS。

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
| 1 | 独立review | - | Critical/Major/Minorなし | 採用findingなし | 390/840px、left flick、途中・最終、候補確定、cancel、theme、Dual、overflowを確認 |
| 2 | cleanup | Minor | Escapeに削除済hide action呼び出しが残る | 修正 | `03293a6`で`cancelAll()`だけへ整理。再review findingなし |
| 3 | responsive | Minor | 390pxのanchor jumpでdemo上端がnavへ6px隠れる | 修正 | mobile scroll marginを96pxへ変更。再測定でnav下10px以上を確保 |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->
該当なし。native仕様は次ticketで判断し、本ticketはブラウザ上の操作プロトタイプだけを変更した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->
`http://127.0.0.1:4173/`の先頭mockで左下レイヤーキーを左へフリックし、途中文、カード内3候補、候補tap確定、再進入後のcancelを確認する。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
