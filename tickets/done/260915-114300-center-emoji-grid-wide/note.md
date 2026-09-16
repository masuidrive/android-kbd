# Work Notes: 260915-114300-center-emoji-grid-wide

## Status: PDH-human-review (Human Review)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内AndroidX view配置だけの変更で外部providerを使わない
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
- [x] ユーザ依頼: wide画面の絵文字7列を右側一覧領域の中央へ揃える
- [-] ユーザ依頼: APK・製品サイトへ公開する - skip: ユーザが本変更はdeploy不要と明示

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

1499px幅・density 300のAPI 36.1 AVDで、左railを除くAndroidX bodyは1278px、実`EmojiView`は99pxだった。v0.15.15では各viewが7分割slotの左端に置かれ、先頭行の彩色pixel boundsは`212..286, 395..469, …, 1304..1383`だった。最終実装ではbody・slot幅を変えず全viewを約42px右へ移し、`256..330, 439..513, …, 1348..1427`となった。全7列が同じ44px移動し、列間隔は不変。773px幅（約412dp）でも7列・4行viewport・縦scroll構造を目視した。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- AndroidX 1.6.0の`EmojiView.onMeasure()`はslot幅と行高の小さい方からresource paddingを引いた正方形を返す。wideではGridLayoutManagerのslotよりviewが狭いのに、view自体はslot左端へ配置されることを確認した。
- 最初の`6ffe942`はbody全体を実view幅へ縮めたため、実IMEで7列が中央へ密集する誤りを検出した。`8d62829`でその方式を撤回し、body・header・rail・7列slotを維持したまま、各attached `EmojiView`へslot内中央の絶対`translationX`を適用した。
- attach、layout、scroll、カテゴリ遷移、body再生成から同じ冪等処理を呼ぶ。View transformなので表示とhit test、accessibility bounds、variation popup anchorが同じ座標へ追従する。
- commit: `6ffe942 fix(emoji): center wide AndroidX grid`、`8d62829 fix(emoji): center cells inside wide slots`。
- commit: `ee870d6 test(emoji): exercise transformed picker through ime`、`14a81cb test(emoji): cover transformed variant popup`、`b814063 test(emoji): route transformed cells through input dispatcher`、`173ccc1 test(emoji): verify accessibility picker commit`、`c93236b test(emoji): cover phone variation popup`。実Activityへattachした`ImeService`とAndroidX pickerを使い、画面座標から移動後cellをtapした確定、独立したaccessibility actionからの確定、scroll/recycle、wideからphoneへの再layout、wideとphone双方のvariation長押しpopupとvariant確定、picker body再生成を回帰testで固定した。
- focused JVM `ImeHideBarTest` PASS、API 36.1 connected `EmojiPickerBodyLayoutInstrumentedTest` 3/3 PASS。実IMEのbefore/after/phone画像と、移動後の先頭cellをtapして入力欄へ`😀`を確定した画像は`evidence/`へ保存した。
- final code `c93236b`で`scripts/test-all.sh --parallel --connected`を実行し、fast-checks、android unit/lint/APK、API 36.1 connected real Mozcの3/3がPASSした。

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
| 1 | test追従 / test到達可能性 | Major | 初回testがsynthetic viewだけで実AndroidX pickerのtap、variation、recycle、再layoutを通っていない | 採用・修正済み | `ee870d6`〜`173ccc1`でproduction `ImeService` + AndroidX pickerへ画面座標から入力するconnected regressionを追加し、各操作の件数と文字を完全一致で検証した |
| 2 | doc sweep | Minor | exact SHAにslot内transform契約の文書更新がない | 採用・修正済み | `docs/reference/sites-native-spec.txt`と`technical-reference.md`を同期した |

PDH-review-1ではsynthetic testだけで実操作を通さないMajorと文書同期のMinorを採用した。PDH-review-2ではcellへ直接eventを送るfalse-positive経路をMajorとして採用した。PDH-review-3では画面座標tap・variation・scroll/recycle・phone再layout・body再生成によりMajor解消を確認し、accessibility actionの確定未検証をMinorとして採用した。`173ccc1`で独立したACTION_CLICKの成功、確定件数+1、対象絵文字の完全一致を追加した。

壊していない側の入力としてphone幅を選び、773px幅では変更前後とも7列・4行viewportを維持し、最終connected testでも412dp相当への再layout後に全attached cellが再計算値へ戻ることを記録した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

wideでAndroidXの正方形`EmojiView`を7分割slot内中央へview transformする実装契約を`technical-reference.md`へ追記する。
`docs/reference/sites-native-spec.txt`にも同じ再適用契約を同期した。独立最終reviewで両方が最終記録commitへ含まれればdoc sweep解消と判定された。

## PDH-verify. AC裏取り

final code `c93236b`を独立verifyし、AC1〜AC4はすべてVERIFIED。1499px wideのbefore/afterでは7列が同じ44px右へ移動し、body・rail・列間隔は不変。412dp相当では7列・4行、画面座標tap、variation長押しpopupとexact variant確定を直接確認した。実AndroidX pickerでaccessibility click、scroll/recycle、body再生成後のtransform再適用も確認した。Surface Observerは4枚のAPI 36.1実画面でwide中央配置、tap確定、phone非退行を確認した。物理Fold実機は本ticketでは未確認。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

2026-09-16: ユーザの「push deploy」を、未統合のwide絵文字中央揃えを含めてclose・pushし、次のreleaseへ含める明示承認として記録した。本ticket内ではdeployせず、統合後のrelease作業で公開する。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- AndroidXのRecyclerView/body境界自体は既に正しかった。偏りの原因はGridLayoutManagerのslotではなく、その中で行高により縮んだ`EmojiView`がSTART配置されることだった。
- AVDは空き444MBでも既定の低容量しきい値により38MB debug APKを拒否した。別アプリは削除せず、`sys_storage_threshold_*`をinstall中だけ一時変更して直後に既定へ戻すことで検証した。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
WhyはProduct Briefのwide画面対応へ接続し、ACは実AndroidX pickerの左右余白とphone非退行として観察可能。左rail・Recent・カテゴリ・収録内容を変えず、公開しないというユーザ判断も固定されている。未確定判断と依存はない。
