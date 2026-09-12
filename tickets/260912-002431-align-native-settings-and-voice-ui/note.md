# Work Notes: 260912-002431-align-native-settings-and-voice-ui

## Status: PDH-open (Opening)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [ ] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [ ] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
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

- 事前監査ではCriticalなし。Majorは、設定画面が素の全幅control列で視覚階層が弱いことと、マニュアルの設定・音声画像が旧UIのままであること。
- native音声は候補欄50dp、固定4行、通常キー形状の左下キャンセル、partial/final、確定・取消、上/右/下のレイヤー切替と永続化を既に実装している。機能を作り直さず実機確認と必要差分へ限定する。
- 設定画面は既存項目と保存動作を維持し、現行HTML/CSSの余白・gray surface・accent・角丸をAndroidのLight/Darkへ翻訳する。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）
  - API 36.1 emulatorの412dp相当でLight/DarkのSetupを撮影し、4カード、細い6dp赤accent、横overflowなし、縦scrollを原寸確認した。
  - Robolectricで4カードとButton/Switch/EditTextの48dp最小高、BuildConfig由来version表示、既存設定の保存を確認した。
  - 音声partial/final、非選択partial、final候補tap、取消、VOICEからの方向レイヤー切替は既存のService/View回帰testで確認した。AVDには日本語音声モデルがないため、実発話のpartial/finalは未確認としてマニュアルの制約に残した。
  - 現行APKをAPI 36.1 emulatorで起動し、QWERTY左下を左フリックしてVOICEへ入り、「非対応」候補face、固定4行面、通常キー形状の左下「キャンセル」を一画面で確認した。`docs/screenshots/v0.10/voice-layer.png`へIME領域を無加工cropし、同一hashをsite assetへ配置した。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- Setupの既存controlと保存listenerを維持したまま、4セクションを角丸surfaceへまとめた。見出しはTextView全体へ背景を付けず、6dp幅のcompound drawableでsiteと同じ細い赤accentを表現した。
- Lightは`#f4f4f4` pageと白surface、Darkは既存IMEに合わせた`#1c1c1e` pageと`#2c2c2e` surfaceを使用した。Button/Switch/EditTextの最小高48dpと縦ScrollViewを維持した。
- バージョンは`BuildConfig.VERSION_NAME`のまま表示し、buildをversionCode 11 / versionName 0.10.0へ更新した。
- 現行音声面は固定50dp候補欄、4行キー高、左下キャンセル、partialの非選択表示、final候補tap確定、取消・レイヤー切替を既存testで満たしていたため、機能コードは変更していない。
- 重複検出: `similarity-generic`が環境に無いためskip。今回の新規helperはSetupActivity内のカード生成と既存子Viewへのsurface tintに限定した。
- `:app:testDebugUnitTest --tests com.masuidrive.gestureime.SetupActivitySlashCommandsTest :app:assembleDebug` は成功した。
- `scripts/test-all.sh --parallel` はfast-checksとAndroid unit/lint/APK buildの両groupが成功した。
- ローカル`site/manual.html`を390px幅で確認し、scrollWidth=innerWidth、画像読み込み欠落なし、v0.10.0表示を確認した。
- `aac16f4`: Setupのsurface・視覚階層と意味あるRobolectric回帰。
- `4a27e50`: v0.10版番号、SetupのLight/Dark画像、manual、technical reference、検証記録。

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

- decision 26へSetupの4セクション、48dp操作領域、Light/Dark追従、BuildConfig由来versionを追記した。

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
