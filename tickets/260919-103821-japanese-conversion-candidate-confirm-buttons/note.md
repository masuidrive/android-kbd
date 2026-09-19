# Work Notes: 260919-103821-japanese-conversion-candidate-confirm-buttons

## Status: PDH-implement

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内IMEで外部provider経路はない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

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
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた
  - 現状は変換中のSpaceが候補巡回を行うがラベルは`Space`、Enterは変換previewの有無にかかわらず`無変換`である。
  - 調査前はnativeのSpace選択状態を`conversionPreview`と見込んだが、実コードではSpace経路から呼ばれていなかった。nativeはMozc `ConversionState.selectedIndex >= 0`、mockは`composition.converted`で識別する。候補欄タップは即時確定するため変更対象外とする。
  - tapと上・左・右・下、中心復帰、候補巡回、確定結果、mock正本一致を測る。公開後はAPK byte一致、metadata、権限、Pages、412px・840px表示を測る。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 実装前提: Mozcの`start`/`update`は利用者が候補を選ぶ前でも有効な`selectedIndex`を返しうるため、その値だけを選択済み判定に使わない。Space経路の`nextCandidate`が有効indexを返したときだけ、service内の明示状態を選択済みにする。
- 実装前提: 読みの追加・削除は`updateReading`を通り、候補選択状態を解除してから新しい変換状態を適用する。確定、レイヤー切替、入力欄切替は既存の`clearCandidateState`へ集約されている。
- nativeは変換中Spaceを`候補`/`CycleCandidate`へ変更し、Space選択前のEnterを`無変換`、選択後を`確定`/`CommitConversion`へ切り替えた。上・左のカタカナ変換と候補欄tap即時確定は維持した。
- 操作mock、保存正本、native仕様、technical reference、manualを同じ状態遷移へ更新し、`site/mock.html`と`docs/reference/mock-source.html`のbyte一致を確認した。
- 重複検出: `similarity-ts -t 0.7 -e html site/mock.html docs/reference/mock-source.html`は重複関数なし。Kotlin用`similarity-generic`は環境に未導入のためskipした。
- focused検証: `./gradlew testDebugUnitTest --tests 'com.masuidrive.gestureime.keyboard.KeyboardLayoutsTest' --tests 'com.masuidrive.gestureime.ImeServiceEnglishSuggestionTest' --tests 'com.masuidrive.gestureime.keyboard.KeyboardViewTest' compileDebugAndroidTestKotlin --no-daemon`はBUILD SUCCESSFUL。接続端末は0台のためinstrumentation実行は未実施だが、412dp/840dpのattached production `KeyboardView` MotionEvent testはcompile済み。
- 実装commit: `14305a0` `[260919-103821-japanese-conversion-candidate-confirm-buttons] feat(ime): 候補選択後の確定キーを表示する`。
- local browser mock: 412pxで「あ」入力後にSpace=`候補`、Enter=`無変換`、Space tap後に先頭候補がselectedとなりEnter=`確定`、Enter tap後に候補欄が消えることを終端操作で確認した。412pxと840pxは`scrollWidth == innerWidth`、840pxはDual Flickの「あ」キー2組を確認した。
- 論理commitは実装`14305a0`、review修正`e37a654`、公開候補`05d2de7`に分割した。`scripts/test-all.sh --parallel`はfast-checksとAndroid unit/lint/APKの2/2 PASS。
- API 36.1 emulatorで今回追加したattached production `KeyboardView` MotionEvent testを単独実行し、412dp/840dpのSpace候補巡回、Enter無変換/確定、上下左右、中心復帰、cancelをPASSした。全connectedは22件中21件PASSで、今回未変更の絵文字rail幅について840dp・density 420で期待253px/実測254pxとなる既存test 1件のみ失敗したため、本ticketの変更範囲外として記録した。
- APKは38,651,216 bytes、SHA-256 `c9669fb7fac87df5e2ed20c28d88bd3acab2d830de9c9e0541cb2e2bce785bc5`。versionCode 35、versionName 0.15.19、arm64-v8a、RECORD_AUDIOあり、INTERNETなし、zipalign成功を確認した。

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
| 1 | state reset | Major | Space選択後に履歴候補を長押し削除するとEnterが確定表示のまま残る | 採用・解消 | `e37a654`で履歴削除時に明示選択状態を解除し、回帰testを追加。再reviewはCritical/Majorなし |

- 壊していない側の入力: 修正を一時的に外した状態では履歴削除後のEnterラベルtestが`ImeServiceEnglishSuggestionTest.kt:596`で失敗し、修正復元後は同testがBUILD SUCCESSFULとなった。
- 独立reviewは最終差分についてCritical/Majorなし。connected全体で見つかった絵文字railの1px差は本ticket以前からある別レイヤーの既存挙動で、本ticketのout-of-scopeとして非採用に分類した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`、`docs/reference/sites-native-spec.txt`、`docs/reference/mock-source.html`を、候補未選択時のEnter=`無変換`、Space=`候補`、Space選択後のEnter=`確定`という同一契約へ更新し突合した。
- `site/manual.html`と`site/mock.html`も同じ状態遷移へ更新した。PDH配布物の変更はなく、pdh-updateは不要。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->
- ユーザの「変換時はスペースは候補ボタンにして、選んだらenterは確定ボタン」「公開までして」を実装、v0.15.19公開、ticket closeの承認として記録する。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
