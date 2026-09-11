# Work Notes: 260910-233809-fix-keyboard-interaction-details

## Status: PDH-open (ticket drafted; implementation and reproduction not started)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [ ] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [ ] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [ ] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [ ] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [ ] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [ ] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [ ] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
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

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [ ] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

## PDH-implement. 実装ログ
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
|   |      |     |      |      |      |

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

- ユーザーは追加指示を後日まとめて作業させるため、今回は起票のみを依頼した。`./ticket.sh start` は未実行。
- 11項目はユーザー申告を起点として記録。キー間隔と特殊キーのアニメーションは下記の比較調査を実施し、その他は未再現のまま着手後に個別確認する。
- 「小。」は現UIの「小゛゜」を指す推定、ESCはキーイベント送信の意図という推定。着手時に元モック・正本仕様と対象アプリで確認する。
- 追加申告はキートップ文字が正本CSSに合わないことと、フリック中の背景減光によるちらつき・視認性低下。原CSSとの表示差と減光対象の範囲は未再現で、着手時に確認する。
- 候補未表示時の案内文削除とキータップ時の触覚フィードバックを追加。実際の案内文・実装位置と、振動の強度・タイミング・対象範囲は着手時に既存実装とAndroid標準設定を照合する。

## Comparison probe (2026-09-11)

- 実機ではなくAndroid emulatorを使用。1080×2400px / density 420（411.4dp）、font scale 1.0、animator duration scale 1。QWERTY静止画は `docs/screenshots/qwerty-spacing-412.png` で、内容を原寸確認済み。正本CSSの実ブラウザ画像は `evidence/css-mock-qwerty-412.png`（viewport 412×915）。
- 画像観測: nativeは各行・各キーの間隔がほぼ均等で狭い。正本CSSは横のキー間より行間が広く、下段のSpace/Enterも異なる幅と小さな文字で見える。フリック中の瞬間は確実な静止撮影ができていないため、画像で再現済みとはしない。
- code由来: 正本CSSのQWERTYはキー高45px、行間10px、左右padding各3pxで視覚上の横隙間6px。412px実測でq hitboxはx=4 / w=40.398、Spaceはx=83.164 / w=217.922、Enterはx=301.086 / w=106.914。Space主文字16px、Enter主文字15px。
- code由来: nativeは412dp時の行高45dpを4等分し、各hit targetを上下左右2dp insetするため、横隙間・行間とも4dp。通常文字は22spで、Space/Enterにも専用の16sp/15sp設定がない。これが正本CSSとの差で、AC 6/10の着手時比較基準になる。
- code由来: nativeの補助ラベル拡大条件はQWERTYの `KeyKind.CHARACTER` の上下方向だけで、Enter/PasteとSpaceは対象外。ユーザーが具体化した特殊キーだけアニメーションしない差を確認した。
- code由来: 下段の幅比は正本CSSが1.45:4.2:2（約19%:55%:26%）、nativeが2:6:2（20%:60%:20%）。画像でEnterが狭く見えた観測と一致する。
- code由来: QWERTY Backspaceはnativeでも正本でも行幅の5%相当。nativeはcenter labelなし・下方向に「BS」を持つが、非操作時のsecondary描画対象が文字キー/Enter/Spaceだけなので静止時は空欄になる。正本CSSは副ラベルとして「⌫」を表示する。
- 「あん」相対配置の画像観測: 正本CSSでは「あ」が小さな主文字として左寄り、「ん」がさらに小さい灰色の副文字として右下へ分離して見える。nativeでは「あ」が大きく、「ん」が近すぎて下側で重なって見える。位置関係と文字サイズは一致していない。
- 「あん」相対配置のcode由来: 正本CSSはspecial主文字16pxを基準に「あ」を `translateX(-3px)`・z-index 1、絶対配置の「ん」を75%（12px）、`left:50%; top:50%; transform:translate(2px,-35%)`・z-index 0・opacity .72とする。nativeは主文字22spを中心から-3dp、副文字16.5spを中心から+7dp・baseline+4dpへcenter揃えで主文字の後に不透明な灰色として描くため、文字中心間が10dpしかなく重なりやすく、重なり順と濃さも異なる。双方とも「あ」が左・「ん」が右下という大枠は近いが、位置・サイズ・重なり・濃さは一致しない。AC 6の主副ラベル配置差として修正対象に含める。
- 追加申告: 日本語レイヤーの候補エリア下端とキー最上段の余白が狭く、他の間隔と揃って見えない。AC 10へ統合し、具体的なpx/dp値は指定せず、着手時に正本CSS・他レイヤーとの比較で決める。
- 追加申告: 日本語変換中のEnterは「確定」表示とし、tapで現在候補を確定、上スワイプで無変換、左スワイプでカタカナを選ぶ。この状態では補助ラベルを表示しない。無変換・カタカナが即時確定か変換継続かは未指定で、着手時の確認事項とする。非変換時のEnter/Pasteは維持する。
- 追加申告: 通常英字、数字・記号、Enter/Paste、Spaceなどの補助ラベルについて、位置・大きさ・主ラベルとの相対位置と間隔を正本CSSへ合わせる。通常表示はAC 6、アニメ中はAC 3で扱い、変換中「確定」キーの補助ラベルなしは優先する例外として維持する。具体値は着手時に原CSSを照合する。
- source確認: C/Aキーはpointerが載っている間だけselected色になる。指を離した後の `pendingModifier` 状態は表示文字をC/Aへ変えるが、背景色条件には含まれず通常のdark色のまま。正本CSSはpending時の `.mode-on` にselected背景を使う。現行one-shotは維持し、待機色を揃える案はユーザー質問由来の課題候補として着手前確認に残す。
- 明示変更: QWERTY Backspaceはtapで1文字削除、上下左右swipeは未割当へ変更する。従来の下フリック削除より新指示を優先し、未指定の長押し動作は変更しない。
- 明示変更: 日本語の現「小゛゜」キーは表示ラベルを「小」だけにする。濁点・半濁点・小文字変換機能と、AC 2のフリック操作調整は維持する。
- 訂正: QWERTY Backspaceの全方向swipe未割当という直前指定は撤回され、tap=1文字削除、下swipe=ESC、上・左・右swipe=未割当へ更新された。主BSラベルは表示するがESCを含む補助ラベルは表示しない。記号レイヤーのバッククォートをESCへ変えるAC 1は別要件として維持し、長押しは未指定のまま。
- 実装default: 無変換は原読みのひらがな、カタカナは原読みをカタカナへ変換したpreviewへ切り替え、Enter「確定」tapで挿入する。ユーザーへ質問後に回答がなかったためrootがdefault続行を明示したもので、個別承認済みとは記録しない。通常かな再入力、BS、候補Spaceで古いpreviewを残さない回帰を確認する。
