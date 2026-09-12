# Work Notes: 260912-055128-make-emoji-grid-scrollable

## Status: PDH-open (Opening)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] 絵文字一覧をnativeと公開mockで3行scroll viewportへ変更し、recent・tap・削除・layer flick・4行高を維持する。
- [x] 意味のあるRobolectric/unit test、focused/full test、manual/reference/progress更新を行い、実装を論理単位でcommitする。
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: APK同梱catalogと端末内SharedPreferencesだけを使い、外部providerを設けないticketである。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

添付画像と指示から、問題は2行paginationで一覧が少なく見えることと判断した。上3行を縦scroll viewport、最下段を固定操作行とするACは画面・tap・dragで観察でき、既存の固定4行高、recent、端末内完結と矛盾しない。追加判断を要する未確定事項はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

実装前に、nativeはcatalog 32件を2頁×16件へ分け、recentを空枠付きの独立1行にしていること、`KeyboardView`が全keyを同じ`GestureInterpreter`へ渡すこと、mockも同じpage stateをlocalStorageへ保存することを読んで確認した。scroll viewportでは、(1) 32件だけなら4 content rowsとなり3行viewportのscroll rangeが1 row pitch、(2) recent最大8件なら5 content rows、(3) clipped rowをhit target/accessibilityから除く必要がある、(4) drag開始後はemoji tap/long-press popupをcancelする必要がある、と測定対象を定めた。外部providerは使わない。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `4203855` `[260912-055128-make-emoji-grid-scrollable] feat(emoji): scroll continuous catalog viewport` — page state/actionを削除し、recent最大8件と32件catalogを空行なしで連結した8列content rowsを`KeyboardView`の上3行へclipした。dragが12dpを超えるとtap/long-press popupをcancelしてpixel単位でscrollし、最下行のAZ layer flickと削除は固定した。縮小されたIME hostでは実measure row pitchでscroll rangeをclampする。TalkBackはvisible nodeだけを出し、hostのforward/back actionで1行scrollする。公開mockとreference sourceも同じviewport・tap/drag分岐へ同期した。
- focused: `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests com.masuidrive.gestureime.keyboard.KeyboardLayoutsTest --tests com.masuidrive.gestureime.keyboard.KeyboardViewTest` — PASS。空recentの先頭24件、recent連結、drag時non-commit、catalog tap、固定control row、縮小host clamp、TalkBack scroll actionを固定した。
- full: `scripts/test-all.sh --parallel` — fast-checks、Android unit/lint/APK PASS。
- review repair（commit pending）: mockはpointerupのみemoji commitとし、pointercancel/lost capture/cancelAll/cancelGestures/blur/visibility/resizeではgesture mapとpointer captureを入力なしで破棄する。native hostはスクロール可能な方向だけをTalkBackへ公開し、境界no-opはfalseを返す。focused/full PASS、browserのblur/cancel反例を記録した。
- review attempt2 repair（commit pending）: emoji viewportのroot clickを他のkeyと同じ`e.detail == 0`限定へ戻し、物理pointerの確定をpointerup helperだけにした。browserで通常pointer tap 1回、keyboard click 1回、drag 0回、lost capture通知後のpointerup/click 0回、blur後 0回を確認した。

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
| 1 | 対称関係・test到達可能性 | Major | 公開mockのemoji gestureは`pointercancel`と`lostpointercapture`でもtapと同じhelperを呼び、12pxのdrag判定前なら絵文字を確定する | 修正済み（再review待ち） | `finishEmojiViewportGesture(pointerId, commit)`を分離し、`pointerup`だけが`commit=true`となる。`pointercancel`/`lostpointercapture`は入力せずmapを破棄する。`cancelAll`/`cancelGestures`はcaptureをreleaseする前にemoji gesture mapをclearする。実browserでmouse down→blur→mouse up後のtextarea/recent不変、pointercancel直後とcapture release直後のtextarea不変を確認した。 |
| 2 | TalkBack境界 | Minor | native hostはscroll端でも前後両actionを公開し、offsetが変わらないactionを成功として返す | 修正済み（再review待ち） | `KeyboardView`は先頭でforwardのみ、末尾でbackwardのみをhostへadvertiseし、clamp後にoffsetが変化しなければ`performAccessibilityAction`はfalseを返す。`KeyboardViewTest`で先頭/末尾のaction listとno-op falseを固定した。 |

対象: base `8e58f0a7a13d3f7abd3c6d3420f78305bac9481c`、target `042922affc204811f93320ab3d81c55fe6abede6`（implementation `4203855`、docs `042922a`）。Criticalなし、Major 1、Minor 1。

### AC / 確定判断の対応

- AC 1: `KeyboardLayouts.emojiContentRows`の先頭3行と`emojiControlRow`を4行layoutへ組み、`KeyboardView.buildEmojiHitTargets`が上3行viewportと4行目controlを分離する。layout/view testが4行高と3行描画を確認する。
- AC 2: `EmojiScrollGesture`が12dp以降をtapから切り離し、pixel offsetを実row pitch由来の範囲へclampする。通常高と縮小hostのview testが到達する。
- AC 3: normalized recentと32件catalogを空行なしで連結して8件ずつchunkする。layout testが重複recent除去後の順序、5 content rows、各8 width unitを確認する。
- AC 4: `ChangeEmojiPage`とpage state/actionをmodel・service・layoutから削除し、固定control rowへ既存layer flickとbackspaceを置く。nativeのdrag非commit・catalog tap・layer tap、および既存backspace gesture testsへ到達する。
- AC 5: nativeはCanvas clipと可視hit/accessibility node、mockはoverflow viewportとpointer追従、cancel/capture loss/blur時の無確定破棄、docsは3行連続scrollへ更新した。Light/Darkと幅は既存共有geometryを維持する。
- 縦方向・上3行viewport・固定最下段: `buildEmojiHitTargets`とmockの`.emoji-scroll-viewport`＋別control rowへ対応する。
- recent＋catalog連結・空recent行なし: `emojiContentRows`と両mockの`emojiRows`へ対応する。
- 行単位に制限しない追従・端clamp: nativeのfloat offsetとmockの`scrollTop`へ対応する。
- Architectural Invariant / Out-of-scope: catalogは静的同梱、recent保存形式と`CommitEmoji`経路、4行preset、Space cursorに変更なし。

修正後はfocused `KeyboardLayoutsTest`/`KeyboardViewTest`とfull suiteを再実行してPASSした。公開mockは自動suiteがないためbrowserで、通常tapの😀確定を維持したうえでmouse down→window blur→mouse upのtextarea/recent不変、pointercancel直後とcapture release直後のtextarea不変を確認した。pointercancelの後に人工的なmouse upを追加すると通常clickという別イベントになるため、その不自然な列は反例証跡に採用しない。Critical/Major解消の最終判定は再reviewへ残す。

### Findings (PDH-review-2)

対象: `1deb8522b1f894ab1bfd11e54d1b1a1a27eefadd`。Critical 0、Major 1、Minor 0。

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | 対称関係・gesture完了 | Major | 前回Majorは`pointercancel`直後のhelper commitを止めたが、capture loss後の物理click fallbackからまだcommitできる | 修正済み（再review待ち） | emoji viewportだけのphysical click許可を削除し、root clickは全keyで`e.detail == 0`のkeyboard/accessibility activationだけを扱う。物理pointerはpointerup helperだけがcommitする。browserで通常pointer tap 1回、keyboard click 1回、drag 0回、lost capture通知後のpointerup/click 0回、blur後のpointerup/click 0回を確認した。 |
| 2 | TalkBack境界 | Minor | 先頭/末尾の不可能actionを公開し、no-opでも成功を返す | 解消 | `KeyboardView.kt:638-657`がoffsetに応じてforward/backwardを個別公開し、`scrollEmojiTo`の変更有無を返す。更新testは先頭backwardと末尾forwardのaction非公開・戻り値false、可能方向の移動を確認する |

通常`pointerup`だけをhelper commitへ渡す分岐、cancel/capture loss時のmap先行clear、`cancelAll`/`cancelGestures`からのcleanup、native TalkBack境界はそれぞれ意図どおりで、通常tap・drag・fixed control row・scroll rangeへの新しい退行は見つからなかった。mock/reference sourceの修正形も同期している。公開mockは自動suiteがないためbrowserでcapture loss通知後のpointerup/clickまで反例を到達させ、入力なしを確認した。focused/fullも再実行してPASSした。Critical/Major解消の最終判定は再reviewへ残す。

### Findings (PDH-review-3)

対象: `83ecf399854f702a488e0dae3328ef41db6a067a`。Critical 0、Major 0、Minor 0。No Critical/Major。

- 前回Major: 解消。`site/mock.html:970-997`と`docs/reference/mock-source.html:893-913`は、通常の物理tapを`pointerup` helperで1回確定し、続く物理DOM clickを`e.detail != 0`で破棄する。`pointercancel`/`lostpointercapture`はhelperへ`commit=false`を渡し、blur/visibility/resizeは`cancelAll`/`cancelGestures`からgesture mapを先にclearするため0回である。後続の物理pointerup/clickもdetail guardにより0回のままになる。keyboard/支援技術の`detail == 0` activationはroot clickから1回確定する。
- 前回Minor: 解消を維持。TalkBackは現在offsetで可能なscroll actionだけを公開し、先頭backward・末尾forwardは非公開かつ直接実行してもfalse、可能方向は1 row移動してtrueとなる。
- 回帰: 通常tap 1回、drag 0回、固定control row、scroll range、mock/reference同期に新しい退行は見つからなかった。修正はroot clickのphysical fallback除去だけで、detail 0 activationとpointerup経路を別々に残している。記録済みbrowser証跡は通常pointer tap、keyboard click、drag、lost capture後、blur後へ到達し、focused/full suiteもPASSしている。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

`technical-reference.md` Decision 17、native/reference mock specification、manualの現行操作説明を、recent＋catalogの上3行scroll viewport、固定AZ/delete行、空recent時のcatalog先頭24件へ更新した。公開済みv0.11のrelease notesとmanual内のv0.11履歴はpage仕様のまま保持した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- `similarity-generic --language kotlin -t 0.7` はPATHに存在しないためskipした。prebuilt CLIを導入せず、変更は既存`KeyboardView`と`KeyboardLayouts`の責務内へ収めた。
- mockの親`.keyboard`は`touch-action:none`なので、viewportへCSSの`touch-action:pan-y`だけを置いても実dragはscrollしなかった。viewport pointerをcaptureして12px超で`scrollTop`を更新し、tapはpointerupで確定、dragは確定しないようにした。browserでtapの😀入力、上dragの`scrollTop=100`、drag後の入力不変を確認した。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
