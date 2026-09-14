# Work Notes: 260914-075602-restart-voice-after-silence-timeout

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
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内Android SpeechRecognizerだけを使い、外部provider/API経路はない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み（接続API 36 emulatorのinstrumentation 3/3 PASS。日本語端末内modelがないため実providerのerror 7再現はhuman reviewへ明示）
- [x] PDH-verify: ドキュメント更新の要否を確認済み（README、manual、native specを更新。PDH配布物更新ではないためpdh-update非該当）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザがクローズを明示承認した（提示後の「公開して」による承認。実機error 7は未観察のまま明記）
- [x] 無音でエラー7になっても音声入力レイヤーで次の発話を続けられるようにする
- [x] v0.15.13 APKと製品ページを公開し、ticket closeとmain統合・pushの準備を完了する

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- Why は Product Brief の6レイヤーと端末内入力へ接続している。
- AC はエラー6・7の待機継続、途中結果維持、lifecycleでの停止、他エラー非退行を観察可能な状態で定義した。
- 設計判断は無音系だけの再開とcancel可能な短い待機に限定し、out-of-scopeと依存なしを確認した。
- ユーザの直接の不具合修正依頼と一致する範囲であり、未確定の製品判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 現行実装を測り、ERROR_NO_MATCH（7）でpartialが空なら `Unavailable("音声を認識できません（error 7）")` へ遷移して認識器を破棄することを確認した。ERROR_SPEECH_TIMEOUT（6）も同じ分岐になる。
- [x] 現行のcancel・入力欄切替・IME終了はcontroller generationを無効化し、古いcallbackを破棄することを確認した。再開予約にも同じgeneration境界を適用して検証する。
- [x] 再開間隔はUI上「認識中」を保ったまま250msとし、単体テストで再開前・再開後・cancel後を時間制御して測る。端末のSpeechRecognizerが発生させる無音時間そのものはprovider依存なので固定値をACにしない。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 修正前の反例として、無音とは別分岐の「途中結果あり + NO_MATCHは候補へ昇格」「RECOGNIZER_BUSYはUnavailable」「権限・モデル不足」を含むVoiceRecognitionControllerTestとImeServiceVoiceHoldTestの29 testがPASSすることを確認した。
- 依存仮定: `onError(6|7)`を受けたSpeechRecognizerは終了済みで再利用せず、新しいrecognizerが必要。factoryの生成・support確認を通常start経路からやり直す。
- 依存仮定: 連続音声レイヤーと単発holdは同じcontrollerを使う。`continueAfterSilence`を連続レイヤーのstartだけで有効にし、単発holdのエラー終了は維持した。
- controller generationで古いcallbackを無効化し、同じgenerationに紐づく250msの再開Runnableをcancel/destroy/start時に削除する。待機中はRecognizingを通知して音声レイヤーを有効に保つ。
- 変更後、無音エラー6・7の再開、途中結果の候補化、cancel、入力欄切替、古いcallback破棄、単発hold非再開、RECOGNIZER_BUSY非再開を含む37 testがPASSした。
- similarity-genericは環境に導入されていないため重複検出をskipした。変更は既存controller内のlifecycle helperと既存testへの追加に限定した。
- 論理commit: `290fec7`（controller/service実装と回帰test）、`925b6ee`（README・manual・native/technical reference）。
- final product SHA `925b6eedb7d1c70a9b29e95d147c80e3590bdd91` で `ANDROID_HOME="$HOME/Library/Android/sdk" scripts/test-all.sh --parallel --connected` を実行し、fast-checks、unit/lint/APK、API 36 emulator instrumentationの3/3がPASSした。

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
| 1 | 再試行制御 | Minor | 即時に6/7を返し続けるproviderではキャンセルまで250msごとにrecognizerを再生成する | 採用済み仕様 | 無待機spinではなく、「キャンセルまで次の発話を待つ」というACを満たす。自動再開を6/7だけに限定し、利用者がキャンセルできる。 |

- 独立review（対象 `925b6eedb7d1c70a9b29e95d147c80e3590bdd91`）はCritical/Majorなし。AC 1〜4の実装対応とfocused regression PASSを確認した。
- 修正前後の反例はRECOGNIZER_BUSY、partialありNO_MATCH、単発holdを使用した。修正前29 test PASS、修正後は同じ経路を含む37 test PASSで出力を維持した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- Decision 21へ、partialなしのNO_MATCH/SPEECH_TIMEOUTだけ250ms後に同じeditor tokenで再開することと、cancel・他error・IME/editor終了で再開予約を破棄することを追記した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- 実機確認手順: 音声レイヤーへ入り、何も話さずエラー7が出ていた時間を超えて待つ。その後に話し、エラー表示なしで候補が出ることを確認する。続けてキャンセルし、発話しても候補や入力が復活しないことを確認する。
- API 36 emulatorは端末内日本語modelがなく実際の発話・error 7を生成できないため、この1点はユーザの実機確認をclose条件として残す。
- 2026-09-14 17:16 JST、上記の実装・検証結果、APKフルパス、実機確認手順を提示した後、ユーザが「公開して」と明示したため、v0.15.13公開とticket closeを承認したものとして進行した。
- release SHA `6420b649e7047b215d329aea840305919b6a94ce` でversionCode 29 / versionName 0.15.13をbuild。並列`test-all`は2つのGradle processが同じdesugar graphを更新してunit/lint/APK系統だけfilesystem競合した。fast-checksと接続emulator 17 testはPASSし、競合系統を単独再実行してunit/lint/APKもPASSした。
- `gesture-ime-v0.15.13.apk`をGitHub Releaseへ直接公開し、再取得した38,616,144 bytes、SHA-256 `69ac5552dcc7b865f1533dce74d77de7c99225e57681f87dc354886516eb0f4e`がlocal artifactと完全一致した。package `com.masuidrive.gestureime`、ARM64、RECORD_AUDIO、INTERNETなしを確認した。
- 製品ページはmasuidrive.jp `8aa4d19`へpushし、Pages run `34837267529`成功。公開4 core fileのbyte一致、412px・840pxの横overflowなし、実mockで左フリック→音声layer→2候補目確定→次の認識を確認した。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

- 2026-09-14: 再開間隔は0msでも実装できるが、即時ERRORを返すproviderで認識器生成が密ループになる。操作感を変えない短い250msを採用し、無音系だけに限定する。

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
