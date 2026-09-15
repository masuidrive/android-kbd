# Work Notes: 260915-100701-reshape-emoji-layer-grid

## Status: PDH-human-review (Approved for release)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み - skip: 外部providerを使わない端末内IMEと静的Webの変更
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
- [x] ユーザ依頼: 絵文字レイヤーからBackspaceを削除する
- [x] ユーザ依頼: Backspace跡を含む最下段まで絵文字一覧を広げる
- [x] ユーザ依頼: 絵文字レイヤーを開くたびRecentタブから始める
- [x] ユーザ依頼: 左のレイヤー切替列をかなレイヤー左列と同じ幅にする
- [x] ユーザ依頼: 左列最上段を絵文字から日本語レイヤー切替へ置き換える
- [x] ユーザ依頼: nativeと操作mockを揃え、APKと製品ページを公開する
- [x] ユーザ依頼: 記号レイヤーの`]`右0.5wをBackspaceにする
- [x] ユーザ依頼: 絵文字レイヤー左上のかな切替ラベルを「あ」にする

## PDH-ticket-review. Ticket contract check
WhyはProduct Briefの6レイヤーと同じジェスチャー体系へ接続する。ACはRecent選択、左列4遷移、左右境界、4行7列、非退行、surface同期として観察可能である。Design Decisionsは2026-09-15のユーザ指示をそのまま固定しており未決判断はない。Dependenciesはなく、AI-1〜AI-4を変更しない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 現行仕様とmockは左railを一律content幅の1/8、右7/8を上3行一覧、最下段右端をBackspaceとしている。nativeも同じ境界をoverlayへ再適用するため、4行化では初回measure・カテゴリ再生成・412/840dpの再確認が必要と特定した。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 実装前に確認した仮定: `KeyboardView` のかな左端列は通常幅ではcontent幅の1/5、Dual Flick有効かつ600dp以上では1/8である。`emojiLayerHorizontalGeometry()` は従来一律1/8で、単一かなの境界と異なることを測定した。絵文字rail、AndroidX body、mask、rail proxyは同じgeometryを使うため、この境界を1か所で表示モードに応じて算出する。
- 実装前の反例: `KeyboardLayouts.emojiContentRows()` は「☺・#!・19」の3行、`emojiControlRow()` は「AZ・EMPTY・Backspace」の最下段だった。`emojiPickerOverlayHeight()` も3行で、既存native/mock/spec/manual/READMEはいずれも3行＋右下Backspaceを記述していた。AC 2/4に反するため4行railと4行bodyへ置換する。
- AC 7の反例: `symbols()` の2行目は`modifier, ^, _, \\, |, ~, {, }, [, ], backspace()`で、Backspaceは既に`]`の右だが幅は既定0.5、centerはnullで下フリックもBackspaceだった。`tapDelete=true, escapeOnDown=true`を明示してQWERTYのflick-BSと同じtap削除・下Escへ変える。
- `KeyboardLayouts`を`あ`・`#!`・`19`・`AZ`の4行railだけへ置換し、右側を4行のAndroidX gridへ拡張した。`KeyboardView`はかなと同じ1/5またはDual Flick時1/8のrail境界をpicker body、mask、touch proxyと共有する。pickerはemoji layerへの可視遷移時だけRecentを選択し一覧を先頭へ戻す。
- mockも`enterEmojiLayer()`で`emojiCategory='recent'`と`emojiScrollTop=0`を明示し、4行7列bodyと`あ`・`#!`・`19`・`AZ` railへ同期した。`site/mock.html`と`docs/reference/mock-source.html`はbyte一致を確認した。
- focused unit testsは`KeyboardLayoutsTest`、`KeyboardViewTest`、`ImeHideBarTest`がPASS。新しいattached Android `MotionEvent` instrumentation testを412dp/840dpのemoji railと記号Backspace用に追加し、`compileDebugAndroidTestKotlin`までPASSした。`adb`がPATHになく接続testはこの環境で実行できない。
- 重複検出: `similarity-ts -t 0.7 site/mock.html docs/reference/mock-source.html`と`similarity-py -t 0.7`を変更ファイルへ実行し、対象言語ファイルなしで検出0。`similarity-generic`はPATHに無くskipした。
- input view再生成時にはpicker状態とemoji可視遷移状態もclearする。絵文字モードを保った再生成で新pickerだけがRecent reset待ちになる回帰testを追加し、同じ絵文字表示中の更新はtransition resetを再発火しない既存testと併せて確認した。`ImeHideBarTest` PASS。重複検出は`similarity-py -t 0.7`で対象なし、`similarity-generic`はPATHに無くskip。

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
| 1 | lifecycle / AndroidX | Major | input view再利用時にRecentへ戻らず、横scroll後はadapter position 0がdetachされる | 採用・解消 | visibility stateをfinish時も初期化し、position 0 holderのattachを待つ回帰を追加した。 |
| 2 | mock / responsive | Major | 絵文字選択でカテゴリがRecentへ戻り、wide Dual時に14列になる | 採用・解消 | 選択中カテゴリを維持し、emoji bodyを常に7列へ限定した。 |
| 3 | RecyclerView lifecycle | Major | child attach中のperformClickがlayout計算中のnotifyを起こし得る | 採用・解消 | identity/pending guard付きheader.postへ移し、同じ非Recent再入場を回帰testした。 |
| 4 | gesture coverage | Major | AZ railの割当済み4方向がattached MotionEventで未検証 | 採用・解消 | 412dp/840dpでtap・4方向・threshold・center復帰・cancelを追加し6/6 PASSした。 |
| 5 | manual surface | Major | manualの絵文字画像が未配置 | 採用・解消 | API 36.1実IME画面を撮影し、manual assetとdocs evidenceへ追加した。 |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- Design decisions 17/18を、Recent再選択、かな列幅と共有geometry、4行7列、絵文字Backspace削除、記号Backspaceのtap/delete・down/Escへ更新した。

## PDH-human-review. 人間レビュー

- 2026-09-15: ユーザは絵文字レイヤーのBackspace撤去、4行化、Recent初期表示、左rail、記号Backspaceを具体的に確認・修正指示し、「合わせてpushして公開して」と本ticketのclose・APK公開・製品ページ公開を明示承認した。
- 提示する確認手順: 絵文字へ入るたびRecentが選択され、左列が`あ`・`#!`・`19`・`AZ`、右が4行7列でBackspaceなしであること。記号2行目の`]`右に`⌫`があり、tap削除・下Escであること。

<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- AndroidX のカテゴリheaderで `getChildAt(0)` はadapter position 0を意味しない。横scroll後はRecentがdetachされるため、layout managerをposition 0へ戻し、position 0のholderがattachして実クリックできるまでRecent resetを保留する。
- `RecyclerView.OnChildAttachStateChangeListener` はlayout計算中にも呼ばれるため、Recent holderのクリックはheaderへpostし、picker/header/bodyのidentityとpending stateがそのままの場合だけ実行する。
- release準備版v0.15.15（versionCode 31）で`scripts/test-all.sh --parallel --connected`を実行し、fast-checks、全unit/lint/APK、API 36.1実Mozc connectedの3/3がPASSした。
- APKは38,634,832 bytes、SHA-256 `94c30778949021294dbd0f2977142af3550a8f36551ffc5641fa2284ca108d0c`、package `com.masuidrive.gestureime`、versionCode 31、versionName 0.15.15、arm64-v8a、RECORD_AUDIOあり、INTERNETなし、zipalign成功を確認した。
- `onFinishInputView` と `onFinishInput` はinput viewを破棄しない。emoji visibility transitionをfalseへ戻さないと、同じviewを再利用した次のemoji entryがRecent resetを省略する。
- emoji pickerのbodyは上記headerと非同期でattachする。Robolectric回帰は非Recent attached headerからproduction resetを呼び、adapter position 0とbody position 0を確認する。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->

## PDH-verify. AC裏取り

- final feature SHA `96abc880709a9acb20ab93b438f27c0387cc829d`を独立verifyし、AC 1〜7はすべてVERIFIED。
- API 36.1 arm64 emulatorのproduction `KeyboardView`を412dp/840dpで計測し、rail幅・touch routing・AZ全方向・Symbols Backspaceを確認した。
- 実`InputMethodService`を起動したnative画像で、左列`あ`・`#!`・`19`・`Az`、右側4行一覧、絵文字Backspaceなしを目視した。
- local browserは412px/840pxで横overflow 0、画像欠落なし、絵文字rail 4キー・4行7列・Backspaceなしを確認した。
- surface limit: physical Foldと実機日本語音声modelは本ticketで未観測。本変更は音声処理を変更しない。
