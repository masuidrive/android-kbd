# Work Notes: 260912-020555-add-mozc-next-word-prediction

## Status: PDH-ticket-human-review (User implementation instruction received)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み - skip: Mozcと辞書はAPK同梱であり、外部provider/APIを使用しない。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

 - Product Briefの「フリックで素早く入力」「Mozcを小さなJNI境界の後ろへ接続」「offline」に接続する。
 - AC読み手は5件すべてから、確定後候補の表示・タップ確定・更新/clear・private抑止・既存UI維持を復元できた。
 - consumer surface: `ConversionEngine`、Mozc JNI、`TextInputController`のsurrounding text、`ImeService`候補state、native候補欄、unit/instrumentation、manual/TR。
 - ユーザから「予測変換実装して」と明示指示済み。設定toggleを増やさず初期有効にする判断は、直前の「ターミナル対応以外はデフォルトオン」を反映した。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）
  - 公式`commands.proto`で`REQUEST_NWP`がsurrounding textからNext Word Predictionを要求し、converter history segmentをresetするcommandであることを確認した。
  - 同protoで`Context.preceding_text` / `following_text`、Android向け`zero_query_suggestion` / `mixed_conversion`を確認した。
  - 同梱`mozc-proto-lite.jar`を`javap`し、`Input.Builder.setContext`とContext両text setterが実際に利用可能と確認した。
  - bundled `mozc.data`がどの文脈で非空候補を返すかは実装後にinstrumentationで実測し、代表的な日本語文脈で1件も返らなければ完了扱いにせず停止する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 2026-09-12: 実装前の仮定として、`REQUEST_NWP` の候補 ID は `SUBMIT_CANDIDATE` で個別確定でき、結果文字列は `Output.result.value` に返ることを、実装後の実機 instrumentation で確認する。有限文脈は UTF-16 の中間で切らず、カーソル前後各 128 code point とする。private / `NO_PERSONALIZED_LEARNING` では controller 自身が surrounding text を取得しない。`find` では similarity-generic に該当する実行物を検出できなかったため skip とする。
- 2026-09-12: `ConversionEngine.predict`、`TextInputController.predictionContext`、`ImeService`の`PREDICTION` sourceを追加した。通常の日本語確定後だけ周辺文脈を渡し、`SUBMIT_CANDIDATE`で候補を追記して再照会する。候補token、editor token、prediction generationを照合し、自己確定のselection callbackはcomposing置換前後の文字数差で吸収する。かな入力・selection・editor変更・候補なしでは候補を消す。private/学習禁止欄はcontrollerで読み取り前に拒否する。
- 2026-09-12: `4a3d208 [260912-020555-add-mozc-next-word-prediction] feat(mozc): add local next-word predictions`。focused unit tests、`connectedDebugAndroidTest`（11 tests）、`scripts/test-all.sh --parallel`がPASSした。
- 2026-09-12: independent reviewのMajor 1/2を`ef2599b [260912-020555-add-mozc-next-word-prediction] fix(prediction): reject stale selection results`で修正した。`にほんご→日本語`の`4→3`、ひらがな/カタカナの同長`4→4`、予測追記`3→4`の自己selectionを許可し、遅延NWP中の外部selectionはgenerationを進めて結果を破棄する。focused unit testsと`scripts/test-all.sh --parallel`がPASSした。

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
| 1 | selection callback | Major | composing置換を確定文字数だけで照合し、正しい予測を消す | 修正済み | `ExpectedSelectionTransition`で置換前後の差を照合し、4→3/4→4/3→4と照会前後の順序を回帰testした。 |
| 2 | async prediction | Major | NWP待機中のselection変更で古い結果が再表示される | 修正済み | `predictionRequestInFlight`中も予期しないselectionでgenerationを無効化し、遅延fakeで再表示しないことを確認した。 |
| 3 | technical reference | Major | 予測候補の長押し削除可否が実装と矛盾 | 修正済み | Mozc予測候補の履歴削除とstate再表示、Android個人辞書候補の除外を明記した。 |

### Findings (PDH-review-2)

- 対象commit `d27b2dddd69b46fb45bc6b957c494a35819debe4`。Critical/Majorなし。3件のMajorが解消し、focused unit 62件とdiff checkがPASSした。
- 修正前はcomposing置換量と確定文字数の不一致、およびNWP待機中selectionで予測が誤消去・再表示し得た。修正後は`にほんご→日本語`、同長かな/カタカナ、予測追記、遅延NWP中の外部selectionを個別に通し、壊していないprivate欄の周辺文字列非取得も継続してPASSした。

## PDH-verify. AC裏取り

- AC 1〜5を独立Verifierが`d27b2dd`でVERIFIED。focused JVM test 62/62、API 36.1 arm64 AVDのconnected test 11/11がPASSした。
- 同梱`mozc.data`で`REQUEST_NWP`、`SUBMIT_CANDIDATE`、予測履歴削除を確認した。パスワード/学習禁止欄は周辺文字列APIの呼出回数0を確認した。
- 物理Foldは未接続。system IMEとしての手動tap journeyは、既知の初回高さ問題でAVDの4行が画面外へ溢れたため未完了とし、後続の固定高さticketで再確認する。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`に、有限context、`REQUEST_NWP` / `SUBMIT_CANDIDATE`、token・generationによる破棄、private欄の非取得、予測候補の履歴削除を追記した。
- review指摘を受け、自己commitのselection transitionと待機中のselection invalidationを追記し、予測候補の長押し削除を誤って「対象外」としていた記述を訂正した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->
- 実装・競合修正・独立review・AC検証までは完了。後続の設定safe areaと固定高さを直したAPKで、利用者に確定後予測の手動確認を依頼する。
## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- `REQUEST_NWP` は`Input.request_suggestion=true`なしでは候補を返さない。公式`SessionTest.RequestNWP`の契約を確認して設定した。
- API 36.1 arm64 AVDで同梱`mozc.data`へ`preceding_text="あけまして"`を渡すと非空候補が返り、`SUBMIT_CANDIDATE`は非空結果を返した。予測候補に対する`DELETE_CANDIDATE_FROM_HISTORY`もconsumedされた。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
