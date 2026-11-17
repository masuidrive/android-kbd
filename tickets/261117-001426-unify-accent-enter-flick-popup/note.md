# Work Notes: 261117-001426-unify-accent-enter-flick-popup

## Status: PDH-human-review (Opening)

## Checklist
- [x] 利用者依頼: 小キーのフリック候補を指で選べる方向表示として見えるようにする。
- [x] 利用者依頼: 日本語Enter左フリックをTabにする。
- [x] 利用者依頼: Enterの候補表示を小キーと同じ方式にし、関連キーの処理と表示を共通定義へ整理する。
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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み - skip: オフラインIMEのキー操作であり外部providerを呼ばない
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
依頼文は小キーの候補表示、Enter左Tab、Enterの共通候補表示と周辺キーの整理を明示しており、AC1〜4の実装前承認として扱う。WhyはProduct Briefのジェスチャー体系へ接続。影響レイヤー: Android app、IME service、custom view UI、unit tests、instrumentation/device tests、docs。Mozc JNIは無関係。外部surfaceは実IME、操作モック、マニュアル。変換中Enterの既存割当は維持する。

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
- [x] 現行コードを確認: `KeyboardLayouts.accent` は中央・左・上・右を定義するが `KeyboardView.syncPopup` は ACCENT を表示対象にせず、`KeyboardPopupController.popupKind` も文字拡大表示へ送る。Enterは上paste・下C-j、変換中は左・上カタカナを定義するが同じポップアップ対象ではない。`KeyboardView.dispatch` は `KeyAction.Tab` を物理キーとして既に送れる。モックは小キーに5方向flickを持ち、Enterは別のnavigation経路。

## PDH-implement. 実装ログ
`fabbe09`: KeySpecの方向popup判定を小・Enter・音声記号で共有し、Enter左Tabを割当。実MotionEventとpopupラベルのunit testを追加。
`00ddecf`: 操作モックとマニュアルを同じ方向割当へ更新。モックと保存原本のbyte一致を確認。
`560209a`: connected Enter検証の旧左Enter期待値を左Tabへ修正。
`44970af`: Enterの旧キー内拡大選択表示を除去し、十字popupを唯一の選択表示へ統一。壊していないEnter中央/上/下、変換時中央/左/上の期待値は維持。
`9661b5a`: 旧Enter拡大表示を現行仕様としていた3つの視覚資料を十字popup仕様に更新。
`3580b83`: 412dp相当エミュレーターでEnter右空欄セルの端が約10px切れることを発見。十字popupのshadowを含む全体に画面内4dp余白を設ける位置計算へ修正し、対象unit testを追加。

`9661b5a` で `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel --connected` を実行。fast-checks と Android unit/lint/APK/connected はともにPASS、connectedは36件全通過。右端修正後の最終製品SHA `3580b83` でも同じ全体スイートを再実行し、両区分PASS、connected36件全通過。以前の `00ddecf` での実行はconnected 2件失敗（Enter左の旧期待値、音声モデルcallback timeout）。前者はテスト修正、後者は後続実行で通過。retry-passとしてこの経緯を隠さない。

ブラウザ実操作: 412pxで小popup `[小,゛,゛,゜]`、Enter popup `[Enter,Tab,paste,C-j]`、Enter左ドラッグで入力欄へタブ1文字。840pxで横はみ出しなし、dual layoutを確認。旧キー内拡大表示除去後も412pxで十字popupとTab選択を再確認。

Android実IMEの412dp相当エミュレーター観察: 小キー十字popupの中央/左/上/右の候補と左選択を視認（`/private/tmp/mdkbd-ime-412-small-down.png`, `/private/tmp/mdkbd-ime-412-small-left.png`）。Enter十字popupの中央/左/上/下候補と左選択を視認し、左UP後に通常入力欄から数字入力欄へフォーカスが一段移った（`/private/tmp/mdkbd-ime-412-enter-down.png`, `/private/tmp/mdkbd-ime-412-enter-left.png`, `/private/tmp/mdkbd-tab-effect.xml`）。この観察で右端の約10pxクリップを発見し `3580b83` で修正。修正版 `/private/tmp/mdkbd-ime-412-enter-down-fixed.png` では右端の角丸・影を含めて画面内に表示し、約10pxの実px余白を確認。

840dp相当は同一エミュレーターの密度overrideで模擬し、観察後densityをreset。Dual Flickの小キー、通常Enter、変換中Enterの十字候補が全表示され、端クリップや不自然な重なりなし（`/private/tmp/mdkbd-ime-840-small-down-fixed4.png`, `/private/tmp/mdkbd-ime-840-enter-normal-down-fixed.png`, `/private/tmp/mdkbd-ime-840-enter-conversion-down-fixed.png`）。物理Fold/タブレットは未観察。小キー方向の最終文字反映は手動座標観察では安定して再証明できなかったため、surface証拠は候補表示と方向強調まで。文字反映は本番KeyboardViewの実MotionEventテストで裏付ける。
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

## PDH-review. 品質検証結果
ReviewerのMajor 1: Enterに旧キー内Paste/C-j拡大アニメーションが残り、十字popupと二重表示。`44970af` でnativeとmockから除去。Major 2: 視覚資料3つが旧アニメーションを現行仕様としていた。`9661b5a` で更新し、対象3資料の再reviewはPASS。非採用findingなし。Criticalなし。
`3580b83` の端余白修正差分も独立review PASS、Critical/Majorなし。
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
`technical-reference.md` のEnter表示説明を十字popupへ変更。`docs/reference/sites-native-spec.txt`、`docs/visual-reference-analysis.md`、`docs/visual-comparison/dynamic-reference.md`、`docs/visual-comparison/frame-fixture.html`、`site/manual.html` も現仕様へ更新。`site/mock.html` と `docs/reference/mock-source.html` はbyte一致。
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
実装・AC裏取り・全体テスト・412dp/840dp相当エミュレーター観察を提示して、利用者による表示と操作の確認を依頼する。利用者の明示close承認まではticketを閉じない。公開前のAPKは `app/build/outputs/apk/debug/` に生成済み。物理端末確認と公開releaseは未実施。
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
