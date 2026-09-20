# Work Notes: 260920-072153-adjust-number-period-and-symbol-label

## Status: PDH-verify

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部providerを使わない端末内KeySpec/UI変更
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [-] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した - skip: 独立reviewの指摘0件で修正なし
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した
- [x] テンキー`.`のtap/left/right/up/downを`.`/`,`/`=`/未割当/未割当にする
- [x] 日本語・テンキー・QWERTY・絵文字railの記号レイヤーラベルを`?}`に統一する
- [x] native・操作mock・保存mock・技術参照・正本仕様・README・manualを同じ割当と表示へ同期する
- [x] focused unit/instrumentation test、mock正本一致、similarity、diff checkを実行する
- [x] 実装ログとchecklistを更新し、論理commitを作成する

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- ユーザ指定は`.`の左右を`,`と`=`、記号レイヤー表示を`?}`へ変更することで確定している。native/mock/文書の同期と未割当上下方向の非入力をACに含め、未確定判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた
  - 現状のテンキー`.`はtapだけで左右gestureがない。記号レイヤー切替は日本語・テンキー・QWERTY・絵文字railで`#!`を共有している。tap/左/右/上/下/中心復帰/cancel、4面の表示、mock正本一致を測る。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 実装前の仮定と実測: `KeyboardLayouts`の既存`.`はcenterだけの`KeySpec`、`KeyboardView`は18dp超で選択した未割当方向を`null`としてdispatchせず、10dp以内へ戻るとcenterへ復帰し、`ACTION_CANCEL`ではdispatchしない。したがって専用`KeySpec`へleft/rightだけを追加すれば既存gesture境界を維持できる。
- 実装前の仮定と実測: 記号切替は4面とも`SwitchLayer(SYMBOLS)`を持つ`modeKey`であり、表示文字列だけが`#!`。`KeyboardView.drawMainLabel`の複合ラベル許可集合とmockの`main`/`ghost`を`?`/`}`へ同期し、actionは変更しない。
- 実装前の仮定と実測: mockのかな・テンキー`flick`は5要素の欠損値を未割当として扱い、中心復帰とcancelを共通pointer経路で処理する。テンキー`.`は`['.', ',', null, '=', null]`でnativeと同じ契約にできる。外部API、通信、保存形式の変更はない。
- nativeはテンキー`.`を専用`number-period` KeySpecへ分け、center/left/rightだけを`.`/`,`/`=`へ割り当てた。4面の記号切替はactionを維持したまま複合ラベルを`?}`へ変更し、nativeの主文字＋補助文字描画許可集合も更新した。mockは同じ5方向配列と`?`/`}`の主・補助表示へ同期した。
- focused検証: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests com.masuidrive.gestureime.keyboard.KeyboardLayoutsTest :app:compileDebugAndroidTestKotlin --no-daemon`はBUILD SUCCESSFUL。412dp/840dpのattached production `KeyboardView`へtap・左右・未割当上下・17dp未満・中心復帰・cancelを送る`MotionEvent` testと、4面の`?}`表示/tap testをcompileした。接続端末は0台のためinstrumentation実行はdeferredとし、rootの公開前connected suiteへ引き継ぐ。
- parity/静的検証: `site/mock.html`と`docs/reference/mock-source.html`はbyte一致（SHA-256 `262a3e1c4a61af71a87d276879a5bc49a50ea7af0f49684163dd5b4cf478b170`）。関連正本から旧`#!`が0件、`git diff --check` PASS、ticket.md差分なしを確認した。
- similarity gate: `similarity-ts -t 0.7 -e html`を2つのmockへ個別実行し、duplicateなし。Kotlin用`similarity-generic`は環境に存在しないためskip（利用可能な`similarity-ts`/`similarity-py`ではKotlinを解析できない）。
- 論理commit: `8520bce` `[260920-072153-adjust-number-period-and-symbol-label] feat(keyboard): テンキー句読点と記号表示を更新する`。native、mock、focused test、関連正本と利用文書を1つの利用者向け挙動変更として記録した。
- root再検証: `scripts/test-all.sh --parallel`はfast-checksとAndroid unit/lint/APKの2/2 PASS。API 36.1 emulatorで新規production `KeyboardView` MotionEvent testを実行し、412dp/840dpのtap・左右・未割当上下・17dp未満・中心復帰・cancel・4面symbol tapがPASSした。
- Surface Observer: 412pxの実browser mockでtap`.`、左`,`、右`=`、上/下無入力を終端まで操作し、`?}`複合表示と横overflow 0を確認した。840pxでも`?}`表示と横overflow 0、JavaScript error 0を確認した。
- 独立AC裏取り: AC 1〜3はすべてVERIFIED。AI-1〜4とout-of-scopeを維持し、API 36.1 attached production Viewの実MotionEventとbrowser mockを本ticketのgesture-routing観察根拠とした。一般アプリへsystem IMEを表示した手操作・視覚観察は未実施として明示する。

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
| - | 全差分 | - | Critical / Major / Minorなし | 解消済み | 独立reviewがnative action、複合描画、mock parity、文書同期、scope、unit testを確認。採用・非採用findingとも0件 |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`の記号切替と絵文字railを`?}`へ更新し、Design decision 33へテンキー`.`の左右割当・未割当上下・共通gesture境界を追記した。`docs/reference/sites-native-spec.txt`、Android/native実装資料、HTML mock実装資料、README、manualも同じ契約へ同期した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- 差分・検証結果を提示後、ユーザの「公開」を本ticketのclose、main統合、pushと次版公開の承認として記録する。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
