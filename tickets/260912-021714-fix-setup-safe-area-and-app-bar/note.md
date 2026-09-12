# Work Notes: 260912-021714-fix-setup-safe-area-and-app-bar

## Status: PDH-ticket-human-review (User implementation instruction received)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み - skip: SetupActivityは端末外provider/APIを呼ばない。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [-] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した - skip: reviewでCritical/Majorがなく修正attemptなし。
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み - API 36 arm64 AVDのLight/Dark portraitとDark landscapeを実画面・UI hierarchyで確認。物理Foldは未接続。
- [x] PDH-verify: ドキュメント更新の要否を確認済み（README、manual、device verificationを更新）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->
- Product BriefのAndroid native設定導線とLight/Dark対応に接続する。
- 5件のACから、safe area、固定トップアプリバー、戻る操作、system iconの判読性、既存設定の非退行を復元できる。
- consumer surface: `SetupActivity`、theme colors/styles、Setup Robolectric、API 35/36の実画面、manual画像。
- ユーザから「ステータスバーにめり込んでる」「トップナビバー的なのは入れたら？他のアプリの設定画面みたいに」と実装指示済み。
## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）
  - appはtarget SDK 36で、`SetupActivity`のroot `ScrollView`とcontentにはsystem bar/display cutout inset listenerがなく、24dp固定paddingだけであることを確認した。
  - Android 16ではtarget SDK 36 appのedge-to-edge opt-outがないため、View側でinsetを消費せずpaddingへ反映する必要がある。
  - 実装後は合成insetの再配信でpaddingが累積しないことをunit testし、API 36.1 AVDの縦・横、Light/Darkでstatus/navigation barとの非重なりとicon contrastを観察する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->
- 実装前の仮定: `WindowCompat.setDecorFitsSystemWindows(window, false)`でtarget SDK 35/36のedge-to-edgeを明示し、root listenerが`systemBars`と`displayCutout`の最大辺を固定app barとscroll終端へ配分できる。Robolectricでは同じinsetを二度dispatchしてpaddingとbar高が変わらないことを測り、API 36.1 AVDではLight/Darkの実画面で確認する。
- `similarity-generic`はリポジトリに存在しないためskipした。外部provider/APIはこのActivityの変更に存在しないため実API 200確認は該当なしとして記録する。
- `SetupActivity`をrootの固定app barとその下だけを重み付きで占める`ScrollView`へ分離した。`systemBars`/`displayCutout`の各辺の最大値をbase paddingから再計算し、topは56dp bar、bottomはscroll content終端へ反映する。
- 戻る操作は48dp `ImageButton`、ローカルvector `ic_arrow_back`、platform ripple、content description `戻る`で実装した。Setup専用themeでtransparent barsを指定し、runtimeでもLight/Dark status/navigation icon appearanceを明示した。
- Robolectric: `SetupActivitySafeAreaTest`はfixed hierarchy、48dp back action、system back、入力テスト/ライセンス導線、cutout優先の左右topとbottom inset再配信の非累積を検証した。既存`SetupActivitySlashCommandsTest`はswitchとslash command保存を継続して通過した。
- focused: `./gradlew :app:testDebugUnitTest --tests 'com.masuidrive.gestureime.SetupActivitySlashCommandsTest' --tests 'com.masuidrive.gestureime.SetupActivitySafeAreaTest' :app:assembleDebug` PASS。
- full: `scripts/test-all.sh --parallel` は fast-checks と Android unit/lint/APK の2/2 PASS。
- API 36 arm64 AVD (`emulator-5554`): final APKでportrait Light/DarkおよびDark landscapeを起動し、UI hierarchyの`戻る`と`masuidrive-kbd 設定`、status/navigation safe area、scroll終端のversion/licenseを確認した。physical Fold7は未接続。
- Commit: `d121739` `[260912-021714-fix-setup-safe-area-and-app-bar] feat(setup): add safe-area app bar`。

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

- 対象commit `f8e926452605954ff702aa38c4f84f80f43a1c45`。独立reviewはCritical/Majorなし。Setup focused test 6/6とdiff checkがPASSした。

## PDH-verify. AC裏取り

- 独立VerifierがAC 1〜5をVERIFIED。API 35 Robolectricのinset/hierarchy/back 3/3、既存設定保存 3/3、full suite 2/2がPASSした。
- API 36 arm64 AVDのLight/Dark portraitとDark landscape 2画像を確認。UI hierarchyで固定app barとScrollViewがroot直下の兄弟であること、scroll終端のversion/licenseがbottom inset上に収まることを確認した。

### AC reader

- AC 1〜5はrecoverableで相互矛盾なし。safe area、固定bar、戻る、theme icon、既存設定非退行をそれぞれRobolectricとAVD観察で裏取りする。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->
- `technical-reference.md`へSetupのedge-to-edge、safe area配分、戻るicon、冪等padding、system bar appearanceを追記した。README、manualのLight/Dark画像、device verificationも同じ実画面へ更新した。
- `technical-reference.md`へSetup専用edge-to-edge theme、固定56dp app bar、systemBars/displayCutout inset、Light/Dark icon appearanceを追記し、実装と突合した。
## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->
- safe area修正、トップバー、戻る操作、Light/Dark画像、独立review/verifyまで完了。後続の高さ設定を同じ画面へ追加したAPKで利用者確認を依頼する。
## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
