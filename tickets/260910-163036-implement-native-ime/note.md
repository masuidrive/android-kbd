# Work Notes: 260910-163036-implement-native-ime

## Status: PDH-open (implementation and external placement complete; human review pending)

## Checklist
- [x] ユーザー依頼: 動作するv1のスクリーンショット付きマニュアルページを作成する（2026-09-11追加）。
- [x] ユーザー依頼: 製品紹介ページを作成し、操作できるモックも活用する（2026-09-11追加）。
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
- [x] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み（Hanger Sitesへ所有者限定・1週間期限で公開し、公開9ファイルのSHA-256一致を確認）
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (412dp/840dp、回転、font scale、reduced motion、全5レイヤーを実エミュレータで観察し `docs/device-verification.md` に記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した
- [x] ユーザ依頼: github.com/masuidrive/pdh の Codex 標準セットを導入する
- [x] ユーザ依頼: Hangar Sites の最終仕様を正本として Android native IME を実装する
- [x] ユーザ依頼: Director は Astra、実装・機械検証は Sol/Terra を使う分担を設定する
- [x] ユーザ追加: 複数資料では Hangar Sites の仕様を優先する
- [x] ユーザ追加: 動く v1 ができるまで作業を継続する
- [x] ユーザ追加: 節目の経過を `[yyyy/mm/dd HH:MM] 内容` 形式で `progress.md` に残す（Director が一元追記）

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- ユーザは `--full-auto` と実装を明示依頼済み。依頼済み範囲の実装許可を同内容の確認待ちへ置換しない。
- 上記は新しく書いた AC 文言の個別承認を得たという意味ではない。AC 読み手と Director の contract check を継続する。
- Sites 仕様を正本とし、個人辞書・ログ取り込みは基礎入力後の拡張として out-of-scope にした。

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
| 1 | 対称関係 / 入力キュー | Major | 変換前Space/Enter・確定直後入力が非同期競合で欠ける可能性 | 解消 | Service操作を直列化し、実IMEのSpace巡回・Enter確定と連続更新を検証 |
| 2 | session状態 | Major | 待機中の入力操作が入力欄切替後のConnectionへ流れる可能性 | 解消 | editor世代tokenとsuspend再開後guard、slow reset回帰testを追加 |
| 3 | doc sweep / 再現ビルド | Major | Mozcスクリプトにproto jar生成欠落と固定Bazel出力パス | 解消 | 生成手順をスクリプト化し、同一artifact再生成を確認 |
| 4 | 配布依存 | Major | Mozc静的依存のthird-party notice不足 | 解消 | APK内assetsへ依存ライセンス一式を同梱しSetupから参照可能にした |
| 7 | Why / 連続入力 | Major | 変換待ち直列化により入力応答が遅延する可能性 | 解消 | 辞書配置済みの別instrumentation processで初回update 68.093ms、10 updates合計261.993ms・最大54.369ms。持続stallは再現せず。初回asset copyやUI全体の時間ではない |
| 9 | 状態対称性 | Major | 未選択suggestionをindex0選択中と報告して最初Spaceと表示が不整合 | 採用 | e5e6f01独立Sol。初期未選択を-1で一貫表示する |
| 10 | 失敗経路 | Major | native SESSION_FAILURE未検出と候補文字への成功fallback | 採用 | e5e6f01独立Sol。error検出と実resultのみ確定 |
| 12 | 修正起因 / 部分候補 | Major | モバイルautoPartialSuggestionのprefix候補を全reading確定するとsuffix欠落 | 解消 | 5488ce6で部分候補を無効化し、Space前後のfull-reading確定を含むconnected test 5件PASS |
| 13 | Surface / fontScale | Major | font_scale1.3で固定キー内の主/副ラベルが重なる | 解消 | 2df6266とbbe40aaでキーと候補stripを枠内fit。1.3/2.0を実画面で再撮影し可読性確認 |
| 11 | 空入力 | Major | update空文字で前回state.readingを継承 | 採用 | e5e6f01独立Sol。空preeditは空readingへ |
| 8 | Why / accessibility | Major | キーがvirtual accessibility子ノードでなくTalkBackのfocus移動で選択不可 | 採用 | 2812ba3独立Terra lens1。キー単位ノードと動作・focusを公開する |
| 6 | 対称関係 / 複数文節 | Critical | Mozc SUBMIT_CANDIDATEが先頭文節のみresultを返し、残りpreeditを現engineが捨てる | 採用 | upstream session.ccと既存testで確認。残文節SUBMITとresult連結、長文実engineテストを追加 |
| 5 | accessibility | Minor | C/A静止表示と読み上げの上下が異なる | 棄却 | 静止表示C上/A下、操作上Alt/下Ctrlという原仕様を読み上げが正しく表す。UI独立レビュー対象e5e6f01、Terra、Critical/Majorなし |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md` を最終構成と突合し、IME/KeyboardView/TextInputController/Mozc JNI、editor・候補世代guard、offline/privacy、412dp/840dp検証条件が現在の実装と一致することを確認した。

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

- 実装・ローカルページ・完全ZIPとHanger Sitesへの外部配置は完成。
- `/tmp/gesture-ime-v1-complete.zip` は全9配布ファイルを含み、SHA-256は `9991f96548f146972cd01e83103c3064116da2c4e2637f4e3bdcb3260e99cc31`。
- Hanger Sitesは `https://amykwzak.aboutme.style/` へ所有者限定・2026-09-18 08:02 JST期限で公開済み。人間レビューと全AC独立verifyは未完のためticketをcloseしない。

## Director scope audit (2026-09-11)
- 独立AC読み手はAC1/2/3/6の外部仕様依存を指摘。親が元の仕様の操作例を具体化した。AC5のテスト代用条件はプロセス要件として除外し、実Mozc変換要件を維持した。
- 続く追加書換えコマンドはauto-reviewが未承認範囲変更として拒否し、未実行。読み取り専用のSol監査結果は `/tmp/android-kbd-ac-reader/scope-audit.md`。元のSites仕様を最終基準として継続することは元依頼と一致との結論。要約ACだけで原文の機能を省略しない。
- 濁点/小文字は共有会話の初期成果物説明で明示された既存機能。かな割当の実装詳細は日本語12キー要件に基づく。個人辞書・ログ取り込みはv1の基礎入力後とし、progressと会話で明示した。
- 受入基準は `docs/reference/sites-native-spec.txt` と共有会話の最終修正を含む。派生ACに書かれていない配列寸法・キー表示・ジェスチャー中断・アニメーション・TalkBackを完成判定から落とさない。

### Scope audit evidence resolved
- Hanger Sites connectorから原HTMLを損失なく取得: `docs/reference/mock-source.html` (SHA-256 e0711d7a2775f1a018c334033c06eb2fdf598ddc22f07d063162ec16e7d83a8c)。267〜298行にかな配列、や/わの5方向、modifierCyclesによる濁小変換が明示されている。先のscope監査で供給資料不足だった項目は新機能追加ではなく既存モックの再現と確認できた。
- 原Markdownは `docs/reference/android-native-implementation.md` / `docs/reference/html-mock-implementation.md`。ブラウザ抽出テキストだけでなく、これら原文とmock-sourceを詳細の照合に使う。

### ユーザー追加依頼: v1案内ページ
- v1完了後にスクリーンショット付きマニュアルページ・製品紹介ページを作り、既存モックも活用する依頼を受領。native ticketのHTMLモック製品化対象外とは別に、案内と操作デモの制作を明示依頼された。
- Solが `site/` へ静的ページを制作し、Androidエミュレーターで撮影した画像と、正本モックの操作部分を使う。デモがMozc変換を行うという表現はしない。
- 最終成果はHanger Sitesへ所有者限定で配置し、元の参照サイトは維持する。APKと各配信ファイルのSHA-256を照合する。

### Commit coordination note
- 2812ba3: Android本体・Gradleの論理commit。486a666: a11y修正に加え、別担当が共有indexへstage済みのライセンス・Mozc手順等が同梱された。すべて依頼範囲の成果であり、履歴の書換えは行わない。以後stage/commit直列化を全担当へ通知した。
- similarity-generic はこの環境に未提供。実行済みとは記録せず、独立Sol/Terraのsource reviewと実consumer検証を行う。

### Native correction check
- 独立Solが224c4d5を修正確認し、採用4件のコード上の修正を確認、修正起因Critical/Majorなし。ただしdevice未再検証として保留。
- 続く実Androidテスト3件は空入力1件PASS、Space変換2件FAIL（Mozc returned no candidates）。unitや静的reviewの成功で上書きせず、Terraがadbを引継いで原因調査・修正中。
- ブラウザ観察: 専用demoを412×915で操作し、実ポインターでQWERTY q→日本語切替→あの2文字入力を確認。元のsticky配置問題は独立ページ化で解消。

### Final native verification checkpoint
- e46ad39: Android専用all_candidate_words出力とmobile request/KANA初期化へ対応。rootがconnected XMLのtests3/failures0/errors0/skipped0を確認。
- scripts/test-all.sh: fast-check5、unit31/fail0/error0/skip0、lint error0、assemble成功。lintは依存更新案内などWarning15件を含む。記録docs/build-verification.md、記録commit0461d6e。
- 独立Terraはe46ad39のa11y/lifecycle/候補snapshot/scroll修正を確認、修正起因Critical/Majorなし。latency指摘は測定待ちを維持する。
- 入力モードと実candidate出力の問題は、コードレビューだけで決着せず実device responseの監査へ切り替えて解消した。

### Final v1 checkpoint
- bbe40aa: 412dp/840dp、全5レイヤー、実Mozc候補と確定、private Paste抑制、回転、reduced motion、font scale 1.3/2.0を実エミュレータで確認。記録は `docs/device-verification.md`。
- 最終ローカル検証はfast-check 5件、unit 32件、connected 5件、lint、assembleがすべてPASS。APK SHA-256は `87ccb6734b22b6bb940995c169fcc4c36457fd95f8c583420ae84ade0e554d0b`。記録は `docs/build-verification.md`。
- 製品紹介、スクリーンショット付きマニュアル、操作デモは完成。`http://127.0.0.1:8765/` は2026-09-11 03:02 JSTにHTTP 200を確認。
- Hanger Sitesは `https://amykwzak.aboutme.style/` へ所有者限定・1週間期限で公開済み。APK単体ZIP（SHA-256 `1efdb437c9c1f44eeea405a15ae79ae84b350fbfb6208b95ca1bbdb0f17816bb`）を配布し、公開9ファイルのSHA-256一致とZIP内APKの最終SHA-256一致を確認した。既存の参照サイトは変更していない。
