# Work Notes: 261012-114749-voice-return-cancel-status

## Status: PDH-verify (v0.15.31 released; native speech-model surface observation and close pending)

## Checklist
- [x] 左下を前回レイヤーのラベル・タップ復帰・既存フリック切替に変更する。
- [x] 左下の隣へ候補を破棄して継続する「キャンセル」を置き、候補一覧内の旧キャンセル行を除く。
- [x] 準備中・認識中・処理中・候補待ちと波形を選択エリアへ移す。
- [x] nativeとブラウザモックを揃え、スマホ・タブレット幅の操作と高さを検証する。
- [x] 認識中のキャンセルで古い認識を破棄して再開し、callbackの混入を防ぐ。
- [x] 音声状態のTalkBack通知とフォーカスを波形更新中も保つ。
- [x] v0.15.30 APKを直接配布のGitHub Releaseへ公開し、再ダウンロードしたassetとローカルAPKをbyte比較する。
- [x] 製品ページ・マニュアル・操作モックを公開し、公開ファイルと実ブラウザで確認する。
- [x] 音声候補を選んだ瞬間に他候補を消し、選択候補が上へ抜けて消えるアニメーションをWebモックへ実装する。
- [x] アニメーションを公開サイトで操作し、速度・消え方をユーザが確認できるようにする。
- [x] 選択候補が候補枠の上端まで動くようWebアニメーションを調整し、到達直前まで文字を見せる。
- [x] 上へ抜けるWeb候補の最後のopacityを約20%にして、製品ページへ公開する。
- [x] Webでの確認後、同じ選択フィードバックをnative音声候補へ実装する。
- [x] 更新したAndroid APKをビルド・検証し、GitHub Releaseへ直接ダウンロード可能なAPKとして公開する。
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
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [ ] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
- Product Briefの6レイヤー共通操作体系に接続。影響層: Android app、IME service、custom view UI、unit tests、instrumentation/device tests、HTML/CSS mock、docs。Mozc JNIは無変更。
- 現状確認: `ImeService.voiceReturnMode` は既に進入前レイヤーを保持。`KeyboardLayouts`のVOICE最下段はキャンセルと非操作状態slotを使い、`VoicePanelView`は固定高の候補エリアである。したがって左下ラベル・slot差替え・status移設は既存モデル内で実装可能。
- ユーザの明示依頼は前回レイヤー復帰、隣キャンセル、状態の選択エリア表示。AC 2の確定契約では認識中のキャンセルも現在の認識を破棄して再開する。候補一覧内の旧キャンセルを下段2番目へ移す。
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
- [x] 既存の戻り先保持・VOICEの6 slot・固定高panelをコードで確認した。412dp/840dpの実寸と各方向のproduction MotionEventは実装後に測定する。認識中キャンセルはAC 2どおり破棄・再開する。
- [x] AC 5: 現行Webモックの候補clickは入力確定直後に再認識開始と全体renderを行い、候補DOMは即時破棄される。選択候補の退出演出を確認できるよう、Web先行の短い暫定速度を実ブラウザで測り、412px/840pxで他候補の即時消失、単一入力、Cancel/レイヤー移動時の残像なしを確認する。native実装はWeb確認後に行う。

## PDH-implement. 実装ログ
- browser mock: `site/mock.html` と `docs/reference/mock-source.html` を同一化。ローカル公開構成 `site/index.html#demo` で412px・840pxを実操作し、日本語とQWERTYからの左フリック進入、戻り先ラベル（あん/Az）、左下tap復帰、候補キャンセル後の再認識、固定45px操作行、横はみ出し無しを確認。`node --check` PASS。
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- Android実装commit `415550b`、モック・文書commit `0164bed`。Coding Engineerのfocused unit 134件 PASS、`compileDebugAndroidTestKotlin` PASS。監督側が接続エミュレーターで追加 `KeyboardViewVoicePunctuationTest.voiceReturnAndCandidateCancelUseProductionMotionEventsAtPhoneAndTabletWidths` を実行し1/1 PASS。最初のinstallは/data空き219MBの容量判定で失敗し、テスト中だけstorage thresholdを1%/50MBにして再実行した。終了後に元へ戻す。
- `6971a9f` の `scripts/test-all.sh --parallel --connected` は fast-checks / Android unit・lint・APK / connected の3/3 PASS。ただし独立reviewのCritical/Major修正で再実行が必要。
- review修正commit `6b9c581`: VOICE下段Cancelを認識中にも即時generation破棄・再開。status viewを維持しRMSはinvalidateだけ、Partial更新もstatusをdetachせず、ラベル変更時だけaccessibility announcement。unitに旧callback拒否・フォーカス/通知回数を追加。focused unit PASS。
- mock・文書commit `01cb9b1`: 認識中Cancelの再開、ARIA説明、仕様、マニュアルを一致させた。ブラウザ実操作で認識中Cancel後に旧3候補ではなく次世代の1候補だけが表示されることを確認。412/840画像を目視し、マニュアル画像を新mockへ差替えた。
- 最終SHA `0a941dc` の `scripts/test-all.sh --parallel --connected` は fast-checks / Android unit・lint・APK / Android connected (real Mozc) の3/3 PASS。独立reviewはCritical/Majorなし。AC裏取りはAC 1・4 VERIFIED、AC 2・3は実日本語音声モデルによるnative観察がないためNOT VERIFIED。コード・unit・mockでは期待動作を確認。
- Surface Observer相当の実エミュレーター観察を試行。最終debug APKをinstallし、IMEを有効化・選択して入力テストの通常欄をtapしたが、`mInputShown=true` / `mInputViewStarted=false` / `mShowInputRequested=false` で入力ビューは開かなかった。スクリーンショット `/private/tmp/voice-ui-native-final.png` にキーボードは写らない。ハードウェアキーボード表示設定を一時変更しても変化なし。設定はすべて `null` に戻した。実音声の状態遷移・RMSは未観察。
- Web先行のAC 5: `55fad54` で候補の退出アニメーションを実装し、`9aef392` で2・3行目の選択位置を保つFLIP補正を追加。候補選択直後に他候補がDOMから消え、選択候補は260msで34px上へ移動・フェードアウトする。412pxと840pxで実ブラウザ操作し、2行目 `top 371→371→80ms後343.9`、3行目 `426→426→80ms後398.9` を確認。入力は1回、次の認識へ進み、Cancelで古いタイマーは復活しない。`site/mock.html` と `docs/reference/mock-source.html` はbyte一致。最終Web SHA `9aef392` の `scripts/test-all.sh --parallel` は fast-checks と Android unit/lint/APK の2/2 PASS。nativeのAC 5はWeb確認後に実装する。
- Web視覚調整 `4e34aa478ae54b0020ce8c41b7f689d63ca48829`: DOM実寸で候補の開始位置、枠上端、候補高さを測って移動距離を決める。260msの78%時点では候補上端が枠上端へ達し、文字は不透明のまま。残り22%で枠外へ抜けつつフェードする。選択候補をsticky状態行より前面へ置く。独立レビューでphone/tablet各1〜3行を確認し、約185msで上端到達・opacity 1、約210ms以降のみフェード、スクロール済みphone 3行目も良好。非選択の即時消去、入力1回、次認識、reduced motionでの即時進行、モック正本byte一致、ブラウザエラーなし。Critical/Majorなし。最終SHAの`scripts/test-all.sh --parallel`は2/2 PASS。
- Web終端不透明度 `1228eb9` は20%へ調整し、412px・840pxの実ブラウザで入力1回、横はみ出しなし、正本とのbyte一致を確認した。native初回実装 `09d1fdc` は接続エミュレーターのattached `VoicePanelView` instrumentation 2件が通過したが、独立reviewで確定遅延と時間補間の2件を発見した。修正 `d549cb7` は候補tap時にeditorへ1回だけ確定し、260msの表示終了後に次の認識を開始する。取消・layer・editor切替では確定済み文字を保持して再開予約だけを破棄する。実時間78%で上端へ達してからalpha 0.2へ退出する。回帰unitは修正前FAIL・修正後PASS、focused unitと接続エミュレーターのattached view instrumentation 2件はPASS。最終SHAの全チェック・公開物照合は別途記録する。

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
| 1 | AC 2 / test追従 | Critical | 進行中認識ではキャンセルがinert。認識世代を破棄して再開する必要がある。 | 採用・修正済み、再review通過 | `6b9c581`で世代破棄・再開・旧callback拒否を追加。 |
| 2 | TalkBack / 対称関係 | Major | RMS更新ごとに状態nodeを再生成し、状態の読み上げとフォーカスを失う。 | 採用・修正済み、再review通過 | `6b9c581`で状態view維持と遷移時通知、Partialもdetachせず。 |
| 3 | 色 | Cosmetic | native状態ラベル色がmockより強い。 | 修正済み | `VoicePanelView`の状態ラベルを`keyboard_muted_text`に揃えた。native描画の実機観察は未了。 |

### Findings (PDH-review-2: Web animation preview)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | AC 5 / 視覚的連続性 | Major | 2行目候補は選択前top371pxから直後316px、3行目は426pxから直後316pxへ瞬間移動する。先行候補のflow削除後に選択行が再配置される。 | 採用・修正済み、再review通過 | `9aef392`のFLIP補正で選択行の開始位置を保持。独立再レビューで2・3行目とも直後の位置は不変で、80ms後は約27px上へ移動。Critical/Majorなし。 |

### Findings (PDH-review-3: native voice candidate animation)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | AC 5 / native・mock state retention | Critical | nativeは候補tapから260ms後にだけ確定し、その間のキャンセル・レイヤー・editor切替でtap済み文字が0回入力になる。Webはtap時に1回入力する。 | 採用・修正済み、対象差分再review通過 | `d549cb7`でtap時に1回確定し、切替時は再開予約だけ破棄。修正前FAIL・修正後PASSの回帰unitあり。 |
| 2 | AC 5 / native・mock motion parity | Major | PathInterpolatorの出力を78%区間判定に使い、nativeは約78msで枠上端へ到達する。Webは約203msまで移動して到達する。 | 採用・修正済み、対象差分再review通過 | `d549cb7`で実時間78%を区切り、両区間に独立easing。120ms/250msの接続testあり。 |

- 3回目のreviewはWeb先行からnativeへ対象を広げた初回のnative review。繰り返しの広い差分reviewを増やさず、2件の再現操作を固定して、修正後は候補tap→即時確定→途中切替と78%時点の位置・alphaを実挙動中心に裏取りする。
- `d549cb77a51480c45d33717b68bf2403b16d656b`の指摘2件に限る独立再reviewはCritical/Majorなし。即時commit、無効化時の確定保持、実時間78%の区間と個別easingを確認した。

## Technical reference 更新
- `technical-reference.md` の音声レイヤー操作・状態表示・波形描画先・候補キャンセル位置を、下段キーと `VoicePanelView` の新構成へ更新した。
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
差分・検証結果・実機未観察の範囲を説明した後、ユーザが「リリースして」と明示指示した。v0.15.30の公開は承認済みと扱う。一方、実日本語音声モデルでの進行中キャンセル、状態遷移、RMS波形はまだ観察できず、AC 2・3は未検証のためticket closeは保留する。ブラウザモックの参考画像は `site/assets/voice-layer-mock-v0.15.30.png`。

## v0.15.30 リリース記録
- リリース元SHA `19983ee727595308a87ecf46c0035b0bde4df376`。`scripts/test-all.sh --parallel --connected` の初回はAVDの/data空き255MBでAPK installが拒否され、fast-checksとunit/lint/APKはPASS、connectedはFAIL（0 tests）。既定の容量しきい値は両方`null`で、テスト中だけ1%/50MBへ下げて同じSHAで再実行した。実出力は `PASS: fast-checks`、`PASS: android unit, lint, apk`、`PASS: android connected (real Mozc)`、`Passed: 3 / 3`。終了後、しきい値を削除し`null`へ復元した。
- `gesture-ime-v0.15.30.apk` は38,669,704 bytes、SHA-256 `f28f1da36fd8c34e83eca1514704ca66c827039c48e58613b119afe5e79f9a73`。`com.masuidrive.gestureime`、versionCode 46 / versionName 0.15.30、API 28以上、arm64-v8a、`RECORD_AUDIO`を要求し`INTERNET`は要求しない。GitHub Releaseから再取得し`cmp`で一致を確認した。
- GitHub Release `https://github.com/masuidrive/android-kbd/releases/tag/v0.15.30`、APK直リンク `https://github.com/masuidrive/android-kbd/releases/download/v0.15.30/gesture-ime-v0.15.30.apk`。feature branchをoriginへpushした。ticketはAC 2・3の実音声native観察待ちでopenのまま。
- 製品サイトの公開元commit `0b4ee75adecc1d5fa4bda386bdca0d0e15e24732`。`site/index.html`、`site/mock.html`、`site/manual.html`、`site/styles.css`を公開元とbyte比較し、Pagesのビルドcommit一致を確認した。公開URLから同4ファイルと音声モック画像を再取得してbyte比較も一致。公開ブラウザ412px/840pxで横はみ出しなし、音声レイヤーへの左フリック・認識中Cancel後の再準備・かなキー入力を確認し、ブラウザエラーなし。参考画像 `/private/tmp/md-kbd-v0.15.30-public-phone-mobile.png`、`/private/tmp/md-kbd-v0.15.30-public-tablet.png`。

## AC 5 Web先行プレビュー公開記録
- Webソース最終SHA `9aef39238aa0446b10be58c38742515b922e9ffd`、公開元 `masuidrive/masuidrive.jp` commit `ec791b68a34d8d35c96812dc5b1a513e04119db8`。GitHub Pages latest buildは同commitの`built`。公開元と`site/`の主要4ファイルはbyte一致し、公開URLから再取得した4ファイルも公開元とbyte一致。
- 公開ページ `https://masuidrive.jp/products/md-kbd/#demo` を412pxブラウザで操作。左フリックで音声レイヤーに入り、3行目候補を選ぶと選択前後のtopはともに437px、他候補は即時0件、選択候補1件のみ、CSSアニメーション0.26s。readonly入力欄は選んだ文1回のみ。350ms後は選択候補0件で認識中へ戻る。840pxではページscrollWidth825px / viewport840px、モックscrollWidth793px / iframe幅793pxで横はみ出しなし。Webのみの先行プレビューであり、nativeのAC 5および実音声AC 2/3は未検証・未実装の範囲が残る。
- 上端到達版の公開元commit `57c4ebd9feed0d84148b690596210054d7299d05`。Pages latest buildは同SHAの`built`、公開URLから取得したindex/mock/manual/stylesは公開元とbyte一致。公開412pxの実ブラウザで左フリック→音声候補3件→3行目選択を実施。選択直後の候補topは437pxのまま、候補は1件になり入力は選択文1回。200ms時点の候補topは299.004px、枠topは299px、opacityは1。840pxはページ825/viewport840、モック793/iframe793で横はみ出しなし、画像欠落0。独立レビューは1〜3行目のphone/tabletとreduced motionまでNo Critical/Major。native実装はWeb確認後の作業として残す。

## v0.15.31 リリース記録
- リリース元SHA `b61a37123f2459ea8b8ea64411ea31303a628264`。`scripts/test-all.sh --parallel --connected` は `PASS: fast-checks`、`PASS: android unit, lint, apk`、`PASS: android connected (real Mozc)` の3/3 PASS。エミュレーターの一時的なstorage thresholdは1%/50MBから元の`null`/`null`へ復元した。物理端末と端末内日本語音声モデルによる実発話は未観察。
- ローカルAPK `/Users/masuidrive/Develop/personal/android-kbd/app/build/outputs/apk/debug/gesture-ime-v0.15.31.apk` は38,901,974 bytes、SHA-256 `85d3077c9edc3f38f403ce2a4c975a7c8e816c40aed589549f449d74797f45e5`。`com.masuidrive.gestureime`、versionCode 47 / versionName 0.15.31、API 28以上、arm64-v8a、`RECORD_AUDIO`要求、`INTERNET`要求なし。GitHub Releaseから再ダウンロードして`cmp`一致。
- GitHub Release `https://github.com/masuidrive/android-kbd/releases/tag/v0.15.31`、APK直リンク `https://github.com/masuidrive/android-kbd/releases/download/v0.15.31/gesture-ime-v0.15.31.apk`。feature branchをoriginへpush済み。AC 2・3の実音声native観察待ちのためticketはopenのまま。
- 製品サイト公開元commit `81acb9ba883fb159476ae2f64283f14abfe6a7b9`。公開元と`site/`のindex/mock/manual/stylesをbyte比較し、Pages latest buildは同SHAの`built`。公開URLから再取得した同4ファイルも公開元とbyte一致。実ブラウザ412pxでは左フリックで音声候補3件を表示し、3行目を選んだ200ms時点でtop299.004px/枠top299px、opacity 1、他候補0、入力1回。別候補の250ms時点でopacity 0.20428。840pxでは2行目を選んだ200ms時点でtop266.002px/枠top266px、opacity 1、他候補0、入力を確認。412pxはページ397/viewport412、mock365/frame365、840pxはページ825/viewport840、mock793/frame793で横はみ出しなし、画像欠落とbrowser errorなし。参考画像 `/private/tmp/md-kbd-v0.15.31-public-phone.png` と `/private/tmp/md-kbd-v0.15.31-public-tablet.png`。
- nativeのAC 5はattached `VoicePanelView` instrumentationと`ImeService` unit、独立reviewで確定保持・上端到達の時間配分・終端alphaを確認した。実際の音声結果を載せたproduction IME画面の観察はできていないため、AC 2・3とともに物理端末確認へ残す。
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
