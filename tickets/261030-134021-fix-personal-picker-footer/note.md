# Work Notes: 261030-134021-fix-personal-picker-footer

## Status: PDH-implement (キー処理の共通化を追加実施、human device review pending)

## Checklist
- [x] 利用者の依頼: 単語一覧のSpace上下フリックはカーソル操作にし、レイヤー切替を起こさない。
- [x] 利用者の依頼: 左下キーは元のレイヤーを表示し、tapで一覧を閉じ、各方向のフリックで指定レイヤーへ切り替える。
- [ ] 利用者の追加依頼: 単語一覧と通常レイヤーでキー種類ごとの定義・タッチ処理を構造化し、同じ挙動を別々に手書きしない。
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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: この変更はローカルIMEとブラウザモックだけで、外部provider呼び出しがない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check

[2026/10/30 22:41] Product Briefの6レイヤー・フリックと、technical-reference.mdの個人情報panelを対象にする。消費面はAndroid IMEの固定高panelと製品ページの操作mock。AC4件は独立AC読み手が全件復元可能と判定。ユーザの文面がtapの元レイヤー・方向別切替・Space上下の目的を明示しており、同内容の実装承認として扱う。変更前のfocused testはPASSで、現行テストはこの報告を捕捉していない。
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
- [x] 現行native footerはlayer keyのtapをKANA/QWERTYへ反転し、Spaceはtrackpad=trueだが上下24dp単位。mockは左フリックを持たないvoiceNavigationを流用し、tapもKANA/QWERTYへ反転することをコードで確認。変更前focused unit suiteはBUILD SUCCESSFUL。

## PDH-implement. 実装ログ

[2026/10/30 22:41] 依存仮定: panelが開いた時点のkeyboardModeはservice側で維持される。MotionEventはDOWN targetへ続けて配送される。mockのpointer captureはreleaseまで同じkeyを保持する。nativeの既存Space trackpad自体は上下CursorDeltaを返すが、単一行では移動先がないため見た目の変化がない。実機と異なる可能性はSurfaceで確認する。非対象の編集キー（句読点・Backspace・Enter）を反例として変更前後で比較する。
[2026/10/30 22:50] Nativeの元レイヤー表示・tap復帰・フリックpreviewを4a5661cへcommit。Spaceの上下35dp MotionEventをattached pickerとIME service経由で送り、複数行editorでカーソル8→5→8、一覧維持、レイヤー不変を確認。短いフリックまたは単一行では上下に移動先がなく、実機の「時々レイヤー変更」の根本原因は未再現。ブラウザモックは実サーバーで上/下Space操作が5→8、一覧維持を確認し、レイヤー上フリックで日本語へ切替を確認。
[2026/10/30 23:25] 追加依頼の依存仮定: pickerの下段キーは既存KeyboardLayoutsのlayer/Space/Enter/voice punctuation factoryを再利用でき、差分は表示ラベル・more..・幅だけ。元のキー割当は専用MapとaddKey引数で重複、短いSpace縦10〜23dpはpickerのACTION_UPだけに別実装。通常キーボードも同じ10〜23dp縦操作をSpace tapにしていた。通常Spaceとpicker Spaceを共通にするため、両方で短距離縦を1行移動へ統一する。単一行では移動先がない既存制約を維持。
[2026/10/30 23:37] `144299f`でpicker下段の全キーをKeyboardLayouts.personalFooterのKeySpecへ集約。more..もExpandPersonalInfoアクションにし、他キーと同じonEditAction経路へ通した。短い縦Spaceの判定はGestureInterpreter.finishTrackpadへ移し、通常KeyboardViewとpickerが共用。最初にMOVE時確定へ寄せる案はcancelでカーソルが動く回帰をfocused testで検出したため採用せず、UP時だけの確定にした。反例は通常tap、句読点、Backspace、Enter、more..、center returnとcancelで、focused 160件がPASS。重複検出はsimilarity-genericがKotlin非対応、similarity-tsがHTML内scriptを対象外と報告したためskip（ツール側の入力形式制限）。
[2026/10/30 23:39] 独立レビューでMajor 2件を検出した。KeySpecと距離判定だけではタッチ状態・方向選択がView間で二重実装、さらに通常6レイヤーの短距離Space挙動を変更してticketの対象外契約に違反していた。`0b1edd1`でKeyGestureRouterを抽出し、2つのViewが共通のbegin/move/end/cancel、方向割当とcursor判定を利用するよう修正。通常Spaceは従来のtapへ戻し、単語一覧下段だけUP時の短距離カーソル移動を有効にするpolicyを明示した。router・実Viewの焦点テストPASS。全体試験と再レビュー待ち。
[2026/10/30 23:41] 再レビューでMajor 0、戻り方向の触覚だけMinorとして検出し、従来どおり中心復帰で触覚を出すよう修正。全体並列試験では絵文字系の単体1件・接続1件が失敗したが、同じテストを単独で各1回再実行すると両方PASSした。並列実行に伴う不安定さの可能性があり、最終SHAで全体を再実行する。ブラウザmockは通常Spaceの短距離縦を従来の空白入力へ揃え、実操作で確認した。
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

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
| 1 | touch threshold / native-mock parity | Major | Space縦10〜23dpでnativeが空白入力、mockは無操作 | 採用 | trackpad開始と24dp単位の間を最小1行移動へ修正し、両surfaceを一致させた |
| 2 | test coverage | Minor | レイヤーキーの中心復帰とcancelが未検証 | 採用 | attached picker MotionEvent testを追加 |

## Test evidence

最初の確定SHA `65bd0841dfe035f2b0695160409b698dc91b4c67` で `ANDROID_HOME="$HOME/Library/Android/sdk" scripts/test-all.sh --parallel --connected` を1回実行した出力:

```text
PASS: fast-checks
PASS: android connected (real Mozc)
FAIL: android unit, lint, apk
ImeHideBarTest > emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway FAILED
374 tests completed, 1 failed
Passed: 2 / 3
```

失敗はこのticket差分が触れていない既存 `ImeHideBarTest.kt:572` の `requireNotNull` で、別の未close ticket `261030-095708-fix-emoji-reentry-test` に修正がある。変更対象 `PersonalInfoPickerViewTest` と `ImeServiceEnglishSuggestionTest` は修正前後ともPASS。別実行の `./gradlew lintDebug assembleDebug` はBUILD SUCCESSFUL。

短い縦フリックの修正前: 15dpでCursorDeltaなし→Space入力。修正後: attached pickerの15dp上/下MotionEventはMoveCursor(UP/DOWN)を各1回、入力Spaceなし。IME serviceの入力欄カーソルは8→5→8、登録候補一覧と元レイヤー維持。モック実サーバーでも15px上/下をマウス操作し、8→5→8、入力値不変、一覧維持。10dp未満のtap、35dp縦、中心復帰、cancel、layerキーcenter return/cancelを関連testで確認。

最終コードSHA `c539a4a011be8c541274a5c31be7fe1990e80182` で `scripts/test-all.sh --parallel --connected` を実行し、fast-checks・Android unit/lint/APK・Android connected (real Mozc) がすべてPASS（3/3）。初回の既存絵文字単体試験1件の失敗はこの最終実行では再現しなかった。独立レビュアー2名の再レビューはCritical/Major残件なし。前Majorの10〜23dp短距離フリックと前Minorのレイヤーキー中心復帰/cancelは解消。非対象の句読点・Backspace・Enter・moreの既存attached testは変更前後ともPASS。AC裏取りAgentは4件すべてVERIFIEDと判定した。物理端末での操作は未確認。

## Technical reference 更新

technical-reference.mdの個人情報panel記述を元レイヤーtap復帰・4方向フリック・popupと短いSpace縦フリックの最小1行移動へ更新。docs/reference/sites-native-spec.txtとsite/manual.htmlも同内容へ更新。mockは実操作に合わせて左フリックと元レイヤー復帰を修正。

## PDH-verify. Surface evidence

最終SHAのAPKをAndroid 16エミュレータ（412dp）へ入れ、設定で候補登録→入力テスト→単語一覧までSurface Observerが実操作した。元がQWERTYのとき左下はAzを表示し、固定高とキー配置に重なり・欠落はない。画像 `/private/tmp/footer-surface-final-picker3.png`。Space上下の実カーソル移動とレイヤーキー全方向は最終SHAのattached MotionEvent/connected testで確認したが、Surface Observer自身は最終APK上で全方向を操作していない。物理端末は利用者確認待ち。エミュレータは停止した。

## 試用APK

最終APK `app/build/outputs/apk/debug/app-debug.apk` は38,910,970 bytes、SHA-256 `34f46e293bbfd6d9348c633c56574aa6a35afaee96c6d24ddfcb5c5b6ce5aa5e`、package `com.masuidrive.gestureime`、version `0.15.34` / code 50。Flyerの `https://vhduzh3k.aboutme.style/` に2026/10/31 23:08 JSTまでの24時間試用版として掲載。20MiB制限に合わせた2分割保存hashと、ページ内の結合APKの期待hash/sizeを手元と照合した。旧2分割ファイルは削除。正式GitHub Releaseとticket closeは実機レビュー待ち。
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
試用APKの案内と検証結果をユーザへ提示する。Android実機で単語一覧を開き、複数行の入力欄でSpace上下フリックのカーソル移動と一覧・レイヤー維持、左下キーのtapで元レイヤー復帰、左・上・右・下フリックの音声・日本語・QWERTY・テンキー切替を確認してもらう。1行欄では上下に移動先がないため、複数行欄を使う。確認報告とcloseの明示承認までticketは開いたまま。
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
