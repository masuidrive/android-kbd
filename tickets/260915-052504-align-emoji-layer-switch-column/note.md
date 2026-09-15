# Work Notes: 260915-052504-align-emoji-layer-switch-column

## Status: PDH-human-review (Approved for push/release 2026-09-15 00:02 JST)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内AndroidX EmojiPickerとcustom Viewだけを使い、外部provider/API経路はない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticketではないため412dp相当/wide実Androidで確認)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（README、manual、native spec、mockを更新。pdh-update非該当）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
- `[PDH-open] -> [PDH-ticket-review]` — 絵文字layerだけ異なる左端操作を、日本語layerの既存4段railへ揃える単一work unitとして定義した。
- ACはnativeとmockで、4ラベル、各遷移、絵文字tap/scroll/Recent、412px/840pxの高さ・重なりを観察できる。未確定のproduct判断やDependencyはない。
- `[PDH-ticket-review] -> [PDH-ticket-human-review]` — ユーザの配置指示と「合わせてpushして公開して」を、上記ACの実装・公開承認として記録した。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 現行nativeはAndroidX EmojiPicker bodyが全幅8列を占有し、custom KeyboardViewは最下段の`AZ`と右端BSだけを描くことを確認した。日本語layer左列は上から`☺`、`#!`、`19`、`AZ`である。bodyを右側7/8幅・7列にすると既存cell幅と4行高さを維持できることを実装後に412px/840pxで測る。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `[PDH-ticket-human-review] -> [PDH-implement]` — ユーザの配置指示とpush・公開指示、Required Probes完了により実装開始。
- 書く前の仮定: AndroidX pickerはcategory headerとbodyを非同期生成し、category移動時にbodyを交換または`MATCH_PARENT`へ戻す。固定railはcustom `KeyboardView`が描き、picker bodyとtouch proxyは同じcontent inset座標を使わなければならない。Recentが空のときもbodyは7列であり、maskはrailを覆ってはならない。
- `KeyboardLayouts`のemoji 4行へ`☺ / #! / 19 / AZ`を固定配置し、picker bodyを右7列、category headerを全幅、control row右端をBSとした。picker上の透明proxyはproduction `KeyboardView`へ同じ`MotionEvent`を転送する。
- phoneとwideの既存3dp/10dp content insetを共有geometryへ集約し、picker body・rail proxy・mask・KeyboardViewが同じ`railRight..contentRight`を使う。AndroidX bodyの再生成時は親paddingをfirst measure前に復元する。
- empty Recentの完全行判定を旧8列からproduction 7列へ統一し、別categoryからempty Recentへ戻るready判定を追加した。viewport縮小maskはbody領域だけに限定した。
- nativeとmockの仕様をREADME、manual、`docs/reference/sites-native-spec.txt`、`technical-reference.md`へ反映し、`site/mock.html`と`docs/reference/mock-source.html`をbyte-identicalに保った。
- logical commits: `6fdb2c8` fixed rail実装、`b98df8e` mock/docs整合、`4caece6` AZ accessibilityとtouch test、`81885cf` shared content geometry、`30a862b` category first-measure・7列Recent・mask安定化。
- similarity-genericはPATHに未導入のため重複検出skip（環境制約）。既存layout helperとtest harnessを拡張し、新しい汎用utilityは導入していない。
- focused `ImeHideBarTest` + `KeyboardViewTest`は`--rerun-tasks`で83件を2回連続PASS。AC verifierの4クラスは124/124 PASS。
- final code SHA `30a862b`で`scripts/test-all.sh --parallel --connected`を実行し、fast-checks、Android unit/lint/APK、API 36 emulator実Mozc instrumentationの3/3 PASS。

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
| 1 | 横座標 | Major | 初回実装の`W/8`が既存content insetを無視し、wideでbodyがrailへ重なる | 採用・修正済み | KeyboardView・body・proxyを共有content geometryへ統一した |
| 2 | category lifecycle | Major | AndroidXがcategory切替時にbodyを全幅へ戻し、次frameまでrailを覆う | 採用・修正済み | body親paddingを再生成treeのfirst measure前に設定し、強制再実行testで固定した |
| 3 | empty Recent | Major | 完全行判定が旧8列のままで、7列bodyのRecent往復がsettleしない | 採用・修正済み | production 7列定数へ統一し往復ready fixtureを追加した |
| 4 | 縮小mask | Major | full-width maskが固定railを覆いうる | 採用・修正済み | maskを`railRight..contentRight`へ限定し非重複testを追加した |

- 壊していない側の反例: 412↔840の外部幅変更、非空category、picker再生成なしの安定frame、通常高さの4行を修正前後でfocused testへ流した。既存のcategory tap、emoji tap、BS、QWERTY/記号/数字遷移は維持された。
- 独立再review対象`30a862b`はCritical/Major/Minorなし。focused 83件PASS、mock/reference byte一致。
- AC verifierは既知コード欠陥の解消と124/124 PASSを確認し、残したsurface項目を下記の実Android操作で補完した。

## PDH-verify. AC裏取りとsurface観察

- AC1: 412dp相当とwide実Androidで左列`☺ / #! / 19 / AZ`を観察し、railから記号、テンキー、QWERTYへ実tapで遷移した。production MotionEvent testも同じ経路を覆う。
- AC2: fresh app dataの412dp相当でempty Recent→人物category→empty Recentを実tapし、戻った直後も7列bodyがrail右側に留まることを撮影した。wideでcategory変更、縦scroll、絵文字tapによる入力、BSによる削除を連続操作した。
- AC3: 412dp相当とwideでかな→絵文字を切り替え、IME上端・下端、4行高、safe areaが不変で、rail、7列body、AZ、BS、縮小maskが重ならないことを目視した。
- surface evidence: ticket-local `tmp/native-empty-recent-phone.png`、`tmp/native-category-after-empty-phone.png`、`tmp/native-return-empty-recent-phone.png`、`tmp/native-emoji-category-wide.png`、`tmp/native-emoji-scroll-wide.png`、各rail遷移画像。いずれもgit対象外。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`のemoji構造を全幅category header + 左固定4段rail + 右7列bodyへ更新し、body再生成時の共有geometry復元を記載した。native spec、README、manual、browser mockも同じ配置へ揃えた。PDH配布物変更ではないためpdh-update非該当。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- review APK: `/Users/masuidrive/Develop/personal/android-kbd/app/build/outputs/apk/debug/app-debug.apk`。
- 確認手順: かな左上`☺`から絵文字へ入り、左列の`#! / 19 / AZ`をtapする。Recentと別categoryを往復し、縦scroll、絵文字入力、BS削除を行い、4行高とsafe areaが変わらないことを確認する。
- 2026-09-15 00:02 JST、ユーザは「絵文字レイヤーの左には かなレイヤー と同じように縦にレイヤー変更並べて。合わせてpushして公開して」と明示した。この絵文字変更の実装・検証後のpush・release・closeまでを承認した指示として記録する。上記結果と実Android証拠は2026-09-15 15:40 JSTに会話へ提示した。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- AndroidX EmojiPickerはcategory切替時にRecyclerView bodyを交換し、幅を`MATCH_PARENT`へ戻す場合がある。body自身のmarginだけを後続global-layoutで直すと一frame全幅になるため、保持される親paddingを再生成treeのfirst measure前に適用する必要がある。
- empty Recentのplaceholder後にもfallback emoji rowsがあり、完全row判定の列数は`emojiGridColumns`と一致させる必要がある。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
