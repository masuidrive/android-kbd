# Work Notes: 260914-013111-highlight-voice-candidate-differences

## Status: PDH-close (v0.15.12 publication in progress)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内SpeechRecognizerの返却済み文字列に対する表示処理で、外部provider経路を変更しない。
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

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 現行nativeは各候補を同じ通常書体・`keyboard_text`色で描画し、候補間の差分を計算していない。
- [x] Android `SpeechRecognizer.RESULTS_RECOGNITION`の各全文候補は空欄除去と完全一致の重複排除だけを経て表示される。
- [x] 候補タップは行のindexを保持し、選んだ1件だけをcommitして認識を再開する既存testがある。
- [x] 操作mockも各候補を単純な`textContent`で描画しており、差分表示は未実装。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->
- [2026/09/14 10:31 JST] ユーザ提示例では「を／が」と「ほ／欲」だけが異なるが、現行UIは全候補を同じ書体で描画するため判別しづらいことを確認。候補全文とindex選択は変えず、異なる文字だけを太字・アクセント色で示す方針をユーザが承認した。
- [2026/09/14 10:35 JST] 未確認仮定を調査。候補数1または全文一致では強調不要、挿入・削除を含む近似候補では文字位置の単純比較が共通部分を誤判定するため、各候補pairの最長共通部分列に含まれないcode pointを差分とする。候補の原文、順序、選択indexは変更しない。
- [2026/09/14 10:37 JST] Nativeの候補文字列へ差分spanを適用し、提示された3候補で各行の「を／が」「ほ／欲」だけがaccent色・太字になるunit testを追加。既存の2行目tapが2行目全文だけをcommitして認識を再開するtestとともにPASSした。
- [2026/09/14 10:39 JST] 操作mockへ同じcode point比較と差分spanを追加し、デモ候補をユーザ提示例へ更新。`site/mock.html`と`docs/reference/mock-source.html`をbyte一致させた。
- [2026/09/14 10:50 JST] 初回reviewでnative/mockの重複文字tie-break不一致と、長文・多数候補での比較負荷がMajorとして見つかった。両surfaceを共通のprefix/suffix除外＋決定的LCSへ揃え、8候補または差分matrix 262,144 cellを超える場合は全文とtapを維持したまま装飾だけを省略する。`aa`/`ab`、600文字の全面差分、挿入、補助平面Unicodeを回帰testへ追加し、Light/Darkの差分色も通常文字で4.5:1を超える専用色へ変更した。
- [2026/09/14 10:55 JST] Android instrumentationで実際の`VoicePanelView`を生成し、提示された3候補が順に`を・ほ`、`を・欲`、`が・欲`だけを専用色・太字にすることを確認した。
- [2026/09/14 10:56 JST] 最終候補で`scripts/test-all.sh --parallel`のfast-checks・全unit・lint・APK buildが2/2 PASSし、API 36.1 AVDの`connectedDebugAndroidTest`は新規実View検証を含む17/17 PASSした。
- [2026/09/14 12:25 JST] v0.15.12・versionCode 28の最終SHA `8d7dffd`でfast-checks・全unit・lint・APK buildが2/2、API 36.1 AVD instrumentationが17/17 PASSした。APKは`com.masuidrive.gestureime`、ARM64、`RECORD_AUDIO`のみで`INTERNET`権限なし。
- 論理commit: `3498874` native/mock差分表示、`c3447f4` manual、`77072ec` parity・負荷・contrast修正、`370a133` flat matrix上限修正。instrumentationと最終検証記録は最終ticket commitへまとめる。
- 重複検出 skip: `similarity-generic`が開発環境へinstallされていないため。変更はNative内の差分計算1実装、mock内の対応1実装で、異なるruntime間の意図的な同等処理である。

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
| 1 | native/mock parity | Major | 重複文字のtie-breakがnative DiffUtilとmock LCSで異なる | 解消 | 両方を同じ決定的LCSへ揃え、`aa`/`ab`で2文字目だけ差分になることを確認。独立再reviewでCritical/Majorなし。 |
| 2 | long-input performance | Major | 長い発話でmain thread負荷またはmockの二乗memoryが膨らむ | 解消 | prefix/suffixを先に除き、候補数とflat matrixの総cell数に上限を設けた。600×600と131072×1で装飾だけを省略し全文を維持。独立再reviewでCritical/Majorなし。 |
| 3 | contrast | Minor | Lightの既存accentは白背景で約4.02:1 | 修正 | 差分専用色をLight `#0057B8`、Dark `#8EDCFF`へ変更し、太字も維持。 |

### PDH-review-2
- 独立reviewは`370a133`を再確認し、Critical/Majorなし。flat `IntArray`と総cell上限により細長い入力でも大量の行objectを作らないことを確認した。
- counterexample: `aa`/`ab`は修正前にnativeとmockで差分位置が不一致、修正後は両方とも2文字目だけを強調する。通常例の単一候補と同一候補は修正前後とも全文を通常表示する。

## PDH-verify. AC裏取り
- AC 1: JVM testとAndroid instrumentationで、提示3候補の強調文字が順に`を・ほ`、`を・欲`、`が・欲`だけになることを確認。
- AC 2: 単一候補・同一候補にspanが付かないJVM testがPASS。
- AC 3: 2行目tapで2行目全文だけをcommitし、認識が再開する既存service testがPASS。装飾後も行indexと全文を変更していない。
- AC 4: native instrumentationと実ブラウザmockの双方で同じ提示例を観察し、mockの2行目tapも2行目全文だけをreadonly表示欄へ入力した。
- 安全性: 挿入、補助平面Unicode、重複文字、600×600、131072×1の回帰testがPASS。上限超過時も候補全文とtapは維持し、装飾だけを省略する。
- Surface Observer: API 36.1 AVD上のactual Android Viewと、ローカルWebサーバ上のactual browser DOMを観察した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->
- Decision 21と`docs/reference/sites-native-spec.txt`へ、複数音声候補の非共通文字だけを太字・accent色にする規則を追記した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->
- 音声レイヤーで似た候補を表示し、助詞や漢字だけが青い太字になること、各行をタップすると選んだ全文だけが入力され認識中へ戻ることを確認してもらう。
- 実装・自動検証・独立reviewを完了し、ticket closeをユーザの明示承認待ちとして提示した。
- [2026/09/14 12:24 JST] 実装・検証結果と作業ブランチpush後、ユーザが「公開して」と明示したため、v0.15.12の公開とticket closeを承認したものとして進行する。
- merge直後に失う利用者機能はない。音声候補の全文、並び順、tap確定、連続認識を維持し、差分装飾だけを追加する。
- GitHub Release: https://github.com/masuidrive/android-kbd/releases/tag/v0.15.12
- APK: https://github.com/masuidrive/android-kbd/releases/download/v0.15.12/gesture-ime-v0.15.12.apk
- 公開APK: 38,599,756 bytes、SHA-256 `450ab93c8f571316cebfbf366f4f6656929d5b750a9028cff009de1b1679f86e`。公開後に再取得し、local成果物とbyte一致。
- 製品ページ: https://masuidrive.jp/products/md-kbd/
- 公開site: masuidrive.jp commit `55cf085`、Pages run `34802979326` success。index、mock、manual、CSSがlocalとbyte一致した。
- 公開browser確認: 412pxと840pxで横overflowなし、browser errorなし。左フリックで音声レイヤーへ入り、3候補の差分が順に`を・ほ`、`を・欲`、`が・欲`だけ強調された。2行目tapで2行目全文だけが入力され、認識中へ戻った。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
