# Work Notes: 261029-020940-personal-info-suggestions

## Status: PDH-human-review (2026-10-30 18:05 JST)

## Checklist
- [x] ユーザ依頼（2026/10/30）: 十字popup修正版の試用APKをFlyerの既存1日限定ページへアップロードする。
- [x] ユーザ指摘（2026/10/30）: 個人情報・定型文一覧の最下段句読点は入力だけでなく、音声レイヤーと同じ5方向の十字popupをフリック中に表示する。
- [x] ユーザ追加依頼（2026/10/30）: 個人情報picker下段のレイヤー切り替え・各編集キーの割当フリックと未割当tap動作を実機経路で確認し、Spaceを約2キー幅へ、端余白・キー間隔を通常キーボードへ揃える。必要なら下段の寸法と操作定義を整理する。
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] ユーザ追加依頼（2026/10/30）: 個人情報カードを開いた直後はメールアドレスのみ上側に表示し、下段左の旧キャンセル位置に「more..」を置いて氏名・住所・定型文を展開する。高さを保つ。
- [x] ユーザ追加依頼（2026/10/30）: メールアドレスを設定で4件まで個別登録・保存し、明示メール欄の候補にも反映する。既存の1件を維持する。
- [x] ユーザ指摘（2026/10/30）: 個人情報一覧の最初の氏名候補の左に、他のキーと同じ余白を空ける。
- [x] ユーザ指摘（2026/10/30）: 氏名欄を選択した時の該当候補が一瞬で消えず、入力可能な間は表示される。
- [x] ユーザ依頼（2026/10/30）: 個人情報一覧の左下に前のレイヤーへ戻る切り替えキーを置く。
- [x] ユーザ依頼（2026/10/30）: 氏名4表記は各1欄にフルネームで登録し、候補は登録値の空白で姓と名に分けて提示する。
- [x] ユーザ依頼（2026/10/30）: メール欄ではBitwardenのインライン候補が表示されても登録メール候補を併記する。
- [x] ユーザ依頼: 設定画面で氏名・メールアドレス・住所を登録できるようにする。
- [x] ユーザ依頼: フォーカス時に入力欄の種類を判定し、判定可能な項目を候補として提示する。
- [x] ユーザ依頼: 候補欄のアイコンから氏名・メールアドレス・住所を選択できるようにする。
- [x] ユーザ依頼（2026/10/29）: 未入力時は候補欄のcard型アイコンを左に表示し、押すと登録済みの氏名・メールアドレス・住所をキー領域に表示して選べるようにする。明示inputType欄ではcardの右に該当候補を表示する。
- [x] ユーザ依頼（2026/10/29）: cardで開くキー領域では項目名を表示せず、登録値を詰めて折り返し表示する。
- [x] ユーザ依頼（2026/10/29）: 設定画面から定型文を登録・変更・削除し、cardの一覧から選んで入力できるようにする。
- [x] ユーザ依頼（2026/10/29）: 一覧の戻る操作を元のcard icon位置へ移し、値をキー領域の先頭から並べる。
- [x] ユーザ指摘（2026/10/30）: 公開モックでcard一覧を開いてもキーボード全体の高さを変えない。
- [x] ユーザ依頼（2026/10/30）: 公開モックで確認した固定高の個人情報・定型文pickerをAndroid版に反映し、実際のIMEで高さと操作を確認する。
- [x] ユーザ依頼（2026/10/30）: 人間レビュー用のAndroid APKをFlyerに24時間アップロードし、取得リンクと整合性を確認する。
- [x] ユーザ指摘（2026/10/30）: 個人情報カードアイコンのトーンを落とし、枠を外す。
- [x] ユーザ指摘（2026/10/30）: 入力テストの氏名欄でもカード右に対応候補を表示する不具合修正。
- [x] ユーザ依頼（2026/10/30）: 候補欄のカードと候補の間に通常キー相当の隙間を設ける。
- [x] ユーザ依頼（2026/10/30）: 氏名を漢字・ひらがな・カタカナ・英字の4表記で個別登録できるようにする。
- [x] ユーザ依頼（2026/10/30）: 住所を郵便番号・都道府県・市区町村・住所1・住所2に分けて登録できるようにする。
- [x] ユーザ指示（2026/10/30）: 住所候補は連結せず、各項目を独立した候補として出す。選んだ後も続けて別の項目を選べるようにする。
- [x] ユーザ依頼（2026/10/30）: キー領域の個人情報一覧にも音声入力と同じ最下段の記号・Space・Backspace・Enterを置き、キャンセルボタンは置かない。
- [x] 新UX: cardアイコンは候補欄の左に置き、空入力時に表示する。
- [x] 新UX: cardアイコンのタップでは候補欄の一覧を使わず、同じ位置の戻る矢印とキー領域先頭からの登録値ボタンを表示する。
- [x] 新UX: 明示inputType欄ではcardアイコンの右に該当する登録候補を表示する。ユーザが2026/10/29に「アイコンは左」と確定した。AC読み手は更新後のAC2/3を復元可能と判定した。
- [x] ユーザ質問: 入力欄の種類を IME が受け取れる範囲と限界を説明する（最終報告へ含める）。
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み（retry-passは下記記録）
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部HTTP providerを新設・変更しないAndroid端末内の機能。既存protected inline AutofillはQA AutofillServiceの実Android経路で観察。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（generic PDHの更新は不要。manual・native spec・mock・保存正本は同期）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み（このturnの最終応答で判断ボードを渡す）
- [x] PDH-human-review: ユーザが「とりあえずこれでよいよ」と回答し、試用APKの確認結果として本修正を承認した（2026/10/30）

## PDH-ticket-review. Ticket contract check
Why は brief の「素早い操作」「端末外へ送らない」に接続。影響レイヤーは Android app（設定・端末内保存）、IME service（EditorInfo 判定と候補操作）、custom view UI（候補欄アイコン）、unit tests、instrumentation/device tests、docs（仕様・mock・manual）。Mozc JNI は対象外。AC 1 は設定で保存・消去・再起動、AC 2 は種別付き欄、AC 3 は汎用欄とアイコン、AC 4 はprivate欄、AC 5 は既存 Autofill との併用をそれぞれ利用者が観察できる。Android の `EditorInfo.inputType` には person-name、postal-address、email のvariationがあるが、Web欄は generic text のことがあり自動判定を保証できない。`hintText`/`label`/`fieldName` は来る場合があるが、信頼できる種類の宣言ではない。AutofillService の候補は保護 View としてIMEへ渡され内容は読めない。

AC 読み手は AC 1〜4 を復元できた。AC 5 の「従来どおり」が曖昧との指摘を受け、候補が表示されタップで自動入力される観察結果へ修正し、再判定で復元できた。`Open Questions` と ticket 本文に未確認・TBD の未解決マーカーはない。設定は各1件とし、Webフォームの汎用欄は候補欄アイコンで手動選択する。複数プロファイルや曖昧なラベル推測は今回の要求にないため除外。ユーザは 2026-10-29 に「メアドや住所、氏名の登録は設定画面」「候補欄になんかアイコン出して氏名とかの選択」と明示した。これと同じ実装について再承認を求めず、非公開欄とBitwardenの非退行を合わせて実装へ進む。

PDH-ticket-human-review: `./ticket.sh check --require 'Required Probes'` は `✓ Required Probes: 1 / 1`。ユーザの二つの明示依頼がAC 1〜3を許可しており、AC 4/5は既存機能の非退行であるため、同じ実装の承認は再度求めない。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] Android の入力種別と inline Autofill 契約を公式資料と既存コードで確認した。`EditorInfo.inputType` にメール・氏名・住所のvariationがあり、Bitwarden等の保護候補の内容はIMEから見えない。AC 2/3/4/5の実際の表示と入力は実装後にproduction IMEとテスト画面で測る。条件に届かない場合は gate を止めて報告する。

[2026/10/30 10:18] 利用者の試用フィードバックによりhuman-reviewから実装へ戻した。既存の氏名/住所保存値を維持する新項目、候補復元、候補card外観・余白、固定高一覧内の編集行を実装中。旧レビュー・試験のpassは新差分に対して未検証のため、最終SHAで再実行する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

[2026/10/29 19:05] 変更は `6b5fbd7`（設定・保存）と `932b66a`（固定高picker、値だけのflow、mock・仕様）に分割。レビューで設定画面再生成のテストが古いrootを参照するMajorを発見し、新しいActivityを生成してroot相違・保存値復元・削除を検査するよう修正。focusedテスト通過、reviewer再確認でCritical/Major残件なし。実Android 16 AVDの412dp相当で定型文だけを登録し、cardから `Thanks_for_checking` を選んで入力欄に確定・復帰を確認。840dp相当でも値だけのボタンが横並びで重なりなし。画像は `/private/tmp/md-kbd-native-snippets-phone.png`、`/private/tmp/md-kbd-native-snippet-committed.png`、`/private/tmp/md-kbd-native-snippets-tablet.png`。物理端末とBitwarden本体は今回も未確認。`932b66a`の全体テストは初回unitとconnected各1件一時失敗、孤立実行はいずれも通過、同一SHAの再実行は3/3通過したためretry-pass。テスト修正後の最終SHAで再実行する。

[2026/10/29 19:08] テスト修正 `0a04ed0` の `scripts/test-all.sh --parallel --connected` は、最初の2回が環境preflightでAndroid tests未実行（ANDROID_HOME未設定、Gradle cache権限不足）。SDKを指定し必要な実行権限で同一SHAを実行するとfast-checks、Android unit/lint/APK、connected real Mozcが3/3通過。ログは `/var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.Gd6AlG6AoH/`。AC裏取りworkerはAC1・AC3をVERIFIED、既存AC2・4・5をNON-REGRESSION VERIFIEDと判定。production Android 16 AVDの412dp相当で1件の定型文選択、840dp相当で横並び表示を観察。複数項目の実折り返しはView layout testとbrowser mockで確認したが、native画面の写真は未撮影。物理端末・Bitwarden本体は未確認。`site/mock.html`と保存正本はbyte一致、`git diff --check`通過。検証後、AVDの表示設定をresetしてemulatorを停止した。`0.0.0.0:34567`のレビューサーバーはTailscale IP `100.77.42.32`からHTTP 200を確認。

[2026/10/29 20:30] ユーザが戻る位置を元card iconへ変更するよう指示したためAC3とDesign Decisionsを更新し、stageをimplementへ戻した。`CandidateStripView`の同じ38dp iconを一覧中だけ戻る矢印にし、pickerから戻るチップを除去。focused Android unitテスト3クラスはpass。実Chromiumで412pxと840pxのmockを操作し、戻る矢印と最初の値のx座標がともに13px、412pxで矢印は候補欄y294・値はキー領域y346、840pxでは1行に収まり横overflowなし。戻るで通常card iconとキー領域に復帰した。native実画面と最終全体試験はこれから。

[2026/10/29 20:39] `6124f59`で実装・mock・spec・manual・ticketを同期。独立reviewerはCritical/Major残件なしと判定。最終SHAの`test-all --parallel --connected`初回はfast checksとunit/lint/APKがpass、Android connectedの既存`VoiceRecognitionAvailabilityTest.reportsInstalledJapaneseModelSupport`が冷起動直後に10秒の応答待ちで失敗した。同テスト単独はpass、同一SHAの全体再実行は3/3 pass。結果区分はretry-pass。browserの412px Mobileでは戻るアイコンx=13pxと最初の値x=13px、元位置での開閉・入力なし復帰・overflowなしを確認し、840px Tabletも1行表示でoverflowなし。画像は `/private/tmp/md-kbd-personal-back-phone.png` と `/private/tmp/md-kbd-personal-back-tablet.png`。Android 16 AVDではGoogle初期設定overlayが途中で前面に割り込み、設定登録とテスト画面遷移まで試したが今回の矢印の実画面写真は取得できなかった。nativeの戻る位置とdispatchは`CandidateStripViewTest`、`ImeServiceEnglishSuggestionTest`で検証。エミュレータは表示設定をresetして停止した。物理端末確認は未実施。

[2026/10/29 20:48] Android実画面未取得の原因を再調査した。AVDに前回の画面上書き設定（1764x2200、336dpi）が残り、1080x2400前提のADB座標で別の設定ボタンを押していた。1080x2400、420dpiに明示設定しUI hierarchyのboundsから「入力を試す」を操作。設定へ `Taro yamada` を保存した後、production IMEのカードアイコンを押すと同じ位置の戻る矢印とキー領域左上の値ボタンが表示され、戻るタップでは入力せず元のカードとQWERTYキーへ復帰した。412dp相当の写真 `/private/tmp/md-kbd-native-picker-phone.png`、復帰後 `/private/tmp/md-kbd-native-card-restored-phone.png`。1764x2200、336dpiの840dp相当でも同じ配置を `/private/tmp/md-kbd-native-picker-tablet.png` で確認した。20:39時点の「Google overlayで実画面未取得」は最終的な原因説明として誤り。AC裏取りworkerへ新しいnative写真で再判定を依頼した。物理端末は未確認。

[2026/10/29 20:49] AC裏取りworkerは追加native写真とfinal code SHA `6124f59`のコード・テストを合わせ、AC3をVERIFIEDと再判定した。412dp/840dpで元card位置の矢印と値の左上配置、戻る後の無入力復帰を実画面で確認。複数値のnative実画面での折返しは未撮影だがView layout testとbrowser mockで確認。物理端末・Fold実機は未確認。suite区分はretry-passのまま。human review待ち。

[2026/10/30 00:45] Tailscale経由のレビューURLが利用者側で開けないため、利用者の明示指示で masuidrive.jp の操作モックを先行公開した。公開中APK v0.15.33 に個人情報選択は含まれないため、manualの新節を「次期APKのプレビュー」と明記し、目次へ追加。source `site/` の4ファイルと公開repoの4ファイルをbyte比較してから、`masuidrive/masuidrive.jp` mainへ `e68c05b` をpush。Pages APIは同SHAを `built` と返し、公開4ファイルはcommitとbyte一致。実Chromiumの公開ページ・mockで412pxのcard→戻る→card→氏名入力、840pxのcard→戻る矢印・値の左端配置を確認し、両幅とも横overflow・画像欠落なし。APKは未更新。ticket closeの明示承認は未取得のためhuman-reviewを継続する。

[2026/10/30 00:53] 利用者指摘の高さ変化を公開ブラウザ412pxで再現。通常時はroot 558px / keyboard 278px、card一覧時はroot 548px / keyboard 268px。通常キー最終行の下margin 10pxが一覧では欠けたことが原因。`site/mock.html`の一覧 `.keys` に同じ10pxの下marginを加え、保存正本とbyte一致。修正後の実Chromiumでは412pxで開閉前後root 558px / keyboard 278px、840pxでroot 541px / keyboard 278pxと同一。製品ページのhero iframeも開閉前後519pxで同一。横overflowなし。Android nativeの固定高panel実装は変更していない。公開サイト更新は次に実施する。

[2026/10/30 00:57] source commit `943ec3a`で一覧の余白を修正し、`5a50e12`で製品ページの埋め込みURLに更新識別子を付けた。公開repoは `3eace78`（mock修正）と `190d645`（iframe更新）をmainへpush。Pages APIは最終SHA `190d645` を `built` と返し、公開4ファイルは同commitとbyte一致。公開製品ページを実Chromiumで確認し、412pxはcard開閉前後hero iframe/rootが519px、840pxは486pxで同一。両幅とも横overflow・画像欠落なし。APK変更なし、ticket close承認待ち。

[2026/10/29 18:51] ユーザがhuman review中に仕様を更新。ticketのWhy/AC1/AC3/Design Decisionsを再合意内容へ更新し、stageをimplementへ戻した。個人情報3値に定型文の複数登録を加え、pickerは固定高のまま値のみの折り返しボタンへ変更した。SharedPreferencesのJSON配列は未登録時`[]`、空項目を保存しない。`EditorInfo`自動候補には定型文を混ぜず、cardからの明示操作のみで表示する。private欄ではcard自体を出さない。Kotlin用`similarity-generic -l kotlin -t 0.7`は言語未対応のためskip。focused 58件の初回実行はRobolectricの幅412pxでは折り返さないというテスト前提が誤り1件失敗し、240pxへ狭めたレイアウト試験はpass。Android実画面と最終全体試験はこれから。

[2026/10/29 11:14] 実装前の仮定を確認した。`TextInputController.isPrivateField` はpassword/visible password/web passwordと`IME_FLAG_NO_PERSONALIZED_LEARNING`をprivateとして判定し、`EditorInfo.inputType` のメール・person name・postal address variationは直接読める。既存`CandidateStripView`はprotected `InlineContentView`を値へ変換せず保持するため、個人情報iconはその同じrowへ別Viewとして追加する。SharedPreferencesは既存`ImePreferences`の端末内ファイルを使い、通信やAutofillService APIは追加しない。

[2026/10/29 11:20] `fb33119`で`ImePreferences`へ氏名・メール・住所の各1件の端末内保存（空値はkey削除）とSetup編集欄を追加した。`ImePreferencesTest`、`PersonalInfoSuggestionsTest`、`SetupActivitySlashCommandsTest`を実行し、`BUILD SUCCESSFUL`（16 tests）を確認した。

[2026/10/29 11:20] `0a7c1aa`で明示inputTypeの個人情報候補、generic欄の38dp人物icon、private欄の非表示、候補tapの`commitText()`、protected inline Autofill Viewとiconの同列表示を追加した。`ImeServiceEnglishSuggestionTest`、`CandidateStripViewTest`、`ImeTestActivitySafeAreaTest`を実行し、`BUILD SUCCESSFUL`を確認した。テスト用画面に氏名・住所の明示inputTypeとinstrumentation testを追加した。候補Viewの既存inline置換テストは最初に失敗したが、`showCandidates()`がprovider Viewを置換する既存契約を復元して再実行しpassした。

[2026/10/29 11:20] `3279c63`でmock、保存正本、native spec、manualを同期した。`cmp -s site/mock.html docs/reference/mock-source.html`は一致。実ブラウザでLight/Darkとも人物icon→3つの例示値→住所入力を確認し、JavaScript errorはなかった。`similarity-generic -t 0.7`は導入済みCLIがKotlin languageを未対応と報告したため、Kotlin変更の構造重複検出は環境制約でskipした。

[2026/10/29 11:27] Director reviewを反映して`ce5019b`で人物iconを候補の横scroll領域外へ固定し、長い通常候補・protected inline Autofill Viewがあってもgeneric欄の導線を常時表示するよう修正した。iconからの選択肢は「氏名: 値」「メールアドレス: 値」「住所: 値」と表示し、選択イベントは対応する生値だけを`commitText()`する。private欄ではiconを`GONE`にし、候補も表示しない。`CandidateStripViewTest`、`ImeServiceEnglishSuggestionTest`、`ImePreferencesTest`、`PersonalInfoSuggestionsTest`、`ImeTestActivitySafeAreaTest`は61件pass。`PersonalInfoInputTypeInstrumentedTest`はMedium_Phone_API_36.1で1件pass。browser mockは人物icon→3項目名付き選択肢→住所の生値入力を確認し、840px幅でJavaScript errorなし。`cmp -s site/mock.html docs/reference/mock-source.html`は一致。

[2026/10/29 11:27] 最終SHA `ce5019b`で`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk PATH=/Users/masuidrive/Library/Android/sdk/platform-tools:$PATH scripts/test-all.sh --parallel --connected`を実行。fast-checks 5件pass、Android unit/lint/APK pass、Medium_Phone_API_36.1接続試験32件pass（3/3区分pass）。使用後に`adb -s emulator-5554 emu kill`でエミュレータを停止した。

[2026/10/29 11:36] 更新後の利用者指示に合わせ、`2a488e8`で未入力時の候補欄左端38dp card型icon、明示inputType欄のcard右側の該当候補、cardタップ時にキー領域へ出す項目cardと戻るキーを実装した。項目cardは「氏名: 値」等を表示し値だけを確定して元のキーボードへ戻る。戻るキーは入力せず復帰し、空項目は表示しない。cardのvectorも人物入りカード形状へ変更した。candidate strip、native panel、mock、保存正本、spec、manualを同期した。

[2026/10/29 11:36] Director指摘を反映して`9c46fd4`でinline Autofill候補表示中も候補欄を再描画せずキー領域panelだけを更新し、card→panel→戻る/選択を維持するよう修正した。finish input、start input/view、input-view再生成、private欄遷移ではpanelの登録値と可視状態を破棄する。`ImeServiceEnglishSuggestionTest`のinline active card経路・戻る・private切替、`ImeHideBarTest`、`CandidateStripViewTest`、`PersonalInfoPickerViewTest`はpass。mockは左card→キー領域の3項目+戻る→住所の生値入力、および840px幅・JavaScript errorなしを確認し、`cmp -s site/mock.html docs/reference/mock-source.html`は一致。

[2026/10/29 11:36] 最終SHA `9c46fd4`で`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk PATH=/Users/masuidrive/Library/Android/sdk/platform-tools:$PATH scripts/test-all.sh --parallel --connected`を実行。fast-checks 5件pass、Android unit/lint/APK pass、Medium_Phone_API_36.1接続試験32件pass（3/3区分pass）。使用後に`adb -s emulator-5554 emu kill`でエミュレータを停止した。

[2026/10/29 11:42] 独立reviewのCriticalを修正した。`af4dff9`で個人情報panelをlifecycleで破棄する際、panelが一時的に保持した全登録値を`candidates`・`candidateSource`からも消去するようにした。戻る操作だけは明示inputTypeの単一自動候補を復元する。inline Autofillの復帰は`showCandidateState()`を経由し、panel表示中に全登録値を候補欄へ戻さない。`ImeServiceEnglishSuggestionTest`でfinish/startとinput-view再生成、inline復帰、private遷移を実Viewで検証し、`InlineSuggestionsTest`、`CandidateStripViewTest`、`PersonalInfoPickerViewTest`とともに`BUILD SUCCESSFUL`を確認した。全体テストはDirectorが最終SHAで実行する。

[2026/10/29 11:43] 再review推奨により`a7ea6ed`でpanel用の全登録値を共有`candidates`から`personalInfoPickerValues`へ分離した。キー領域panelの選択時だけこの専用リストから値をcommitし、通常候補・inline復帰・lifecycle経路は専用値を参照しない。mockの左cardもnative vectorと同じ人物入りカードSVGへ同期し、manualの人物icon/card表現を個人情報カードアイコンへ統一した。focused regression（generic/明示/private、open→finish/start、open→onCreateInputView、inline active open→inline終了、戻る/選択）として`ImeServiceEnglishSuggestionTest`、`InlineSuggestionsTest`、`CandidateStripViewTest`、`PersonalInfoPickerViewTest`を実行し`BUILD SUCCESSFUL`、`cmp -s site/mock.html docs/reference/mock-source.html`一致を確認した。全体テストはDirectorが最終SHAで実行する。

[2026/10/29 11:50] Astra根因診断を反映し、`e8a9ac8`でpanel専用状態の破棄と既存input-viewの候補再描画を分離した。`onFinishInputView(false)`は値を残さず再開マーカーだけを保持し、`onStartInputView(..., true)`と単独`onCreateInputView()`はgeneric欄で左cardのみ、明示inputType欄でcard右の単一自動候補だけを復元して全surfaceを描画する。`onStartInputView`で通常の変換候補を無条件に消さない。focused regressionは同一root/stripでgeneric・email各2周のcard→finish/start、genericの再card→1回確定、inline Autofill中hide/show、private切替、開いたpickerの単独input-view再生成、inline終了を実行した。`ImeServiceEnglishSuggestionTest`、`InlineSuggestionsTest`、`CandidateStripViewTest`、`PersonalInfoPickerViewTest`は`BUILD SUCCESSFUL`（29 tasks）で、`cmp -s site/mock.html docs/reference/mock-source.html`と`git diff --check`もpass。全体テストはDirectorが最終SHAで実行する。

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
| 1 | IME lifecycle / privacy | Critical | picker用の登録値を通常 `candidates` に置いたため、hide/showやinput-view再生成で通常候補欄へ全値が残り、cardも消える | 採用 | `af4dff9`、`a7ea6ed`でpicker専用状態へ分離。generic・明示型・private・inlineの回帰試験で漏出しないことを確認。 |
| 2 | native / mock parity | Major | mockは人物glyph、nativeはcard形iconで表示が不一致 | 採用 | `a7ea6ed`でmockと保存正本をnative vectorに合わせて同期。 |

### Findings (PDH-review-2)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | IME lifecycle / privacy | Critical | `onFinishInputView(false)`後の同一View再表示でcardと明示型候補が復元されない | 採用 | Astraで根因を診断し、`e8a9ac8`でpicker値破棄と候補再描画を分離。generic/emailを同一root/stripで2周ずつ再表示し、card再操作、private遷移、inline経路もfocused testで確認。独立reviewerが`e8a9ac8`を再reviewしCritical/Major残件なしと判定。 |

### Findings (PDH-review-3, `6674af2`)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | 設定保存 test | Major | 旧3項目のtestが10項目化で失敗 | 採用 | 全10項目の保存・再生成・削除を確認するtestへ更新。focused test通過。 |
| 2 | footer gesture | Major | 独自の24dp一発判定は通常キーの閾値・中心復帰・方向未割当と異なる | 採用 | `GestureInterpreter`を共有し、実`MotionEvent`で方向、閾値、中心復帰、未割当、キャンセルを検証。修正前に別経路のEnter上下とSpace四方向は既存focused testでpass、修正後も同テストでpass。 |
| 3 | footer geometry | Minor | presetのpitchでfooter高さを固定し、幅変化後の実キー高に同期しない | 採用 | `KeyboardView`の有効row pitchを幅ごとに読み、panel同期時に値の変化がある場合だけfooter寸法を更新。 |

同種Criticalの再発に対してAstraで根因診断を実施した。修正前の壊していない側は「generic欄の通常候補表示」、修正後も通常候補が維持されることを同じfocused testで確認した。追加した状態は再表示用markerのみで、永続データ・公開endpoint・権限は増やしていない。実装commitは設定、IME、docs、review修正に分かれている。ticketのWhy・AC・Architectural Invariants・out-of-scopeは実装workerから変更していない。外部provider実APIの確認は本件に該当しない。Androidの保護inline Autofillは別のplatform経路として実Viewで検証する。

### PDH-verify surface（最終実装SHA `e8a9ac855aaedbce91da0ea7601ff920de781b78`）

Surface Observerはproduction IMEをAndroid 16 API 36.1 AVDで操作した。412dp相当と840dpの両方で、generic欄の左card→固定高キー領域の3項目+戻る→値だけ確定、明示email/name/address欄のcard右候補、private欄の非表示を観察した。設定3値はforce-stop後の再起動でも残った。IME frameは412dp相当で初回・picker・戻る・private・hide/showすべて`[0,1545][1080,2400]`、840dpで初回・picker・private・hide/show・input-view再生成すべて`[0,1499][1764,2200]`。QA AutofillServiceの保護inline候補を表示し、tapでemail/passwordが入力された。Bitwarden本体と物理端末は未確認。画像は`tickets/261029-020940-personal-info-suggestions/tmp/surface-observer/`に保存した。

browser mockは最終SHAの`site/mock.html`を実Chromiumで動かした。412px Lightで左card→キー領域の氏名/メール/住所/戻る→住所の生値入力と元のキーボードへの復帰を確認し、document幅はviewportの412pxと一致。840px Darkでcard→3項目表示、幅840px一致を確認した。`matchMedia('(prefers-color-scheme: dark)')`がtrueを返すことを確認した。nativeと保存正本のmockはbyte一致させる。browserは例示値を使う操作見本で、設定値の永続性やAndroidのprotected inline候補の証拠には使わない。

合成済み製品ページ`site/index.html`のiframe内mockも実Chromiumで確認した。412px Lightで左cardを押して3項目がキー領域へ出て、戻るで入力せず復帰。840px Darkではproduct pageとiframeが表示され、cardが見え、`prefers-color-scheme: dark`がtrue、document幅はviewportを超えなかった。次の840px操作時にagent-browserのsocket directoryがsandboxで書けず中止したが、単体mockの840px操作は上記どおり確認済み。

### PDH-verify test 実行記録

最終SHAでの初回`scripts/test-all.sh --parallel --connected`はfast-checksのみpass、Android 2区分は`~/.gradle/wrapper/...zip.lck`への`Operation not permitted`でテスト開始前に失敗。記録は`tmp/test-all-final.log`。同一SHAでGradleキャッシュへアクセスできる実行権限にして再実行する。これはtest failureではなく環境のpreflight failureとして、初回結果を消さずに記録する。

権限を調整した最初の実行（`tmp/test-all-final-escalated.log`）ではfast-checksとconnectedがpass、unit/lint/APK区分で`ImeHideBarTest.privateEditorSwitchesToItsDedicatedEmptyPickerWithoutReusingPublicPicker`が失敗（345件中1件）。当該テスト単独と`ImeHideBarTest`クラス単位はpassしたが、全unitの`--rerun-tasks`（`tmp/unit-rerun.log`）では同テストと`ImeHideBarTest.emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway`が失敗（345件中2件）。いずれも絵文字レイヤーの表示テストであり、本ticketの変更経路に隣接するため、原因確定なしに無関係と扱わない。retry-passの判断が必要なまま記録する。

同一SHAでの全体再実行結果（`tmp/test-all-retry.log`、suite自身のsummary行）:

```text
========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk
  PASS: android connected (real Mozc)

Passed: 3 / 3
```

**結果区分: retry-pass**。unit内の2件は上記のとおり初回または全unit再実行で失敗しており、greenな一発通過とは表現しない。最終コード変更後の追加diffはない。`cmp -s site/mock.html docs/reference/mock-source.html`と`git diff --check`はpass。

AC裏取りAgentは最終実装SHAでAC 1〜5を全件VERIFIEDと判定した。AC 1は設定値の保存・空値削除の単体試験とforce-stop後の実画面、AC 2は明示3種類の412dp/840dp表示と候補tap、AC 3はgeneric欄のカード一覧・戻る・生値確定・空欄除外、AC 4はprivate欄の候補非表示、AC 5はQA AutofillServiceの実protected inline候補とtap後のemail/password入力を根拠とする。Bitwarden本体・物理端末・Android 9〜15は未確認であり、QA providerの結果をBitwarden固有の実機証拠とは呼ばない。テストはretry-passなので、ticket closeの可否は人間レビューで判断を求める。エミュレータは検証後に停止した。

変更前の`7c5b30f`で全unitテストを同じAndroid SDK・Gradleで一度実行し、`BUILD SUCCESSFUL`だった。したがって今回の一時失敗を変更前からあると実証できておらず、retry-passの区分は維持する。`scripts/dev-server.sh --seed --port 34567`でlocalhostの合成済み製品ページを起動し、412px Lightでは左card→3項目→戻る、840px Darkでは左card→3項目→メール選択で生値入力と復帰を確認した。両幅で横overflowなし。画像比較は変更前後の同じ412px Light mockを`/private/tmp/md-kbd-personal-before.png`と`/private/tmp/md-kbd-personal-after.png`、開いた一覧を`/private/tmp/md-kbd-personal-open.png`へ保存した。実装差分の逆適用dry-runは`git diff --binary features/261027-065123-make-ca-tap-tab..e8a9ac8 | git apply -R --check`でpassし、作業treeは変えていない。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->
`technical-reference.md`の個人情報候補節を追加した。`EditorInfo.inputType`による明示型判定、左card、キー領域picker、private欄非表示、inline Autofillとの共存、同一View再表示時の描画復元を実装と照合する。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->
判断ボードは機微情報の扱いに合わせてrepo外の`/private/tmp/md-kbd-personal-info-review.md`にmode 600で保存し、例示値のみを使用した。独立したAstra reviewerは初回、AC 4/5の確認範囲・Bitwarden本体未確認・rollback後の保存値・入力先アプリの指定条件が曖昧で「決められない」と判定。boardを修正し、同reviewerが「決められる」と再判定した。残る軽微な日本語表現1点も修正した。追加調査を求める指摘は残らない。ユーザにはこの達成とretry-passを受け入れてticketを閉じるか判断してもらう。明示の返答まではclose、merge、公開を行わない。

[2026/10/30 07:23] 利用者の「それで実装して」を受け、公開モックの固定高一覧と既存native実装を突合した。nativeは`PersonalInfoPickerView`をキー4行と同じ高さのoverlayとして候補行の直下へ配置しており、rootの高さは候補行と通常キーの測定値で決めている。412/840幅でcard開閉前後のroot測定高と4件表示・戻る時の非入力を検証する回帰テストを追加し、focused Gradle実行はpass。Android実画面での再確認と最終suiteは続けて行う。

[2026/10/30 07:27] Android 16 AVDへ現在のfeature APKを導入し、1080×2400/420dpi（約412dp）と1764×2200/336dpi（約840dp）でgeneric欄のcardを実tapした。前後スクリーンショット`/private/tmp/md-kbd-personal-current.png`と`/private/tmp/md-kbd-personal-open-412.png`、`/private/tmp/md-kbd-personal-closed-840.png`と`/private/tmp/md-kbd-personal-open-840.png`のIME上端はそれぞれ同じ位置で、一覧の戻る矢印も候補行左に表示された。AVDの保存値は1件なので複数値の折り返しはRobolectric回帰テストと既存Viewテストで確認。物理端末は未確認。

[2026/10/30 07:34] コードSHA`21259bc`の`scripts/test-all.sh --parallel --connected`は初回・同一SHA再実行ともfast-checksとconnectedがpass、unit/lint/APK区分で`ImeHideBarTest`の絵文字header取得が1〜2件失敗。失敗クラス単独はpass。同一SHAで負荷を分けた`scripts/test-all.sh --connected`はfast-checks、unit/lint/APK、connected 32件の全区分pass（3/3）。判定はretry-passであり、並列時に絵文字header初期化の一時失敗が残る。AVDのsize/density上書きをresetし、検証後に停止した。Bitwarden本体・物理端末は引き続き未確認。human-review gateは継続し、ticket close、main merge、公開は行っていない。

[2026/10/30 09:02] 利用者の「アップロードして」をhuman-review用APKの共有依頼として扱い、Flyerへ24時間の公開ページ`https://vhduzh3k.aboutme.style/`を作成した。元APK`app/build/outputs/apk/debug/app-debug.apk`は39,179,434 bytes、SHA-256 `ac10502be8439901dc35138e337bbc4c00be907a09b891fa6f8a4f1a12c97615`、package `com.masuidrive.gestureime`、versionCode 49 / versionName 0.15.33、arm64-v8a。Flyerの20MiB/ファイル制限のため19,600,000 bytesと19,579,434 bytesに分割し、両方の保存後size/hashが元分割ファイルと一致。ページのボタンは分割2件と再結合APKのSHA-256をブラウザで検証してから`.apk`を保存する。Flyer site_getでpublic、24h、有効期限2026-10-31 00:02:01 UTC（JST 09:02）を確認。正式GitHub Releaseとticket closeは未実施。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
[2026/10/30 10:36] 利用者の最新指示に従い、住所の郵便番号・都道府県・市区町村・住所1・住所2は独立候補のままとし、キー領域の一覧も1件選んだ後に開いたままにした。実Androidエミュレータ840dpで東京都→千代田区を連続tapし、住所欄へ順に入力されて一覧が残る画面を確認。初回明示欄が空だったのはinline suggestionのinflate待機中に通常候補を隠す条件が原因と推定し、実体Viewがない間は候補を保つよう修正。再build/install後の初回住所欄で5個の独立候補を確認。focused ImeServiceEnglishSuggestionTest・PersonalInfoPickerViewTest・ImePreferencesTest pass。Web mockは同じ連続選択を412pxで操作し、東京都→千代田区を確認。画面記録: /private/tmp/md-kbd-address-after-prefecture-840.png、/private/tmp/md-kbd-address-after-city-840.png、/private/tmp/md-kbd-address-first-after-fix-840.png。
[2026/10/30 10:42] `6674af2`で`ANDROID_HOME=... scripts/test-all.sh --parallel --connected`を実行。fast-checksとAndroid connected（real Mozc）はpass。Android unit/lint/apk群は`SetupActivitySlashCommandsTest.setupSavesAndRestoresThreePersonalInfoFields`の旧3項目期待で1件失敗（359 tests中1 fail）。設定画面の対象項目が10へ増えたため、同テストを10項目の保存・再生成・削除検証へ更新し、focused testはpass。修正後の最終SHAで全体試験を再実行する。
[2026/10/30 10:51] reviewer `6674af2` はCritical 0、Major 2、Minor 1。旧3項目testを全10項目へ更新し、footerの独自24dp判定を`GestureInterpreter`共有へ変更。実`MotionEvent`でtap、割当方向、未割当方向、18dp閾値の内側、中心復帰、取消を確認した。Spaceのcursor移動は同じtrackpad解釈を使う。幅に応じた有効row pitchでfooter寸法を冪等に更新。修正前に別経路のEnter上下とSpace四方向はfocused test pass、修正後も同じtestと追加testがpass。`similarity-generic --language kotlin -t 0.7`は現行CLIがKotlin非対応で実行不可。全体suiteと再reviewは次。
[2026/10/30 10:57] 最終実装SHA `abe0a14b5a46c94b7c6331a5b3c78a42ce1d7a79`で`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel --connected`を実行。実出力は次のとおり。

```text
========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk
  PASS: android connected (real Mozc)

Passed: 3 / 3
```

同SHAで独立reviewerは前回Major 2件・Minor 1件の修正を確認し、Critical/Major/Minor残件なし。AC裏取りworkerはAC1〜5を全件VERIFIEDと判定。412dp/840dpのnative住所候補と連続入力画面は直前の`6674af2`で取得し、最終差分が触れるfooter gesture/寸法はattached Viewの実MotionEventとrow pitch/root高test、clean full suiteで補完した。Bitwarden本体・物理端末/Fold実機・Android 9〜15は未確認。mock正本はbyte一致。エミュレータは検証後に停止した。human review待ちにつきticket close・main merge・正式APK公開は未実施。
[2026/10/30 11:24] 利用者の「apkは？」を受け、最新レビュー用デバッグAPKをFlyerへ更新した。ローカル`app/build/outputs/apk/debug/app-debug.apk`は39186610 bytes、SHA-256 `84a827ce3d3ab1dcd911b4565de2a9cf922ccf85dc48fa5b3d90c4a9e0392ce5`、package `com.masuidrive.gestureime`、versionCode 49/versionName 0.15.33。Flyer `https://vhduzh3k.aboutme.style/`の20MB制限で前半19600000 bytes（SHA-256 `f69b80ad1920f548d911028c5ea554077fab8470cf2f425f39d889da50b79929`）と後半19586610 bytes（`a1d0d74bb16a1658a4855be8e8c703af3a3a70b9c33a168c22a6db9af704db61`）に分割。Flyer保存後の各size/hashは一致し、index.htmlは両ファイル名・全体byte数・全体SHA-256を参照。旧分割ファイルを削除し、site_getで公開・有効期限2026/10/31 11:24 JSTを確認。正式GitHub Releaseは未実施。

[2026/10/30 12:07] ユーザの新しい5点の指摘に対して `e8f89af` でAndroid実装・focused testを変更した。原因は `CandidateStripView.showCandidates` がinline View一覧を空にし、逆にinline Viewがあると `ImeService.showCandidateStrip` が登録候補の描画自体を止めていたこと。両方を同じ横列に描き、provider Viewは文字列化しない。姓名は保存値の空白を最大1回で分割し、空白が無い値は推測分割しない。pickerの左余白は3dpから10dpへ変更。左下キーのtapは通常レイヤーキー同様にQWERTYとかなを切り替え、上/右/下のflickで各レイヤーへ移る。`./gradlew :app:testDebugUnitTest --tests '*CandidateStripViewTest' --tests '*PersonalInfoPickerViewTest' --tests '*ImeServiceEnglishSuggestionTest' --offline` は70件pass。Android 16 AVDおよび最終full suiteは続けて確認する。公開中サイトとFlyerレビューAPKはこの版より古い。
[2026/10/30 12:22] Android 16 AVDの氏名欄で候補が空になる状態を再現した。保存値はNAME_LATIN=`Masui yuichiro`で、入力開始後の`invalidateConversion`が同期で候補を消した後、非同期の`resetConversion`でも新候補を消す競合が原因。同期clearだけを残し、非同期側は現行世代の変換engine resetと必要なcomposition cancelに限定した。resetを遅延させる回帰テストを追加。修正APKを再導入し、同欄で`Masui`と`yuichiro`が継続表示される画面`/private/tmp/md-personal-ime-fixed.png`、cardを開いた一覧で左余白・左下「あん」・2件の独立選択を`/private/tmp/md-personal-picker-fixed.png`に記録した。独立reviewで指摘された全角空白はUnicode区切り文字対応、SYMBOLSのtap戻り先はKANA、provider Viewと通常変換候補の優先関係は従来維持して個人情報候補だけ併記、設定画面の個人情報欄はAutofill対象外にした。focused unitは変更4クラスでpass。Bitwarden本体と物理端末は未確認。
[2026/10/30 12:35] 同SHA `cfcbc0b` の `test-all --parallel --connected` は2回ともfast-checks/connected passだが、`ImeHideBarTest`内の異なる絵文字表示testが各1件失敗。各単独はpass、同SHAの直列`test-all --connected`は3/3 pass。従ってtest区分はretry-pass。独立review2名はCritical/Major/Minor残件なし。AC verifierはAC1〜4をVERIFIED、AC5を現SHAでprotected inlineとのnative同時表示/tap未観測としてDEFERREDにした。
[2026/10/30 12:36] AC5追加実観察でAndroid 16 AVDにQA AutofillServiceを有効化し、設定画面へテスト用`qa@example.com`を登録。メール欄では保護QA Viewが360dp幅で候補行を占め、登録候補は右への横スクロール後に見えた (`/private/tmp/md-personal-email-scrolled.png`)。初期表示で両方を見せるため、登録メール候補併記時だけinline inflate幅を画面幅の半分相当に制限（120〜240dp、通常は360dp）し、再生成した候補Viewにも既存snapshotを描画。修正後の初期画面 `/private/tmp/md-personal-email-inline-coexist.png` に両方が同じ行で見える。登録候補tapでメール欄に`qa@example.com`、新規テスト画面のQA protected candidate tapでメール欄に`qa@example.test`とパスワード欄に値が入力されたことをUI階層で確認。QAサービスはBitwarden本体ではなく、物理端末も未確認。変更した`InlineSuggestionsTest`と`ImeServiceEnglishSuggestionTest`のfocused unit pass。最終全体試験と再reviewは次。
[2026/10/30 12:40] SHA`38289be`の再reviewでMajor 1、Minor 1。Majorはprotected Viewが複数の場合、すべてのprovider Viewの後に登録候補を置くと再び見えないこと。登録候補をcard直後へ移し、inline併記中はチップ表示だけ160dp上限と末尾省略にして、全文はaccessibility/確定値に保つ。複数provider View+長い登録メールの順序・幅・全文をfocused `CandidateStripViewTest`へ追加した。Minorは`onStartInputView`の重複描画分岐が既存`onCreateInputView`描画と重なり、追加テストもその分岐を証明しないこと。分岐とテストを削除した。Focused `CandidateStripViewTest`/`InlineSuggestionsTest` pass。前SHAの並列全体試験は絵文字表示test1件が失敗、同SHAの直列全体試験は3/3 pass。修正後の新SHAで全体試験を行う。
[2026/10/30 12:43] 最終順序のAndroid 16 AVD画面 `/private/tmp/md-personal-email-final.png` を確認。メール欄のカード、登録済み`qa@example.com`、QA Autofill保護候補が初期画面の同じ横列に見える。通常の候補はBitwardenと同じprotected Viewを使うQAサービスで確認したものであり、Bitwarden本体と物理端末の確認ではない。長い登録メールと保護候補2件はfocused UIテストで順序・表示幅・accessibility全文を確認。レビュー用Flyer APKは古いSHAのまま。
[2026/10/30 12:46] 最終実装SHA`0194e838a2c0bd3eab446393e8f55519048491d4`。`scripts/test-all.sh --parallel --connected`初回はANDROID_HOME未指定でAndroid区分が実行開始前に止まり、環境修正後はfast-checks・connectedがpass、unit/lint/apk区分が既知の`ImeHideBarTest.emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway`1件でfail。同SHA直列`test-all.sh --connected`はfast-checks、unit/lint/apk、connectedの3/3 pass。試験評価はretry-pass。独立reviewで前回MajorとMinorが解消、残件なし。AC verifierはAC1〜5全件VERIFIED。実provider複数の画面、Bitwarden本体、物理端末は未観測。人間レビューへ移る。
[2026/10/30 12:53] 最終デバッグAPK `app/build/outputs/apk/debug/app-debug.apk` は39186610 bytes、SHA-256 `fbf881952a1c54b7083fba7f309a08afa3b243fe3d85a29e7794b4f403aa4b33`。Flyer `https://vhduzh3k.aboutme.style/` へ前半19600000 bytes（SHA `66cd3257471ae3d6d5d8ddaf84f7240854268db2949af36482e37e14b119b56b`）と後半19586610 bytes（SHA `fccbffcd24e8e1ddd65b3acb3fbb9625a6ac7bf7578ee5033e749090ebead5ae`）をアップロードし、保存後の各size/hash、ページの新SHA参照、旧ファイル削除、公開期限2026/10/31 12:53 JSTをFlyer APIで確認。GitHub Releaseの正式公開ではない。判断ボード `/private/tmp/md-kbd-personal-info-review.md` は旧住所データのrollback制約と最終APKの入手先を反映し、独立読み手レビューで判断阻害4件解消済み。

[2026/10/30 14:14] ユーザの追加依頼をAC1〜3へ反映し、実装へ戻した。既存`personal_info_EMAIL`は1枠目として保持し、独立したメール2〜4枠を端末内保存へ追加。明示メール欄の自動候補は登録済み最大4件。カードで開くpickerはまずメールのみ、下段の「more..」で氏名・住所・定型文も表示する。戻る・editor切替時は展開状態と値を破棄。Setup画面、native view、browser mock、manual、native spec、technical-referenceを同期した。focused `ImeServiceEnglishSuggestionTest`・`SetupActivitySlashCommandsTest`・`PersonalInfoPickerViewTest`・`ImePreferencesTest`は`BUILD SUCCESSFUL`。モックはbrowserで412px/840px幅の初期メール4件、More後18件、開閉前後のキー領域210px、横はみ出しなしを確認。`cmp site/mock.html docs/reference/mock-source.html`一致。`similarity-generic`はKotlin非対応のため実行不可。エミュレータのnative観察と最終suiteは続行する。

### Findings (PDH-review-4, `ea9d395`)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | native / mock parity | Major | mockの個人情報footerが6列のまま7要素を配置し、Enterが2行目に回り得る。Moreだけ標準キーでない | 採用 | mockのMoreを標準keyにし、More表示時だけ7列へ変更。browser 412pxで全要素のy座標一致、210px固定、More後18件を確認。独立review再確認でCritical/Major残件なし。 |
| 2 | 空欄非表示 | Minor | 空白のみのメールを保存・候補表示できた | 採用 | メールを保存/読取時にtrimし、空白だけの値は削除。登録候補もblankを除外。focused `ImePreferencesTest`と独立review再確認で解消。 |

[2026/10/30 14:21] `ea9d395`で`ANDROID_HOME=... JAVA_HOME=... scripts/test-all.sh --parallel --connected`を実行した最終出力:
```
========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk
  PASS: android connected (real Mozc)

Passed: 3 / 3
```
レビュー修正後のfocused `ImePreferencesTest`・`ImeServiceEnglishSuggestionTest`は`BUILD SUCCESSFUL`。修正をcommitした最終コードSHAで全suiteを再実行する。

[2026/10/30 14:30] 最終コードSHA `ad2029219c20aa2e725bca3a1ce4b3fbd2ee0837`で`ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel --connected`を実行。fast-checks/connectedはpass、unit区分の`ImeHideBarTest` 2件（`emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway`、`privateEditorSwitchesToItsDedicatedEmptyPickerWithoutReusingPublicPicker`）が失敗し、同クラスの単独再実行はpass。同一SHAで負荷を分けた`scripts/test-all.sh --connected`の最終出力は次のとおり。

```text
========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk
  PASS: android connected (real Mozc)

Passed: 3 / 3
```

結果区分はretry-passとする。AC裏取りworkerは更新後AC1〜5を全件VERIFIED、独立reviewerはCritical/Major残件なし。Surface ObserverはAndroid 16 AVDの412dpでカード→メール4件のみ→more..→他項目の展開、明示メール候補と設定4枠を確認。840dpでは通常IME高さと設定4枠を確認したが、nativeのmore.. tap自体はADB座標操作で再現できず未観察（840dpのlayout testとbrowser mockは確認）。412dpのスクリーンショットは`/private/tmp/md-kbd-native-mail.png`、`/private/tmp/md-kbd-native-more.png`。エミュレータは検証後に停止した。Bitwarden本体・物理端末は未確認。最終のmockと保存正本はbyte一致、`git diff --check`もpass。人間レビュー待ちにつきticket close、main merge、公開は未実施。

[2026/10/30 16:27] 利用者の「テストしたい」を受け、最終SHA `a950b5a` のデバッグAPKを`ANDROID_HOME=... JAVA_HOME=... ./gradlew :app:assembleDebug --offline`で再ビルド（UP-TO-DATE、BUILD SUCCESSFUL）。ローカル`app/build/outputs/apk/debug/app-debug.apk`は39,187,042 bytes、SHA-256 `727b93951b63b34acef9b55a3c51faec8449317f412985d0b14408a951cc6081`、package `com.masuidrive.gestureime`、versionCode 49/versionName 0.15.33、arm64-v8a、permission `RECORD_AUDIO`。Flyer `https://vhduzh3k.aboutme.style/`へ前半19,600,000 bytes（SHA `ed668a1df6f9471fe1b74355627dd59ef21474cc6d31e2d858367c22b8b08e3d`）と後半19,587,042 bytes（SHA `887243ad2015034e79c8217e17025b46643c6cb849458699e8b3c0f97a0ebdaf`）をアップロード。保存後の各size/hashとindexの全体SHA参照、旧分割ファイル削除、public・24h・期限2026/10/31 16:26 JSTをFlyer APIで確認した。正式GitHub Releaseではなく人間レビュー用。

[2026/10/30 16:35] ユーザの下段フリック・Space幅・端余白の指摘を受けてPDH-implementへ戻した。既存実装は各キーのLayoutParamsが等幅でSpaceも1単位、親に左10dp/右3dpの非対称paddingと各キー左10dp marginが重なり、通常KeyboardViewの3dp/10dp content inset + 各キー左右3dpとは一致しなかった。Backspaceとmore..にはGestureInterpreter listener自体が無く、未割当方向へ動かすとclickにならない。変更前の反例はEnter上下フリック・Space4方向カーソル操作の既存focused test（pass）を採用する。

[2026/10/30 16:49] 下段の寸法を通常KeyboardViewの共通geometryに合わせ、全キーにGestureInterpreterを通した。Spaceを2単位、他のキーを1単位とし、more..非表示時も枠を保持してキー位置を安定させた。未割当方向は中央タップに戻し、hapticも発生させない。productionのPersonalInfoPickerViewにMotionEventを送るfocused test（レイヤー上、句読点左、Space右、Backspace/More未割当右、Enter上）と412/840dpの寸法testはpass。ブラウザmockは412/840 viewportでSpaceが隣の2倍、横はみ出し無しを確認。Android 16エミュレータの412dp/840dpでダミー登録した個人情報ピッカーを撮影し、下段の左右端とSpace幅を確認した（`/private/tmp/md-kbd-picker-native.png`、`/private/tmp/md-kbd-picker-wide.png`）。物理端末の確認ではない。`site/mock.html`と`docs/reference/mock-source.html`はbyte一致、`git diff --check` pass。similarity-genericはKotlin非対応、similarity-tsは対象JSファイル無しのため適用なし。

[2026/10/30 16:52] コードSHA `02e32a801800a2b1ab64097489a5ab7c5937aa77` に対する `scripts/test-all.sh --parallel --connected` はfast-checks、Android unit/lint/apk、connected(real Mozc)の3/3 PASS。今回差分の独立reviewerはCritical/Majorなし。AC裏取りworkerはAC3追加部分をVERIFIED（production MotionEvent、412/840dp寸法とnative画面、mock byte一致）。人間レビュー待ち。エミュレータとローカルサーバを停止した。

[2026/10/30 16:55] 更新版APK `app/build/outputs/apk/debug/app-debug.apk` は39,187,042 bytes、SHA-256 `33d8705c5d06fb1bdcf492b15166936c87824d406d38bc13dba6cdf9aa05b9d2`。Flyerの1日限定ページ差し替えを試みたが、Bearer upload keyをshell commandに含める方法が自動承認レビューで拒否された。理由はプロセス一覧やログへの秘密漏えいリスク。拒否された経路を迂回してアップロードしない。既存Flyerページは旧SHA `a950b5a` のAPKのまま。更新版はローカルAPKとして提示する。
[2026/10/30 17:07] 利用者の「一番下の記号も判定するけど記号でない」を受け、個人情報picker下段「、。？！」を調査。現行b0e971dを入れたAndroid 16 AVDで、840dpはタップ「、」→左「。」→上「？」→右「！」→下「、」が同一通常入力欄に順に入り、412dpも上「？」・右「！」・下「、」が入った。スクリーンショット `/private/tmp/md-kbd-punct-all.png`、`/private/tmp/md-kbd-punct-phone-rightdown.png`。IME service + production PersonalInfoPickerViewへのMotionEvent左フリック統合テストを追加しfocused test pass（未commit）。Web mockの個人情報一覧でもタップ「、」・左フリック「。」が入力欄へ入った。現状は同じ不具合を再現できず。Flyer試用APKは旧a950b5aで、最新b0e971dはアップロード未完了。発生画面・実際のAPKを確認して継続する。
[2026/10/30 17:11] 利用者は発生箇所を個人情報一覧と登録キーワード一覧と特定。両者は同じPersonalInfoPickerView。登録キーワードを表示した状態で句読点キーの左右上下MotionEvent→ImeService→RecordingConnectionの入力を確認する新しいfocused test `pickerPunctuationFlickCommitsTheSelectedCharacterWithSnippetsVisible` を実行しPASS。現行コードで再現できないため、実端末上の具体的なフリック方向・入力結果の回答待ち。テスト変更は未commit。
[2026/10/30 17:26] 利用者が「入力はできるが十字UIが出ない」と補足。原因はPersonalInfoPickerViewの下段句読点キーが入力のフリックactionだけ実装し、KeyboardPopupControllerへ接続していなかったこと。音声レイヤーのvoice-punct KeySpecを共通popupへ渡し、DOWNで中央表示、方向変更で選択表示、UP/CANCEL/DETACHで閉じるようにした。focused Robolectricはpopupの5方向表示・選択・解除と、登録キーワード一覧からIME serviceまでの上下左右入力がPASS。Android 16 AVDの412dpと840dpでもpopup表示と位置をスクリーンショット `/private/tmp/md-kbd-popup-live.png`、`/private/tmp/md-kbd-popup-up.png`、`/private/tmp/md-kbd-popup-wide.png` で確認し、選択文字が入力欄へ確定した。実機確認は未実施。mockは既に十字表示を持ち、保存元とbyte一致。
[2026/10/30 18:05] コードSHA `52e631b32adbb7ae0601336da9533aa469193e96` の `scripts/test-all.sh --parallel --connected` は summary `PASS: fast-checks` / `PASS: android unit, lint, apk` / `PASS: android connected (real Mozc)` / `Passed: 3 / 3`。独立reviewerはCritical/Majorなし。エミュレータ停止済み。
[2026/10/30 18:05] 利用者のアップロード依頼に従い、既存のFlyer試用ページ `https://vhduzh3k.aboutme.style/` のAPKを修正版へ切り替えた。APKは39,187,042 bytes、SHA-256 `46a1eab1949e3ac68e64efb8c76c89af9603408cb829533d8e8171be74fba547`。Flyerの20MiB制限に合わせ2分割で転送し、返却SHA-256は両分割ともローカルと一致。ページは2ファイルを取得・結合し全体SHA-256検証後に直接 `.apk` として保存する。旧a950b5aの分割2ファイルは削除し、Flyerのファイル一覧とページの新SHA参照を確認。upload keyはcurl argv・ログへ置かず、標準入力のheaderとして渡した。公開GitHub Releaseではなく、人間レビュー用の1日限定試用ページ。
[2026/10/30 18:13] 利用者の「とりあえずこれでよいよ」をレビュー承認として受領。公開版をv0.15.34/versionCode 50へ上げ、製品ページのリンクとmanualから次期プレビュー表記を外して本機能の説明へ更新する。主checkoutは別feature branchで未commit変更を持つため、それを変更せず別checkoutでticket closeを進める。
[2026/10/30 18:18] v0.15.34準備SHA `6f3fabc184a31a9cc936367d25e659e4d950ce85` の `scripts/test-all.sh --parallel --connected` 初回summaryは `PASS: fast-checks`、`FAIL: android unit, lint, apk`、`FAIL: android connected (real Mozc)`、`Passed: 1 / 3`。並列Gradleのunit compileで多数のunresolved referenceが出て、接続試験は起動直後のEmojiPickerBodyLayoutInstrumentedTestで32件中2件目に中断。順番に再実行するとunit 372件中1件 `ImeHideBarTest.emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway` がRobolectricのheader View nullで失敗。同テストを単独でも再現し、変更前SHA `52e631b` の独立worktreeで同一testを実行して同じline 572で失敗することを確認した。これは今回の版番号・文書差分による退行ではない。起動完了後の `./gradlew connectedDebugAndroidTest` は32/32 PASS、`./gradlew lintDebug assembleDebug` もPASS。unit全体は未greenであり、base側と同じ既存失敗として区別して扱う。v0.15.34 debug APKは38,811,506 bytes、SHA-256 `31fe137ae3e3f4d358832d71191efbaf69827ca944fb28e31f0c1522bd575b91`、package `com.masuidrive.gestureime`、versionCode 50、arm64-v8a、マイク権限とAndroid生成のアプリ内receiver権限のみ。エミュレータ停止済み。
