# Work Notes: 261030-095708-fix-emoji-reentry-test

## Status: PDH-human-review (AC verified; awaiting physical-device confirmation)

## Checklist
- [x] 利用者の依頼: v0.15.34で残った絵文字再表示の単体テスト失敗を直し、全体テストを再確認する。
- [x] 利用者の依頼: インストールできる公開 APK と製品サイトの案内を確認する。
- [ ] 利用者の依頼: インストール後に実機で挙動確認するまではチケットをクローズしない。
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザの「それは直して」により既知の失敗修正が承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み（逐次3/3、並列1/3とretry-passを記録）
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み - skip: テストfixtureのみの変更で外部provider経路なし
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（現在のAndroidX Viewを検査）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み（Android Emulator 411dp/838dp）
- [x] PDH-verify: ドキュメント更新の要否を確認済み（production/UX変更なし）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

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
- [x] 対象testを冷ビルドで再実行し、変更前はline 572でheader null、Activity attachだけ追加するとline 611で旧headerのattached positionsが[1,2,3,4,5,6,7,8]のまま失敗、現在のheaderと旧headerは別instanceと確認した。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

[2026/10/30 19:02] AndroidX EmojiPickerViewのhead/bodyはRecent選択時に交換される。既存testはRobolectricでrootをwindowにattachせず、生成前のheaderを即取得していた。rootをActivityへattachし、Recent履歴を1件seedし、reset後はpicker内の現在のheader/bodyを取得して検査する。production codeは変更不要。冷状態のfocused testは変更前FAIL、変更後PASS。別経路の反例として変更前の全unit testはPASS（再実行時の結果でありcoldではない）。同じ全unit testを最終変更後にも実行する。

[2026/10/30 19:14] 1st review/AC verifierのCriticalを採用。内部pending set/reflective resetの直接操作はAC1の利用経路を証明せず、再生成testもpendingまたはheaderAttachedのORでAC2の表示を証明していなかった。別カテゴリの実holder click→公開onKeyActionでKANA→EMOJIへ戻る経路へ変更。再生成testは新rootをActivityへattachし、新pickerの現在のheader/bodyでRecent position 0とbody first position 0を確認。冷状態focused 2 test PASS。2nd reviewでCritical/Major残件なし。論理commit `9d9b79e`（旧View参照修正）、`b7b288e`（実操作/再生成の検証）。

[2026/10/30 19:14] 最終コードSHA `b7b288e0359b5a2b8ecec834dc4fcaff04551a76`。`ANDROID_HOME=... scripts/test-all.sh --parallel` の実出力 summary: `PASS: fast-checks` / `PASS: android unit, lint, apk` / `Passed: 2 / 2`。続く `--parallel --connected` はsummary `PASS: fast-checks` / `FAIL: android unit, lint, apk` / `FAIL: android connected (real Mozc)` / `Passed: 1 / 3`。並列unit失敗は `ImeHideBarTest.categoryTransitionRestoresAndroidXFullWidthBodyToKeyboardContentBounds`（body null）と `privateEditorSwitchesToItsDedicatedEmptyPickerWithoutReusingPublicPicker`（expected 0, got 8）。接続側は `VoiceRecognitionAvailabilityTest.reportsInstalledJapaneseModelSupport` 1件。いずれも今回変更した2 testではない。競合のない `ANDROID_HOME=... scripts/test-all.sh --connected` のsummaryは `PASS: fast-checks` / `PASS: android unit, lint, apk` / `PASS: android connected (real Mozc)` / `Passed: 3 / 3`（32/32 connected）。並列だけ失敗した3 testはretry-passとして扱い、green結果へ混ぜない。

[2026/10/30 19:14] AC verifierはAC1/AC2ともVERIFIED。Surface ObserverはAndroid Emulator API 36.1の411dpで顔カテゴリ→KANA→Emojiと操作し、時計アイコン下線とRecent本文への復帰を観察。約838dpへの画面構成変更後もRecent表示を観察。物理端末なし。証拠 `/private/tmp/emoji-smile.png`、`/private/tmp/emoji-after-kana.png`、`/private/tmp/emoji-reopen.png`、`/private/tmp/emoji-recreated-tablet.png`。画面構成変更はconsumer側でのinput surface再構築観察で、`onCreateInputView()` call自体のtraceではない。エミュレータ停止済み。

重複検出: similarity-generic でKotlinはunsupportedと確認しskip。

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
| 1 | AC1 | Critical | 内部pending set/reflective resetだけで実レイヤー復帰を確認しない | 採用・解消 | 現在のカテゴリを実際にclickし、KANA→EMOJIへ公開操作で復帰するtestへ変更。再reviewで残件なし。 |
| 2 | AC2 | Critical | pendingかheader attachのORではRecent表示を確認しない | 採用・解消 | 再生成pickerの現在header/bodyでRecentと本文先頭を検査。再reviewで残件なし。 |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

該当なし。production implementationと公開UIは変更せず、Robolectric test fixtureだけを修正する。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

[2026/10/30 19:14] `b7b288e`の差分、2 ACの証拠、並列failureと逐次retry-passを会話で提示し、close判断を依頼する。ticket close・push・公開更新は未実施。production code差分はないため新APKは不要。

[2026/10/30 21:41] 利用者はインストール後の実機確認前にcloseしないと明示。公開済みv0.15.34 APKのSHA-256 `31fe137ae3e3f4d358832d71191efbaf69827ca944fb28e31f0c1522bd575b91`、38811506 bytes、versionCode 50、ARM64を再ダウンロードして確認。公開サイトの4 core filesは現site/とbyte-identical。テストのみの今回差分はアプリ本体と公開サイトを変えないため、実機確認用に既存v0.15.34を案内する。実機確認待ちを維持する。

[2026/10/30 21:43] 公開サイトのindex/mock/manual/stylesを実HTTPで取得し、GitHub Pages commit `cb401f69ef8c3fcc26420a471bfbe90dd721f5ef`の4ファイルとbyte一致。Pages API statusは同commitで`built`。実ブラウザで412px幅のタップ「あ」と上フリック「う」、840px幅のタップ「あ」を操作し、入力欄更新と横はみ出しなしを確認。公開APKは元のローカル成果物ともbyte一致し、package `com.masuidrive.gestureime`、versionCode 50 / versionName 0.15.34、arm64-v8a、minSdk 28、権限はRECORD_AUDIOとアプリ自身のdynamic receiver permission。ユーザの物理端末確認は未実施。既存v0.15.34が現在のproductionとsiteをすでに公開しているため、同内容の再アップロードは行わない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
