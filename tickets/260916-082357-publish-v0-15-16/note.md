# Work Notes: 260916-082357-publish-v0-15-16

## Status: PDH-close (Publishing main)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部providerを使わないAPK・GitHub・静的Pages公開
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
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

2026-09-16: ユーザの「push deploy」を、本ticketに列挙したmain push、v0.15.16 APK Release、製品ページ公開の明示承認として扱う。直前公開版v0.15.15、次versionCode 32、GitHub repositoryがPUBLICでdefault branchがmainであることを実測した。未確定判断はない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] GitHub公開状態、Release asset数とbyte一致、APK metadata・ABI・permission、3系統test、Pages run、公開4file、412px・840px overflowを測定して記録した

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

### 実装前の仮定と測定

- 次版はv0.15.16: GitHub Release一覧でv0.15.15がLatest、公開日時2026-09-15T11:22:52Zであることを確認した。versionCodeは現行31のため32とする。
- repositoryは公開済み: `gh repo view`でvisibility `PUBLIC`、default branch `main`を確認した。
- 製品サイト公開元は同期可能: `../masuidrive.jp`はmainがorigin/mainと一致しclean。変更前の`mock.html`と`styles.css`は正本とbyte一致し、`index.html`と`manual.html`だけをv0.15.16へ更新する。
- Release assetは1個: `gesture-ime-v0.15.16.apk`だけを添付し、ZIPは作らない。

### Release準備

- `app/build.gradle.kts`をversionCode 32・versionName 0.15.16へ更新した。
- README、製品ページ、マニュアル、`docs/v0.15.16-release-notes.md`へwide絵文字中央揃え、音声最下段操作、カーソルfallbackを記録した。
- commit: `0b88bfb`（版番号・release docs）、`6c27b4f`（privateカーソル境界）、`2bc5f75`（既存音声機能説明の保持）。

### Test・artifact・公開

- release SHA `2bc5f75`に対し、sandbox外Gradle環境で`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel --connected`を実行し、fast-checks、Android unit/lint/APK、API 36.1 connected real Mozcが3/3 PASSした。sandbox内初回はGradle lock作成拒否でAndroid 2区分が1件も走らず、テスト失敗ではなく実行環境エラーとして再実行した。
- local APK `/Users/masuidrive/Develop/personal/android-kbd/app/build/outputs/apk/debug/gesture-ime-v0.15.16.apk`は38,634,828 bytes、SHA-256 `92568d409ea73c435c2de8bbd81ff5c71d13bf706fcc13d36e159a9eb1cfcc55`。package `com.masuidrive.gestureime`、versionCode 32、versionName 0.15.16、minSdk 28、targetSdk 36、arm64-v8a、RECORD_AUDIOあり、INTERNETなし、zipalign成功。API 36.1 emulatorへ導入して同versionを確認した。
- GitHub Release `v0.15.16`を公開し、assetは`gesture-ime-v0.15.16.apk`の1個だけ。再取得先`/tmp/gesture-ime-v0.15.16-download.apk`は同じ38,634,828 bytes・同SHAで`cmp`完全一致した。
- masuidrive.jp `3c0bd4c`をpushし、Pages run `35075035104`がsuccess。公開index/mock/manual/stylesは正本とbyte一致した。公開index・manualは412pxと840pxで`scrollWidth == innerWidth`、v0.15.16直リンク、indexの操作demoを確認した。

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
| 1 | privateカーソル境界 | Major | 公開4文書がterminal手動設定をprivateでも使えるように読める | 採用・解消 | `6c27b4f`で自動・手動ともprivateはDPADなし、手動設定は非privateだけと明記 |
| 2 | 音声機能説明 | Minor | 新説明追加時に既存の連続認識・途中結果等がトップから脱落 | 採用・解消 | `6c27b4f`と`2bc5f75`で空候補操作と既存5機能を同じカードへ保持 |

- Sol最終reviewは`2bc5f75`にCritical・Major・Minorなし。version・APKリンク、private境界、音声6項目、HTML構造、local assetを確認した。
- 壊していない側としてversion/APKリンクと操作mockを選び、修正前後ともv0.15.16完全一致、`site/mock.html`はreferenceとbyte一致、公開demo読み込み成功を確認した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- 配布だけのticketでruntime設計判断は変えない。元機能の契約は統合済みの`technical-reference.md` decision 7・18とwide絵文字配置記録に存在するため、追加更新不要。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

2026-09-16: 機能ticketの差分・検証結果提示後にユーザが「push deploy」と明示したため、v0.15.16のGitHub Release、製品ページ公開、このrelease ticketのclose・main pushまでを承認済みとして実行した。未対応findingと未確認ACはない。mainへの最終統合・pushをclose処理として続ける。

main `5127b94`をGitHubへpushし、public repositoryのdefault branchにv0.15.16のsource・release docs・機能修正が揃った。AC 1〜4をすべて達成したため、ticketをno-merge finalizeする。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
