# Work Notes: 260912-080017-show-emoji-recents-and-expand-catalog

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: AndroidX Emoji Pickerと端末内SharedPreferencesだけを使い、外部provider/API経路がない
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

現行は`EmojiCatalog.entries`と公開mockの双方が固定32件で、recent最大8件をcatalog先頭へ連結している。候補欄は全高50dp、face 34dpをtop14/bottom2に置き、最上段key faceまで10dpである。ユーザは絵文字レイヤーで候補欄をカテゴリicon行へ置き換え、先頭のRecentを選ぶと下の3行へ履歴を出す方針を明示した。公式資料では`EmojiPickerView`が横方向のclickable header、縦scroll一覧、最近使用、長押しvariationを提供し、`emojiGridRows`/`emojiGridColumns`を設定できる。安定版1.6.0はEmoji 16.0をsupportしminSdk 23、現appはminSdk 28/compileSdk 36である。1.7.0-rc01はCompose側compileSdk 37.1を要求するため採用しない。未確定のproduct判断はない。

独立AC読み手は最新契約のAC 1〜8をすべて復元した。絵文字レイヤーだけ候補欄を50dpのカテゴリicon行へ置換し、その下を一覧3行＋固定control 1行とする構成は全体高不変と整合する。Recentの新しい順・重複なし最大24件、確定時の先頭反映、private時の履歴非表示と通常欄での復元も操作結果として観察できるため、更新後契約をticket-human-review承認として扱う。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

実装後に、最上部50dpのcategory icon行＋一覧3行＋既存control 1行の全体固定高、8列、category移動、Recent最大24件、variation長押し、一覧tapの1回確定・並替え・editor/token guard、private時Recent非表示、非絵文字候補のface 38dp・top10/bottom2・最上段keyまで10dp、412/840・Light/Darkのnative/mock overflowを測る。AndroidX pickerの外部通信は発生しないことを依存関係と実行時から確認する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

[2026/09/13 15:23 JST] 初回Recent更新の境界修正を反例検証した。階層追加callback内で`bindEmojiPickerViewport`を同期実行すると、AndroidXがRecyclerView構築中にbody heightを変更して既知の`ImeServiceEnglishSuggestionTest.emojiCommit...`がlayout loopへ入った。picker rootの`clipChildren`・`clipToPadding`と固定control境界の`clipBounds`だけを同期維持し、bodyのoverscan・listener・accessibility処理は従来どおりpost/global-layout後に限定した。追加testから本番経路でない`emojiGridColumns=9`同期再構築を除き、412dp・840dpと全height presetで、posted settling前後ともpicker bottomがAZ/BS操作行の上端と一致することを固定した。既知Recent更新testとの同時focused実行は8秒でPASS。

[2026/09/13 14:54 JST] v0.15.6実機feedback: fresh installで初めて絵文字レイヤーを選ぶと、AndroidXが最初のRecyclerView bodyをcell作成用に`viewport + 8dp`で再生成する瞬間に固定AZ/Backspace control rowへ侵食した。picker rootの暗黙のclipに依存せず、rootをdrawing/child boundaryとして明示clipし、body再生成直後にも3行viewportのclipとcell accessibilityを同期する。Robolectricでは412/840dp・小/標準/大・初回/AndroidX body再生成直後/posted settle後でpicker/bodyの下端がcontrol topを越えないことを固定した。API 36.1 AVDではfresh installの初回絵文字表示でIME frame `[0,1545][1080,2400]`、control row座標`x=105,y=2200`のtapがQWERTY切替へ到達することを確認した。Kotlin向け`similarity-generic -t 0.7`は環境に存在せずskipし、既存のboundary/viewport helperを照合した。

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
| 1 | privacy | Major | 通常Recentのproviderをprivate pickerへ差し替えると、非同期refresh中に履歴が一時表示され得る | 採用・修正 | picker instanceをpublic/privateで分離し、private providerは常に空にした |
| 2 | geometry | Major | カテゴリ切替直後の古い行高で確定すると4行目の上端が見える | 採用・修正 | 選択categoryの新しい行attachを待ち、preset物理高とclip/maskを分離した |
| 3 | Recent | Major | AndroidXのdirty更新が非同期commitより先に消費されると成功後のRecent表示が古い | 採用・修正 | public commitと保存成功後だけproviderを再設定してadapterをrefreshした |
| 4 | final review | - | `4be7208`の成功/private/stale guard、Recent adapter、mask、固定高を再確認 | 解消 | Critical 0 / Major 0 / Minor 0 |

指摘修正の反例として、private→public復帰時の保存済み履歴、commit失敗、古いeditor token、空Recent、Faces/People切替、Small/Standard/Largeを各修正前後で維持した。`640f9fb`では該当service testが約121秒だった状態から13秒、`4be7208`後は10秒で完了し、Recent provider内容も`[❤️]`へ更新されることをtestで固定した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

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
実装前に、AndroidX 1.6.0のheader/list構造、Recent providerの保存順序、カテゴリ切替時のRecyclerView配置時機、private欄でproviderを差し替えた場合の非同期漏洩、画面幅・高さpreset変更時の再計測を仮定として列挙した。AAR bytecodeとAPI 36実画面を測り、通常用とprivate用pickerを分離した。

`d36a5fe`でAndroidX picker、最大24件Recent、候補face 38dp、native/mockを導入した。`19854c9`でprivate専用pickerへ分離し、`6b9926e`以降で幅・preset・category変更時のgeometryを実測値へ追従させた。カテゴリのRecyclerViewはheader選択より後で行をattach/recycleするため、stale callbackと選択categoryを識別し、picker本体はpresetの物理高へ固定、見える末尾だけclip/maskする構成へ収束した。`8c6053d`で空Recentを実際にattachされた履歴なし表示と後続2行の下端へ切った。

`640f9fb`で不要なparent requestLayoutと同値mask更新を除いた。reviewで、AndroidXの選択順がlistener→provider記録→dirty設定であり、非同期commit中にdirtyを消費できることを検出した。`4be7208`でcurrent editorかつpublicのcommit成功とPreferences保存後だけpublic picker providerを明示更新し、表示中Recentの即時並替えを固定した。変更は`96b60be..4be7208`の論理commitへ分割した。

重複検出はKotlin/HTMLを対象に使える同梱`similarity-generic`が環境に無いためskipし、既存helperとlayout patternを`rg`で照合した。最終`./scripts/test-all.sh --parallel` summaryは以下。

```text
========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk

Passed: 2 / 2
```
`technical-reference.md`の決定17へ、AndroidX pickerの50dpカテゴリheader、8列3行、最大24件Recent、private provider、成功確定時だけ保存する現仕様を反映済み。決定18へ、絵文字時のoverlayとpreset固定高、入力先切替時の再適用を反映済み。
AndroidX Emoji Picker 1.6.0はカテゴリtap後にRecyclerViewの行を非同期でattach/recycleするため、header選択直後の旧行高を新categoryの確定値として使えない。`setRecentEmojiProvider`はpicker bodyを再生成せずRecent adapterをrefreshするが、絵文字選択callbackより後にAndroidX自身のprovider記録とdirty設定が走る。アプリ側の非同期commit成功後に明示refreshする必要がある。
API 36 arm64 emulator（1080x2400、420dpi、Light）で、空Recent、Facesから😀確定直後のRecent先頭反映、privateの空Recentとpublic復元、カテゴリ連打、Small/Standard/Large、設定と入力テストの10表示を確認した。StandardはIME top 1472、QWERTY first key top 1624、4行bottom 2173、hide bar top 2178、nav pill 2364で10表示とも不変。SmallはIME top 1524/control top 2070、Largeは1419/2044。各presetで下端cut/sliverなし。最終画像は`site/assets/emoji-categories-api36-v0.13.png`。

物理Fold端末は未接続のため、最終human reviewでは公開APKをFoldの外画面・内画面で開き、絵文字レイヤーの3行高とアプリ切替直後の高さを確認する。
