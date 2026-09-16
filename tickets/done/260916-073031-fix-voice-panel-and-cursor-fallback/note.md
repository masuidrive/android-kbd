# Work Notes: 260916-073031-fix-voice-panel-and-cursor-fallback

## Status: PDH-human-review (Awaiting approval)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部providerを利用しない端末内IMEの変更
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

## PDH-ticket-review. Ticket contract check
2026-09-16: Product Briefの6レイヤー、Spaceカーソル操作、ターミナル対応に接続する。ユーザ報告の「音声入力レイヤーが空で戻れない」を再現可能なlayout contractへ落とし、通常欄と非標準欄を分ける要望をInputConnection能力ベースの観察可能なACにした。外部通信や新規product判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] panel下端と最下段6 slotのtap境界、412px/840pxの再配置、通常・能力不足・private・手動terminalのInputConnection出力を測定し、focused testと実Androidで結果を記録した

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

### 実装前の仮定と測定

- 音声panelは最下段controlを覆っている: `updateVoicePanelLayout`が候補欄50dpと4行全体を高さに使い、`VoicePanelView`が不透明背景と全面`ScrollView`を持つことを確認した。technical-reference.mdは候補欄と上3行だけを覆う契約であり実装が不一致。
- 通常欄とターミナルをEditorInfoだけで完全識別できる: Android標準に該当属性はなく成立しない。既存controllerが利用する`getExtractedText`と`setSelection`の成否を能力判定に使う。
- 反例（変更前）: 抽出可能な通常欄では`setSelection`だけが呼ばれ、DPAD eventは0件。変更後も同じfixtureで維持する。

### 実装・focused test

- `KeyboardView`に候補欄下の上3行分だけを表すvoice panel高さを追加し、`ImeService`の初期配置とwidth同期へ適用した。Robolectricでは400px/840pxで最後の6キーの先頭よりpanel下端が上にあること、rootから送ったCancelの実`MotionEvent`が露出した`KeyboardView`へ届いてKANAへ戻ることを確認した。
- `TextInputController`は非privateかつterminal設定OFFで`getExtractedText`がnull、または`setSelection`がfalseの時だけ方向DPAD pair / HOME・END pairへfallbackする。privateは能力不足時にも送信せず、terminal設定ONの既存強制DPADは維持した。`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk ./gradlew testDebugUnitTest --tests com.masuidrive.gestureime.TextInputControllerTest --tests com.masuidrive.gestureime.ImeServiceVoiceLifecycleTest` はPASS。
- 重複検出 skip: `similarity-generic`がPATH上に存在せず、Kotlinファイル用の指定CLIを実行できない環境制約。
- 実装commit: `6070912`（cursor fallback）、`d591bf4`（voice panel layout）、`1fc219a`（panel/tap境界と再配置のreview修正）。

### Review finding 修正

- 反例（修正前）: `voicePanelEndsAtBottomControlTapBoundsAndRoutesTheirUpperEdges`を先に追加して実行するとFAILした。wide Large（840px、density 1）で第4行Cancelの`tapBounds.top`はroot Y=239pxだが、panel下端はface topの244pxで、5pxのタッチ領域を覆っていた。
- 修正後: `KEY_ROW_GAP_DP`をhit targetとvoice panel高さで共有し、panel下端を`tapBounds.top`（row gap半分だけface topより上）に一致させた。`updateVoicePanelLayout`はdesired heightと一致すればlayoutParams再代入・`requestLayout()`を行わない。same-width反射呼出で`isLayoutRequested == false`を確認した。
- `ImeServiceVoiceLifecycleTest`はwide Large初回840px、412pxへの切替、840px再適用で各panel高さを確認し、最下段6 slotのtapBounds上端+1pxへroot経由`MotionEvent`を送った。Cancel・句読点・Space・削除・Enterは各`KeyAction`を発火し、status slotはactionなしでpanel境界外であることを確認した。修正後の`ImeServiceVoiceLifecycleTest`（15 tests）と`KeyboardViewTest`（55 tests）はPASS。

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
| 1 | 音声panel境界 | Major | panel下端が第4行face上端で、tap領域の上5pxを覆う | 採用・解消 | `1fc219a`でpanel下端を共有row gap由来の`tapBounds.top`へ一致させ、最下段6 slotの上端実MotionEventを回帰test化 |
| 2 | layout更新 | Minor | 同じ高さでもlayoutParams再代入とrequestLayoutが走る | 採用・解消 | `1fc219a`で高さ不変時をno-opにし、`isLayoutRequested == false`を検証 |
| 3 | wide初期配置 | Minor | Large設定の840px初期生成が未検証 | 採用・解消 | `1fc219a`で840→412→840のpanel高さとtap境界を検証 |

### PDH-review-2

- Sol独立再reviewでCritical・Major・Minorなし。上記3件の解消、AC 1〜5、ticket不変、論理commit分割を確認した。

## PDH-verify. 検証結果

- `ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --connected`を順次実行し、fast-checks、Android unit・lint・APK build、API 36.1 emulator connected 20件が3/3 PASSした。
- 並列実行では既存`ImeHideBarTest`に順序依存の失敗が1件出たが、単独実行と最終の全順次suiteではPASSした。このticketのproduction/test差分に同classの変更はない。
- API 36.1 emulatorへ最新APKを導入し、音声認識結果がない「マイクの許可が必要です」状態でも最下段6 slotが表示され、Cancel tapで直前の文字layerへ戻ることを実InputMethodServiceで確認した。
- `TextInputControllerTest`で通常欄は`setSelection`だけ、抽出不能・選択拒否の非private欄は方向DPAD down/up、private欄はDPADなし、terminal設定ONは強制DPADを確認した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- Design decision 7を、抽出不能時のno-opから、非private入力先だけのInputConnection能力ベースDPAD fallbackへ更新した。音声panelが上3行だけを覆う契約はdecision 18に既に記録済みで変更不要。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

### 確認手順

1. 通常の入力欄で左下layerキーを左へフリックし、音声layerへ入る。
2. 認識結果が空、権限未許可、または端末非対応でも、最下段にCancel・状態・句読点・Space・削除・Enterが見えることを確認する。
3. Cancelをtapし、直前の文字layerへ戻ることを確認する。
4. 通常欄ではSpaceフリックでカーソルが直接移動することを確認する。xterm.jsなどが成功応答だけ返して移動を無視する場合は、設定のターミナル対応をONにして矢印キー送信へ強制する。

2026-09-16: 修正内容と検証結果の提示後、ユーザが「push deploy」と指示したため、本ticketのclose・main統合・pushと次版公開を明示承認したものとして記録した。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
