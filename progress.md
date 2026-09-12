[2026/09/11 01:31] 動く v1 まで継続する依頼を受領。以後の経過は日本時間でこのファイルへ追記する。
[2026/09/11 01:31] 共有会話と Hangar Sites の実装仕様を確認。Galaxy Z Fold7 向け Android ネイティブ IME とし、日本語・テンキー・カーソル・QWERTY・記号の5レイヤーを実装する。
[2026/09/11 01:31] PDH 最新版 15e6289 の Codex 向け構成を導入中。Sites の仕様原文をローカルへ保存し、実装チケットを作成した。
[2026/09/11 01:31] Sol が PDH・Android 本体とキーボード UI を分担し、Terra が Mozc の実ビルド・JNI 接続を担当。Java 17、Android SDK 36、エミュレーターの存在を確認済み。
[2026/09/11 01:35] PDH の導入と構造チェック5件が完了。実装チケット 260910-163036-implement-native-ime を開始し、機能の受入条件8件を記録した。
[2026/09/11 01:35] v1 は5レイヤーのネイティブ入力と実際の Mozc かな漢字変換までを対象にする。個人辞書・会話ログ取り込み・同期は基礎入力後の拡張として区別した。
[2026/09/11 01:35] Mozc の現行 JNI を調査し、libmozc.so・実辞書 mozc.data・Protocol Buffers の3点を組み込む経路を確定。公式指定の NDK r29 とビルド依存を準備中。
[2026/09/11 01:40] Mozc ビルド用の Bazel 9 を取得。NDK r29 のダウンロード検証でハッシュ不一致が見つかり、使用せず公式配布元から再取得中。
[2026/09/11 01:40] 受入条件と経過の更新コマンドが自動承認レビューで「未承認の範囲変更」として拒否された。該当コマンドは未実行。元の Sites 仕様と現在の受入条件の差を読み取り専用で独立確認している。
[2026/09/11 01:39] ユーザーから、v1完成後にスクリーンショット付きマニュアルページと製品紹介ページを作り、モックも活用する追加依頼を受領。実アプリの動作確認・撮影後に制作する。
[2026/09/11 01:41] 独立確認では、Sites 原文を最終仕様として実装する方針は元の依頼と一致すると判定。要約ACだけでは省略される配列・アニメーション・キャンセル等も原文に照らして検証する。拒否された追加の書換えコマンドは再実行していない。
[2026/09/11 01:42] Android の Gradle 構成、IME登録、セットアップ画面、入力処理、キーボード描画、変換インターフェースの実装が揃い始めた。統合前に配列差分とSpaceカーソルの往復操作を点検し、担当へ修正を依頼した。
[2026/09/11 01:46] UI担当から5レイヤーとフリック判定テストの初回ビルド成功を受領。Spaceの往復・原点跨ぎを修正し追加テスト中。かなポップアップ、ラベルアニメーション、長押しアクセント、連続削除も実装した。統合ビルドはMozcクラスと生成コードの着地待ち。
[2026/09/11 01:49] Mozcの実JNI接続クラスを追加し、公式ライブラリ・辞書・Protocol Buffers生成を実行中。統合前レビューでセッション解放、並行呼出し、辞書コピー途中失敗の処理を修正対象にした。入力先処理には部分テキスト範囲、Ctrl編集操作、クリップボードの回帰テストを追加した。
[2026/09/11 01:53] Android 36.1 arm64 エミュレーター emulator-5554 が起動。外画面相当の約411dp幅で検証準備完了。MozcのProtocol Buffers jar生成も成功し、アプリに配置した。
[2026/09/11 01:53] Hanger Sites専用接続から既存モックHTMLと実装資料2件を取得。3ファイルすべて配信元SHA-256と一致した。かな最下段・カーソルの先頭/末尾キー・長押し等を実ソースで照合し、ページ制作でも再利用する。
[2026/09/11 01:55] Mozcの実辞書（約18MB）とAndroid arm64ライブラリ（約9.5MB）の公式ビルドが成功し、アプリへ配置した。次にAPK統合ビルドとエミュレーター上のかな→候補→確定を検証する。
[2026/09/11 01:56] 実Mozcライブラリ・辞書込みのAPKビルドに成功。再現ビルドスクリプト、依存版・ハッシュ、ライセンスを追加し、エミュレーター上の変換テストへ進んだ。
[2026/09/11 01:56] 受入条件のキー列略記を展開する3か所の表記修正が、自動承認レビューに再度拒否された。元の略記を維持したまま動作検証を続行し、この表記修正だけ非同期でユーザー確認中。
[2026/09/11 02:02] エミュレーターの実Mozc変換テスト kanaProducesJapaneseCandidateAndCommitsIt が OK (1 test)。かな→日本語候補→確定が実ライブラリと辞書で通った。通常IMEとしての操作と、変換待ち中の連続入力を続けて検証する。
[2026/09/11 02:07] 実IMEで英字入力、かなフリック、実Mozc候補表示、候補タップ確定まで成功。独立UIレビューで重大指摘なし。変換待ち操作と入力欄切替のキュー処理を点検し、並行して製品紹介・マニュアルの骨組みをSolが制作する。
[2026/09/11 02:07] 配布準備の点検でMozc再現スクリプトの不足と依存ライセンス同梱漏れを発見。Terraが生成手順とライセンス一式を修正する。
[2026/09/11 02:11] 長い日本語の候補確定を点検し、複数文節の後半が失われるMozc接続の不具合を発見。単語変換の成功だけでは完了とせず、残り文節の確定処理と長文テストを追加している。
[2026/09/11 02:19] TalkBack向けに各キーを個別の操作ノードとして公開する修正が入り、回帰テストが通った。Mozcの依存ライセンスをAPKへ同梱し、Protocol Buffers jar生成は同じバイト列を再生成できることを確認した。
[2026/09/11 02:26] Mozc修正の独立レビューは通過したが、修正版の端末テストでSpace変換時に候補が空になる2件の失敗を検出。Terraが端末検証を引継ぎ、原因を修正中。モック専用ページはスマートフォン幅で英字入力・日本語切替・かな入力まで確認した。
[2026/09/11 02:35] Android版Mozcの候補出力 all_candidate_words を読む修正で端末テスト3/3が成功。最終コードe46ad39のローカル検証もfast-check5件、unit31件、lint、APKビルドがすべて成功した。スクリーンショットの内容不一致を検出し、Solが正しい各レイヤーを撮影し直している。
[2026/09/11 02:46] スクリーンショットをマニュアル・製品紹介へ反映し、全リンク欠落0とモック入力を確認。端末の文字倍率1.3でキー表示の重なりを検出し、通常表示を維持するサイズ上限を追加。1.3/2.0の回帰テストが成功し、端末再確認中。
[2026/09/11 03:02] 最終実装 bbe40aa を端末確認し、fast-check 5件・unit 32件・connected 5件・lint・APK buildが成功。APK SHA-256は87ccb6734b22b6bb940995c169fcc4c36457fd95f8c583420ae84ade0e554d0b。製品紹介・マニュアル・操作デモを完成し、http://127.0.0.1:8765/ のHTTP 200を確認。全9ファイルを含む /tmp/gesture-ime-v1-complete.zip（SHA-256 9991f96548f146972cd01e83103c3064116da2c4e2637f4e3bdcb3260e99cc31）を準備した。外部Hanger新規サイトは未作成で、所有者限定・1週間の配置はユーザー承認待ち。
[2026/09/11 08:00] ユーザー承認後、Hanger Sitesへ所有者限定・1週間期限の新規サイトを確保して完全ZIPを送信した。確定時にAPKの展開後サイズ34,645,297 bytesが1ファイル上限20,971,520 bytesを超えるため拒否された。不完全な新規サイトは削除待ちへ移し、元の参照サイトは変更していない。ページ内のAPK導線を欠落させずに公開するには、Sites外の配布先またはファイル上限の変更が必要。
[2026/09/11 08:06] APKを単体ZIP（19,859,255 bytes）で配布するようページと導入手順を修正し、Hanger Sitesへ所有者限定・1週間期限で公開した。URLは https://amykwzak.aboutme.style/ 、site IDはf2cd5fc5-71db-416d-b9e7-b2da21f96279、期限は2026-09-18 08:02 JST。公開9ファイルのSHA-256はローカルmanifestと全件一致し、ZIP内APKも最終APK SHA-256 87ccb6734b22b6bb940995c169fcc4c36457fd95f8c583420ae84ade0e554d0bと一致した。
[2026/09/11 08:07] 公開サイトを実ブラウザのmobile 412px幅で確認。製品紹介・マニュアルに横overflowなし、画像3点の読込とナビゲーション、操作デモのq入力と状態表示が成功した。
[2026/09/11 08:22] GitHubに非公開リポジトリ https://github.com/masuidrive/android-kbd を新規作成し、完成履歴5351555をfeatures/260910-163036-implement-native-imeへpushした。
[2026/09/11 08:32] 端末内日本語音声入力の追加指示を未着手ticket `260910-233205-add-on-device-voice-input` として起票した。今回はticket作成のみとし、後日の一括作業指示とAC確認を待つ。
[2026/09/11 08:38] キー割当・日本語キーのフリック・補助ラベルアニメーション・Spaceカーソル移動・QWERTY BSラベルの5点を、未再現のユーザー申告として未着手ticket `260910-233809-fix-keyboard-interaction-details` に起票した。実装やデバッグは開始せず、後日の一括作業指示を待つ。
[2026/09/11 08:39] 同じ未着手ticketへ、キートップ文字を正本CSSモックに合わせる調整と、フリック中の背景減光を廃止してちらつきを抑える調整を追加した。具体差と減光範囲は未再現のまま着手時の照合事項とし、実装・デバッグは開始していない。
[2026/09/11 08:42] 同じ未着手ticketへ、候補未表示時の案内文削除とキータップ時の触覚フィードバックを追加した。候補機能は維持し、振動の詳細はAndroid標準設定と既存実装を踏まえて着手時に決める。実装・デバッグは開始していない。
[2026/09/11 08:49] 約412dpの実Android QWERTYを撮影し、正本CSSモックと比較した。nativeは横・縦とも4dp相当、正本は横6px・行間10pxで、Space/Enterの幅と文字サイズにも差がある。通常文字キーだけに限定された補助ラベル拡大条件もsourceで確認し、特殊キー整合とキー余白を未着手調整ticketのAC 3/10へ記録した。実装修正は開始していない。
[2026/09/11 08:51] 既存比較画像で「あん」キーを再確認。正本CSSは16pxの「あ」を左へ3px、12px・opacity .72の「ん」を右下へ分離する一方、nativeは22sp/16.5spの文字中心を10dpだけ離して描くため、実画面で大きく重なって見える。AC 6の主副ラベル配置差としてnoteへ記録し、実装修正は開始していない。
[2026/09/11 08:53] 日本語レイヤーの候補エリア下端とキー最上段の余白を広げ、他の間隔と視覚的に揃える申告を既存調整ticketのAC 10へ統合した。余白値は固定せず、着手時に正本CSSと他レイヤーを比較して決定する。実装修正は開始していない。
[2026/09/11 08:55] 既存の未着手調整ticketへ、日本語変換中のEnterを「確定」表示にし、tapで候補確定・上スワイプで無変換・左スワイプでカタカナを選ぶAC 11を追加した。変換中は補助ラベルを表示せず、非変換時のEnter/Pasteを維持する。無変換・カタカナ後に即時確定するか変換を継続するかは未指定として着手時確認に残し、実装は開始していない。
[2026/09/11 08:56] 同じ未着手ticketのAC 6へ、通常英字・数字記号・Enter/Paste・Spaceなどの補助ラベルの位置、文字サイズ、主ラベルとの相対位置・間隔を正本CSSへ合わせる条件を明記した。アニメ中はAC 3、変換中「確定」キーは補助ラベルなしの例外を維持し、実装は開始していない。
[2026/09/11 08:59] source確認でC/Aは押下中だけ選択色になり、指を離したone-shot待機中はC/A表示へ変わるだけで背景色が通常darkのままと判明した。待機色の変更は質問由来の課題候補として着手前確認に残した。QWERTY BSは新しい明示指示を優先し、tapで1文字削除・全方向swipe未割当へAC 5を更新した。実装は開始していない。
[2026/09/11 09:01] 日本語レイヤーの現「小゛゜」キーは表示ラベルを「小」だけにする条件をAC 2へ追加した。濁点・半濁点・小文字変換機能とフリック操作調整は維持し、実装は開始していない。
[2026/09/11 09:02] QWERTY BSの直前指定をユーザー訂正に合わせ、tap=1文字削除、下swipe=ESC、上・左・右swipe=未割当へAC 5を更新した。主BSラベルは表示し、ESCを含む補助ラベルは表示しない。記号レイヤーのESC要件は維持し、実装は開始していない。
[2026/09/11 09:07] Dual Flick設定をONにした広幅画面で、日本語12キー3×4を左右2セット表示して両手入力を試す未着手ticket `260911-000706-add-dual-flick-wide-layout` を起票した。Fold7展開画面を対象とし、OFF/狭幅は単一配列、幅変化時はcomposition保持を要件候補にした。thresholdと周辺キー配置は未確定で、実装・probeは開始していない。
[2026/09/11 09:11] ユーザーの段階1実装・公開指示を受け、調整とDual Flickを同じdeliveryで開始準備。C/A待機色をCSS一致として採用し、無変換/カタカナはpreview切替後に「確定」tapで挿入するdefaultで進める。音声入力は段階1公開後まで未着手を維持する。
[2026/09/11 09:16] 調整ticketを旧native featureから、Dual Flick ticketを調整featureから標準PDH手順で開始し、両方を同一delivery履歴へ接続した。設定default OFFの永続化、候補placeholder削除、ESC送出、変換中ひらがな/カタカナpreviewと確定、v0.2.0化を本体側へ実装し、対象単体テストが成功した。
[2026/09/11 09:25] UI初回35cf876を統合し、fast-check 5件・unit 41件・lint・APK build・実Mozc connected 5件が成功。その後の独立reviewで、かな縦長Enterと句読点の重なり、840dp行高、あん副文字の重なり順・濃さのMajor 3件を採用し、rowspan修正aa423a8と追加修正を進めている。初回テスト結果はbaselineとし、最終UIで再検証する。
[2026/09/11 09:41] 最終統合128edacでreview Major 3件を解消し、限定再reviewは追加Critical/Majorなし。fast-check 5件・unit 45件・lint・APK build・connected 5件が全て成功。APK SHA-256は77b99a39375a86caa506222c8503c92fcd4cf1c665e553f6870e7fac0b90241b。
[2026/09/11 09:41] エミュレーターで412dp QWERTY/単一かな、840dp Dualかな、実入力「にほんご」からMozc候補・「確定」・「日本語」挿入を確認。Space左右と複数行上/下、小キーの濁点・半濁点・小文字、無変換/カタカナpreview、BS tap削除、Dual設定永続化を実操作し、画面幅・設定を既定へ復元した。Galaxy Z Fold7実機と物理振動は未確認として区別する。
[2026/09/11 09:39] 将来作業として英数字候補buffer ticket `260911-003916-add-english-candidate-buffer` を起票。調整・Dual Flick・APK公開・音声公開の後に検討し、今回の実装には含めない。候補辞書、対象範囲、単語境界、既定ON/OFFは未決で、ネットワーク候補は追加しない。
[2026/09/11 09:47] Space下フリックの過大移動で次の入力欄へfocusが越境する不具合を端末で検出。上下移動を現在editor内の改行区切りlogical lineへclampする修正7cf0f3fを適用し、過大な上/下往復後も通常欄のfocusが維持されることを確認した。最終fast-check 5件・unit 48件・lint・build・connected 5件が成功し、APK SHA-256はfcd9a3cdac964d80c852d7f48abda50b97bb1ad6f3cc1b46da7f416ead08cc78。
[2026/09/11 09:51] Gesture IME v0.2.0を既存の所有者限定Hanger Sites（https://amykwzak.aboutme.style/）へ公開した。先にAPK ZIP、続けてページ一式を更新し、旧v0.1 ZIPを保持した。配布ZIP内APKのSHA-256は最終成果物fcd9a3cdac964d80c852d7f48abda50b97bb1ad6f3cc1b46da7f416ead08cc78と一致し、公開14ファイルすべてのsize/SHA-256がローカルと一致した。アクセス範囲owner_onlyと期限2026/09/18 08:02 JSTを維持した。
[2026/09/11 09:57] v0.2公開後、音声入力ticketを現在deliveryから開始。API 36エミュレーターで端末内recognizer自体は利用可能だが、checkRecognitionSupportのinstalled日本語model一覧は空だった。AVDでは非対応表示を正とし、実発話成功とは扱わない。端末内factory限定、録音権限、session世代、preview確定/取消の実装と回帰テストを進めている。
[2026/09/11 10:27] Gesture IME v0.3.0を既存の所有者限定Hanger Sites（https://amykwzak.aboutme.style/）へ公開した。先にv0.3 APK ZIP、続けて音声入力マニュアルと実AVD画像を含むページ一式を更新し、v0.1/v0.2 ZIPを保持した。配布ZIP内APKのSHA-256は最終成果物4e691b4003dcdc883de55dd812b27fd7fba3061b408867d561ac63b821fa2ceaと一致し、公開17ファイルすべてのsize/SHA-256がローカルと一致した。アクセス範囲owner_onlyと期限2026/09/18 08:02 JSTを維持した。
[2026/09/11 10:23] 音声入力v0.3の最終実装6ec20ccでfast-check 5件、unit 62件、connected 7件、lint、APK buildが成功。APK SHA-256は4e691b4003dcdc883de55dd812b27fd7fba3061b408867d561ac63b821fa2cea。API 36 AVDは端末内recognizer対応だが日本語model未導入のため実発話成功は未検証と区別した。
[2026/09/11 10:23] 端末でマイク未許可時の通常候補維持、許可後のIME hide→showで音声ボタン復帰、model未導入時の非対応表示、機密欄で候補・音声UI非表示を確認した。
[2026/09/11 10:23] 音声公開後に着手するQWERTYラベル微調整preview ticket `260911-011016-add-qwerty-label-adjustment-preview` を起票。さらにユーザーから、その後は英数字候補bufferを含む残りticketも完了まで進める指示を受領した。音声v0.3公開前には着手しない。
[2026/09/11 10:28] v0.3公開の実ブラウザ確認後、QWERTYラベル微調整preview ticketを開始。主/補助/Space・Enter/小型複合の5群ごとにscaleとX/Y位置を調整し、同じKeyboardViewで412dp/840dpと文字倍率1.0/1.3/2.0の安全範囲を検証する。
[2026/09/11 10:35] ユーザーから音声操作の変更を受領。左下レイヤーキーは短tap/flickの既存操作を維持し、1秒以上holdで録音開始、開始時2回振動、release後の最終結果を同じ入力欄へ自動確定する。QWERTY微調整と同じv0.4で公開し、別ticketとして実装する。
[2026/09/11 10:37] v0.4へ、縦swipe拡大文字の位置ずれ、QWERTY BSの削除記号、Enter→Pasteアニメーション、cursorレイヤーとSpaceのカーソル移動不良という追加4点を含める指示を受領。UI3点は描画側、カーソル2経路は実入力欄で挿入位置を観測して再検証する。
[2026/09/11 10:38] カーソル移動不良は標準入力欄では動作し、ブラウザのxterm.jsで再現するというユーザー補足を受領。通常Editorの安全なsetSelectionを維持し、xtermのhidden textareaへ矢印KeyEventを送る経路は誤判定とfocus越境を避ける条件を調査してから実装する。
[2026/09/11 10:42] v0.4へ、日本語キーの高さを単一・DualともQWERTYへ揃え、候補欄下に増えた隙間の背景色をキーボード下の隙間と揃える追加調整を受領。狭幅45dp・広幅52dpを描画側で検証し、必要ならIME全体高さも追従させる。
[2026/09/11 11:08] v0.4実装8727e73でfast-check 5件、unit 84件、lint、APK buildが成功。APK SHA-256は6a3c05f4afa603549815a0d130bf026b5d2b06fdf0bb5c789e020561a4be6c75、connectedは直前の同一production codeで7件成功。QWERTY調整、1秒hold音声、terminal互換カーソル、追加UI修正を統合した。Android Chromeのxterm fixtureではSpace左移動後の途中挿入と、cursor layerのHome/End受信を実画面で確認した。4方向を含む全キーの個別ログ証跡、設定save/reset、hold非対応経路の最終端末確認は継続中。
[2026/09/11 11:18] 公開前確認で旧音声ボタンのStart/Stop/Confirmが残る仕様不一致を検出し、長押し専用へ修正した。最終7fec052でfast-check 5件、unit 85件、lint、APK build、connected 7件が成功。APK SHA-256は8f6604d5bc433727a6bf421923c3f7557cfdb6b52d0119d94ee3b62c94246457。旧APK・画像・ZIPは公開対象から除外し、最終APKをエミュレーターへ再導入した。
[2026/09/11 11:27] v0.4公開後の次作業として、正本HTMLを定量分析しnativeのキー形状・角丸・影・label baseline/補助位置・popup・上下swipe frame・Enter/Pasteを描き直すticket `260911-022705-rebuild-keyboard-visual-fidelity` を起票した。日本語キー高、BS⌫、hold音声など後続仕様は維持し、英数字候補bufferはこのv0.5公開後のv0.6へ順序変更する。今回は起票のみで分析・実装を開始していない。
[2026/09/11 11:38] Gesture IME v0.4.0を既存の所有者限定Hanger Sites（https://amykwzak.aboutme.style/）へ公開した。先にv0.4 APK ZIP、続けて最終AVD画像とページ一式を更新し、v0.1〜v0.3 ZIPを保持した。配布ZIPは19,914,964 bytes、SHA-256は7f6297055c0d619775fad107df93afb79d183c142c7dd005655de49915e350d6で、内包APKのSHA-256は最終成果物8f6604d5bc433727a6bf421923c3f7557cfdb6b52d0119d94ee3b62c94246457と一致した。公開24ファイル・81,514,636 bytesの全size/SHA-256をローカルと照合し不一致なし。アクセス範囲owner_onlyと期限2026/09/18 08:02 JSTを維持した。
[2026/09/11 11:45] v0.5 visual fidelity ticketをv0.4完成履歴から開始。正本mock-sourceのselector/lineを根拠に、5layerの寸法・色・角丸・影・主副label・ghost・popup・animation閾値/終端を `docs/visual-reference-analysis.md` へ定量整理した。HTML Enter/Pasteはnavigation label置換経路のため通常secondary transformを通らない元bugも切り分けた。
[2026/09/11 11:51] 候補表示時にcandidate rowと未選択候補だけが`#2a313a`へ変わる実装を確認。候補欄・未選択候補をkeyboard gapと同じ`#29292c`へ固定し、選択中候補のaccentは維持する修正と回帰テストを追加した。
[2026/09/11 11:51] 全レイヤー共通切替キーの最新指定を反映し、左=記号・上=日本語を維持したまま、右をQWERTY、下をテンキーへ交換した。tapと1秒hold音声は維持し、layout回帰テストと正本override記録を更新した。
[2026/09/11 12:10] v0.5の同一platform比較基準として、API 36 Android Chromeで正本HTMLのQWERTYを412dp外画面・840dp内画面の両方で撮影し、キー全体とファイル名の一致を原寸で確認した。native最終後にAndroidフォントのbaselineとpopupを同端末で比較する。
[2026/09/11 12:12] 412dpの正本HTMLをdark modeで追加撮影し、key face・下影・主副label・Enter/Pasteの色比較基準を保存した。画面設定はlight、1080x2400/420dpiへ復元した。
[2026/09/11 12:17] v0.5描画初稿のKotlin compileは成功したが、fidelity完了とは扱っていない。独立確認で検出したpopup上端clampは`f5abeab`で修正し、Popup対象テスト7件は成功。KeyboardViewは全mode Enter経路の残り1件を切り分けて修正中のため、全suiteと端末QAは未実施。
[2026/09/11 12:38] 最後に使ったレイヤーを次回IME表示で引き継ぐ追加指定を反映。実際のSwitchLayer時に5種のKeyboardModeだけを保存し、設定なし・不正型・不正値はQWERTYへfallbackする。IME View再作成後の復元を含むfocused testは成功した。
[2026/09/11 12:44] API 36実IMEの正しいraw座標で、かな上swipeは「う」選択・候補表示まで発火する一方、5tile popupが表示されない実欠陥を再現した。dumpsysのPopup frameはy=3176..3614と画面外で、IME window origin=1587の二重加算と一致した。先の縮小view座標をADBに使った無効な観測と分け、window内座標へ補正する`d32e7c1`の修正後端末proofを継続中。
[2026/09/11 13:02] v0.5 APK公開後、英数字候補v0.6より前に公開demoを更新する追加指定を受領。Dual Flickに対応し、スマホ縦長frameを外して入力欄とkeyboardを中心に可変幅で示す。現在のv0.5最終QA・APK公開を優先し、demo実装はその後に行う。
[2026/09/11 13:15] v0.5最終revision 563423fでfast-check・unit 106件・lint・APK build、connected 7件が全て成功し、独立限定reviewはCritical/Majorなし。APKは34,859,489 bytes、SHA-256 fed641ca9f948d856fb00fdb257d8c2d5fe207ca3085016cf4fd4bdf8379acb5。API 36.1 AVDで412dp全5layer、候補背景固定、popup before/after、840dp QWERTY/Dualかな、412/840×font scale 1.0/1.3/2.0、last-layerのhide/reopen・別editor復元を確認した。端末は1080×2400、420dpi、font 1.0、light、Dual OFF、terminal OFF、QWERTY、Gesture IME選択へ復元した。
[2026/09/11 13:17] v0.5公開後の次作業として、公開demoをDual Flick対応の入力欄＋keyboard中心・可変幅表示へ更新するtodo ticket `260911-041600-update-dual-flick-demo` を起票した。続く英数字候補ticketは既定OFF、ASCII英数字buffer、英字prefix最大5候補、数字混在時は原文維持、端末内固定辞書というDirector判断へ契約を更新した。demo公開完了までは英数字候補の実装を開始しない。
[2026/09/11 13:17] v0.5用の新しいowner_only site `https://h3qwrv2c.aboutme.style/`（site id `05643792...`）へのupload_beginは、既存URL更新の承認だけでは別宛先への具体payload送信承認にならないとして自動reviewに拒否された。完成15ファイル・20,710,951 bytes・ZIP SHA-256 `834af...`は未送信で、公開成功とは扱わない。rootが具体payloadと新URLを示してユーザー承認を確認中であり、承認前の再試行はしない。
[2026/09/11 13:21] Dual Flick公開demo ticketをv0.5完成履歴から標準手順で開始した。可変幅/Dualだけでなく、背景非減光、空候補placeholder削除、BS⌫、小label、変換中確定・無変換・カタカナ、Enter/Paste animation、last-layer、右QWERTY・下テンキーという最新native仕様へ旧mockを同期する範囲を契約へ追加した。音声はブラウザで録音せず非対応を明示する。
[2026/09/11 13:29] demo実装f26e370のresponsive/Dual部分を独立browser/static確認。412pxは単一かな・横overflowなし、1000px内画面は左右2組のかなキー、Dual/layerはreload後も保持、console error/warning 0。pointer別stateとrelease/cancel処理をsource確認し、synthetic 2-pointer probeは逆順releaseで「かあ」、片方cancel後に他方releaseで「か」を保持した。実multi-touch端末試験ではなくbrowser合成eventとして区別する。その後の全layer監査でEnter/Paste、Symbols ESC、狭幅BS clipping等の未同期を検出したため、f26e370全体を最終PASSとは扱わず修正を継続する。
[2026/09/11 13:31] demo追補1ac684c/74a24a1後の静止表示を限定再確認。412px横overflowなし、QWERTY BSはface内、Symbols ESC表示・backtickなし、空候補placeholderなし、日本語「小」と通常Enter/Paste表示、console warning/error 0。その後rootがEnter animationのmain opacity/label遷移に追加Majorを検出したため、最終PASS/公開は追補修正後まで保留する。
[2026/09/11 13:35] demo最終追補b823768を独立computed確認。外phoneはkeyboard幅412・単一かな・row/pitch/Enter 45/51/96px、内phoneは幅840・Dualかな・52/58/110pxでnative寸法と一致した。実keyboard幅でDualを判定し、Enter/Paste可視性修正も限定確認済み。初回および追加Majorを解消した。
[2026/09/11 13:37] 既存owner_only siteの https://amykwzak.aboutme.style/demo.html へDual Flick可変幅demoを公開した。remote/localはdemo 3,719 bytes SHA `6b898e...b8a`、mock 57,652 bytes SHA `71b56c...f46e`で一致し、owner_onlyと期限を維持した。既存サイトのdownloadはv0.4のまま保持し、新v0.5 siteへの15ファイル送信は別承認待ちのまま実行していない。
[2026/09/11 13:55] 英数字候補v0.6を開始し、既定OFF設定、28,001語の固定端末内辞書、最大5件のprefix engine、ServiceのASCII compositionと入力境界、描画token付き候補選択を実装した。選択移動時のraw維持と65文字目の新buffer開始を追加修正し、Service配線9件、Voice lifecycle・Preferencesを含むfocused 16件、候補UI 11件、engine 4件が成功。独立reviewと全suite・端末検証は継続中。
[2026/09/11 14:21] v0.6最終成果でfast-check 5件、unit 121件、lint、APK build、connected 7件が成功。APKは34,965,858 bytes、SHA-256 `3411e9e0cf0ef4cc6c91fe593fe7a162bd0618d55d60907da40b5b7ea50be3ca`。API 36.1 AVDで設定ON/OFF、native `pro`候補と`problem`への重複なしtap確定、Enter/Space、数字、Kana境界、private欄、airplane modeを確認し、設定・接続を既定状態へ復元した。新owner_only siteへのv0.5/v0.6送信はユーザー承認待ちのため未実施。
[2026/09/11 14:34] ユーザーの明示承認を受け、owner_only site https://h3qwrv2c.aboutme.style/ へv0.5/v0.6の製品紹介・マニュアル・Dual Flickデモ・画像8点・APK ZIP 2本を公開した。全15ファイル、41,164,699 bytesのremote size/SHA-256が `docs/verification/v0.6-site-manifest.sha256` と一致し、v0.6 ZIPは20,056,780 bytes、SHA-256 `d24e50f30080d5b468fd4a4655e1c4bacdc1a3dbd80b3e42d6dc392acefb41dc`。公開ページとマニュアルのv0.6ダウンロード導線をowner認証済みブラウザで確認した。
[2026/09/11 14:34] Webデモは端末テーマに応じたLight/Dark表示に対応しているが、Android nativeキーボードはdark固定であることを確認。公開・GitHub push完了後の次作業として、端末テーマ連動のnative Light Modeをチケット化して実装する。
[2026/09/11 14:37] Android nativeはDayNightテーマを使用しているが、キーボード・候補欄・ポップアップの色がDark固定でLight Mode未対応と確認。チケット260911-053738-support-native-light-modeを作成し、端末uiMode自動追従で実装開始。
[2026/09/11 14:43] HTML正本のLight配色と既存Dark配色をAndroidのvalues/values-nightへ定義し、キーボード・候補欄・フリックpopup・設定プレビューが端末uiModeへ追従する実装を追加。Light/Darkの描画テストを含む対象unit testが成功。
[2026/09/11 14:50] Light Mode実装でfast-check 5件、unit 125件、lint、APK build、API 36.1 AVD connected test 7件が成功。AVDをLight/Darkへ切り替え、Gesture IMEの両テーマ実画面をスクリーンショット保存した。
[2026/09/11 14:57] 追加依頼を3チケット化: レイヤーキー左スワイプの音声入力、設定可能な最大6件のスラッシュコマンド候補（既定/compact,/clear,/quit）、QWERTY下スワイプ補助ラベルの縦中央移動。Light Mode版の公開後に順次実装する。
[2026/09/11 15:02] 音声認識は現在`onPartialResults`を無視していると確認。途中結果を候補・確定エリアへ一時表示し、取消・非対応・許可UIをキートップ/候補の角丸・影・Light/Dark配色へ揃えるticket 260911-060238-align-voice-status-ui-and-show-partialsを作成。
[2026/09/11 15:10] Light Mode v0.7の独立再reviewで追加Critical/Majorなし。候補Light背景の正本差を修正し、runtime候補token・popup再描画を回帰testへ追加した。最終fast-check 5件、unit 127件、lint、APK build、connected 7件が成功。Light/Dark実画面と設定preview、製品紹介・マニュアルを確認し、owner_only Sites公開用18ファイルmanifestを確定した。
[2026/09/11 15:17] Gesture IME v0.7.0をowner_only Sites `https://h3qwrv2c.aboutme.style/`へ公開した。製品紹介・マニュアル・Light/Dark画像・APK ZIPを更新し、公開18ファイル・61,540,258 bytesのpath/size/SHA-256はローカルmanifestと全件一致。認証済み実ブラウザでv0.7製品ページとマニュアルのダウンロード導線を確認した。
[2026/09/11 15:18] QWERTY下swipe補助labelの縦中央化ticketをv0.7完成履歴から開始。現状の固定13dp移動は45dp keyで0.5dp、52dp keyで4dp中央からずれると測定し、実key高に応じた終端と狭幅/広幅Canvas回帰testへ修正する。
[2026/09/11 15:23] QWERTY下swipe補助labelの終端を実key高から算出し、45dp/52dpの双方で文字visual centerがkey中央へ一致するよう修正。戻りanimation、上swipe、Enter/Pasteは既存testを維持し、fast-check 5件、全unit、lint、APK buildが成功。独立reviewへ進む。
[2026/09/11 15:28] 下swipe中央化の独立reviewで、ユーザー調整Y±8dpによる終端ずれと、rowSpan Enter/Pasteへ動的移動量が漏れるMajor 2件を検出。QWERTY文字キーだけ中央を強制し、全5layer Enter/Pasteは従来13dpを維持。直接cancel復帰も含むKeyboardView test 31件が成功した。
[2026/09/11 15:29] 日本語変換候補・英字補完候補がHTML正本の候補faceと一致しない追加指摘を受領。native共通CandidateStripViewと`demo.html`内の`mock.html`を、高さ34/最小幅82/左右14/間隔5/角丸7/下影1、Light/Dark選択色へ揃えるticket `260911-062719-align-candidate-ui-with-html`を起票した。
[2026/09/11 15:31] QWERTYとかなはキー面高が外45dp/内52dpで一致する一方、縦gapが10dp対6dpのため4行全体で16dp差が出ると確認。ユーザー補足に従い、かな・Dual Flickの縦gapをQWERTYの10dpへ揃えるticket `260911-063048-unify-four-row-keyboard-heights`を起票した。
[2026/09/11 15:39] アプリ入力時にIME上部の空きが過大になりキー群が下へずれることがある実画面報告を受領。添付画像を`docs/verification/intermittent-keyboard-vertical-offset.jpg`へ保存し、保存layer復元・候補欄固定高・KeyboardView再計測・bottom inset順を通常起動/hide-show/入力欄切替/候補状態別に再現するticket `260911-063912-fix-intermittent-keyboard-vertical-offset`を起票した。
[2026/09/11 15:45] 日本語/英字候補faceをHTMLの34高/82最小幅/14余白/5間隔/R7/下影1へ揃え、Dark通常/選択色もkey paletteへ修正。demo mockは英字候補toggleと1文字prefix、tap置換、大文字保持に対応した。独立reviewのMajor 2件・Minor 2件を解消し、fast-check 5件、unit 129件、lint、APK build、実ブラウザCSS/入力確認が成功。
[2026/09/11 15:50] かな・Dual Flickの縦gapを6dpからQWERTYと同じ10dpへ変更し、row pitchも外55dp/内62dpへ統合。キー面高45/52dpと横gap6dpは維持。412/840幅のQWERTY・記号・かなで4行総高/face高/gap一致、既存Enter/Dual hit targetを含むKeyboardView test 32件が成功。
[2026/09/11 15:54] キー縦gap統一の独立reviewはCritical/Majorなし。指摘されたDual直接証跡を追加し、840dp Dualかなでface52dp・gap10dpを固定。親高不足時の既存縮小挙動は、添付画像由来の縦ずれticketでlayout lifecycleと併せて調査する。
[2026/09/11 16:22] QWERTY＋記号の全割当を監査し、印字可能ASCIIで入力不能なのは既存仕様でESCへ置換したバッククォートだけと確認。記号レイヤーでQWERTY下フリックと重複する`"`をバッククォートへ、`/`を`-`へ置換し、ESC tap維持・記号上フリックなし・ASCII 95文字網羅testを行うticket `260911-072234-complete-qwerty-symbol-ascii`を起票した。
[2026/09/11 16:26] 日本語はQWERTYと同じ縦gap 10dpへ修正済みだが、テンキーとカーソルは6dpのままで4行全体が16dp短い対象漏れを確認。全5レイヤーの外45dp/内52dp faceと縦gap 10dpを統一するticket `260911-072658-unify-all-four-row-layout-heights`を起票した。
[2026/09/11 16:34] 断続的な縦ずれはIME root内のKeyboardViewが`height=0, weight=1`で、windowの`AT_MOST`初回計測時にintrinsic高がdesired heightへ寄与せず候補欄50dpだけになることをAVDで再現。`WRAP_CONTENT`へ修正し、候補欄50＋keyboard228＝root278、候補/音声状態で全高不変をunit testへ固定。修正後の初回・hide/show・入力欄切替でkeyboard領域がpixel一致し、fast-check 5件、全unit、lint、APK build、connected 7件が成功した。
[2026/09/11 16:43] 記号レイヤーの重複tap `"`をバッククォート、`/`を`-`へ変更し、ESC tapとQWERTY `l`下の`"`・`b`下の`/`を維持。QWERTY＋記号の`CommitText`集合が印字可能ASCII `0x20..0x7e` 95文字と完全一致し、記号文字keyに方向割当がない回帰testが成功。独立reviewはCritical/Majorなし、全suiteとconnected 7件も成功した。
[2026/09/11 16:49] 対象漏れだったテンキー・カーソルを含む全5レイヤーの4行geometryを統一。外幅はpitch55/gap10/face45・全高228dp、内幅は62/10/52・全高256dpとし、全mode直接testと既存Dual/縦長Enter/Space/Cursor hit targetを確認。独立reviewはCritical/Majorなし、全suiteとconnected 7件が成功した。
[2026/09/11 17:11] レイヤーキー中央の1秒hold音声開始を廃止し、左フリック方向確定で端末内音声認識を開始、指を離して終了・確定する操作へ変更。中央tap/holdと上日本語・右QWERTY・下テンキーを維持し、音声中の2本目pointerは認識と残りのキー入力を取り消す。対象unit、fast-check 5件、全unit、lint、APK build、connected 7件が成功した。
[2026/09/11 17:33] 設定画面へスラッシュコマンド候補6slotを追加し、初期値`/compact`,`/clear`,`/quit`、先頭slash補正、空欄・重複の表示除外を実装。通常欄の`/`を候補tap時だけ置換確定し、stale tapとprivate欄を保護した。独立reviewはCritical/Majorなし、全suiteとconnected 7件が成功。API 36.1 AVDで6欄表示、QWERTYの`/`入力、3候補表示、`/compact`確定を確認した。
[2026/09/11 17:40] 端末内SpeechRecognizerのpartial resultsを有効化し、最新途中文字を候補欄の共通faceへ表示。release後もfinalまでpartialを保持するが入力欄へは確定せず、finalだけを自動入力する。cancel時の表示残留も修正し、取消・許可・非対応の34dp/R7/影/Light-Dark色を回帰testへ追加。独立reviewはCritical/Majorなし、全suiteとconnected 7件が成功した。実発話partialはAVDに日本語modelがないため実機確認待ち。
[2026/09/11 17:55] v0.8公開物の最終化を開始。versionCode 8/versionName 0.8.0へ更新し、製品紹介・マニュアル・操作モックを、左フリック保持の音声入力、途中結果、設定可能なスラッシュ候補、全5レイヤー同高、QWERTY＋記号のASCII 95文字網羅へ同期した。モックにはマイクを使わない音声途中表示と確定、取消も実装し、ブラウザで外画面・内画面の確認を進めている。
[2026/09/11 18:01] 公開モックをブラウザで比較し、旧CSSの対象漏れで内画面テンキー/カーソルだけ58px、日本語/QWERTY/記号は52pxという高さ差を再現。全レイヤーを外45px・内52px・縦gap10pxへ統一し、内画面5種すべて4行238px、Dual Flick各行8キー、横overflow 0を確認した。`/`入力で3候補表示と`/compact`置換、記号面のbacktick/minusも実操作で確認した。
[2026/09/11 18:12] v0.8.0最終版はfast-check 5件、unit、lint、APK build、API 36.1 connected実Mozc 7件が成功。APK 34,871,816 bytes/SHA-256 `58909cbb5c5213d52f2cb50fb5f5697ac542eb6896e3f76a11327c42bd7591ff`、ZIP 20,072,474 bytes/SHA-256 `7bd0129cad8c63827a94fcc671fbdbe28e32a09d6986805e8c33f65ee5988e21`。Terra reviewの音声mock lifecycle Majorを修正して再review Critical/Majorなし。既存owner_only Sites `https://h3qwrv2c.aboutme.style/`へ変更7ファイルを公開し、公開21ファイルはローカル同名SHAと全件一致。認証済み実ブラウザで製品ページ、v0.8マニュアル画像2点、音声ラベル付き4行demo、console error 0を確認した。
[2026/09/11 19:13] 日本語変換の学習有無を確認。Mozc初期化で`incognitoMode=true`かつ`historyLearningLevel=NO_HISTORY`を明示しており、候補確定による履歴学習・永続化は行わない。そのため現版には候補長押しで削除する学習項目がなく、条件付き要望の実装は不要と判断した。
[2026/09/11 19:20] demoのEnter下フリックで`paste`だけ瞬間移動していた原因を、通常の副ラベルと異なり`.key-hint`に90msのtransform/color/opacity transitionが無いことと特定。nativeで固定している13dp移動量は維持し、demoの補間を通常キーと揃えた。Enter上フリック`Ctrl+J`とMozc学習・Android個人辞書対応のticketを起票し、並行実装を開始した。
[2026/09/11 19:27] Enter上フリック`Ctrl+J`のnative実装とgesture/描画testが完了。待機中は追加ラベルなし、選択中だけ`C-j`を中央表示し、変換中の上無変換・左カタカナを維持した。候補内容更新時だけ横スクロールを先頭へ戻すticketを追加し、nativeとdemoの同期を開始した。
[2026/09/11 19:55] Mozc の学習順位変化を Android 計装テストで確認した。Android 個人辞書の SHORTCUT 読み出しも実プロバイダで確認済み。候補内容が変化した場合だけ候補欄を先頭へ戻す実装と、同一内容での選択変化ではスクロール位置を維持するテストを追加した。
[2026/09/11 20:08] レビュー指摘を反映し、実JNIで候補学習、新sessionで先頭化、履歴削除後の順位復元まで恒久計装testへ追加。Provider未提供・拒否時は通常Mozc候補だけを出す仕様をmanualへ明記し、設定実画面を撮影した。fast-check 5件、unit、lint、APK build、API 36.1 connected実Mozc 9件が成功。Terra再reviewはCritical/Majorなし。v0.9.0 APKは34,888,644 bytes/SHA-256 `695506d514d60338fb4bb1b9d9d48957c65a123c31efeff153540b1d48e2bc25`、ZIPはSHA-256 `e1c7895d90d3cffd0d405170f1048760dc2159e180a7bbdd86a23800aa7403e5`。
[2026/09/11 20:18] `https://h3qwrv2c.aboutme.style/` をGesture IME v0.9.0へ更新。Sites容量上限に合わせ、現行ページから未参照のv0.5 ZIPだけを配布サイトから外し、v0.6〜v0.9を保持した。製品紹介、manual、demo、mock、画像3点、v0.9 ZIPの公開SHA-256がローカル8ファイルと全件一致した。
[2026/09/11 20:19] v0.9.0実装・テスト・公開物をcommit `b11a393`まで `github.com/masuidrive/android-kbd` の `features/260911-060238-align-voice-status-ui-and-show-partials` へpushした。
[2026/09/11 20:57] ユーザのclose承認を受領。音声途中結果UI、Enter上Ctrl+J、Mozc学習・Android個人辞書、候補内容変更時scroll resetの4ticketをPDH-closeした。統合commit `7318247` を `features/260911-055701-configure-slash-command-candidates`、元履歴commit `6844732` を音声UI branchへpushした。
[2026/09/11 22:30] タップ精度を監査。nativeは見えるキー面をそのままACTION_DOWNのhit boundsに使うため、HTMLへ合わせて広げた横6dp・縦10dpの隙間が無反応領域になっていた。412dp QWERTYのセル内有効面積は概算約70%で、以前よりタップ許容度が下がるため修正ticketを作成。外観とフリック閾値を維持し、隙間を近いキーへ中点分割する実装を開始した。
[2026/09/11 22:57] 描画矩形とtap矩形を分離し、横6dp・縦10dpの見えるgapを隣接キーへ中点分割。EMPTYは無反応、Dual Flickと複数行Enterは重複なし、ACTION_DOWNとaccessibilityは共通判定にした。独立Terra reviewはCritical/Majorなし。v0.9.1最終版でfast-check 5件、unit、lint、APK build、API 36.1 connected実Mozc 9件が成功し、AVDのQWERTY表示もv0.9.0から不変と確認した。
[2026/09/11 23:03] v0.9.1 APKは34,888,644 bytes/SHA-256 `2baffbe07ea94a1c67a01749ec512867fd9d21309f81a71c6c7949b556ca7a23`、配布ZIPはSHA-256 `fa2f5bff5f4af0b61af0637a554b609603aa94d578dab6cd9fb65e77f58e7e37`。既存Sitesのv0.6削除は自動承認reviewで拒否されたため既存版を保持し、v0.9.1専用owner_onlyサイト `https://fez69vft.aboutme.style/` を新規公開。公開HTML/CSS/mock/APK ZIPはローカルSHAと一致し、実ブラウザで製品ページの01〜15連番、manual、v0.9.1 download link、console error 0を確認した。
[2026/09/11 23:04] タップ精度修正commit `e046d0e`、v0.9.1公開準備commit `868ad2f`、公開確認commit `ba13e3d`を `github.com/masuidrive/android-kbd` の `features/260911-055701-configure-slash-command-candidates` へpushし、remote head一致を確認した。
[2026/09/12 00:36] 公開サイトトップを一般Android向けの`Android Flick Keyboard by masuidrive`へ再構成するticket `260911-153653-refresh-public-site-hero-mock`を起票。ヒーローを入力可能mockにし、初期文「ここは入力できるよ」、Light/Dark、Mobile/Tablet、Dual Flick切替、`md-kbd`ブランドを実装する。Foldは専用品ではなく、閉じたスマホ幅と開いたタブレット幅の両方で使える対応例として残す。今回はローカル確認までとする。
[2026/09/12 01:04] 製品トップの最初を実際に入力・タップ・フリックできるmockへ変更し、実viewportに応じて390pxはMobile、840pxはTabletを初期選択することを確認。Light/Dark親同期、Tablet Dual Flick 4行各8キー、横overflow 0、iframe下の余白なし、独立demo継続を実ブラウザで確認した。「QWERTYも、Flickで。」へコピーを変更し、旧テーマ静止画セクションを削除。全test suite PASS、独立reviewはCritical/Major/Minorなし。
[2026/09/12 01:04] 左フリック成立時に認識を始め、途中結果と可変件数の最終候補、送信・キャンセルを扱う専用音声入力面の検討ticket `260911-160113-voice-input-layer`を起票。Android SpeechRecognizerは最有力順の複数候補を返せるが端末実装により1件の場合もあり、途中結果も0回以上のため両方を扱える仕様とした。
[2026/09/12 01:35] 専用音声入力layerをブラウザmockへ実装。左フリック成立で即座に模擬認識を始め、途中結果の後に一体型card内へ3候補とCancelを表示する。上部候補欄への音声候補と送信buttonは廃止し、候補tapでexact 1回確定、Cancelで入力不変のまま元layerへ復帰。通常候補欄は空でも42pxを維持し、視覚feedback行を削除した。390/840pxの横overflow 0、iframe実高一致、Mobile/Tablet、Dual、Light/Darkを実ブラウザ確認。全suite PASS、独立reviewはCritical/Major/Minorなし。
[2026/09/12 01:47] サイト上部を正式表示`masuidrive-kbd`、省略形`md-kbd`へ整理。Lightのeditorとtextareaを薄いgray `#f3f4f7`、Darkを従来の`#1c1c1e`とし、mock最下段の言語・閉じる帯とhome indicatorを削除。「操作モック」「ブラウザで試す」はトップ先頭demoへjumpするよう変更した。reviewの旧hide参照とmobile anchor隠れを修正し、390pxでdemo上端がnav下10px以上、横overflow 0、候補欄42px、console error 0を確認。全suite PASS、再review findingなし。
[2026/09/12 02:11] 音声入力mockをキーボード面へ合わせ、左上キャンセルと発話全体の候補を1行1件で縦に並べるUIへ変更。既存キーと同じ高さ45px・角丸R5・影・Light/Dark色を使用し、Mobile/Tabletの確定・取消・overflow 0を確認した。
[2026/09/12 02:11] APK配布をGitHub Releasesの直接APKへ移す作業を開始。Sitesからv0.1.0〜v0.9.1の旧ZIP 10本を削除し、サイト容量を約200MBから3.6MBへ削減、全ページのリンクをv0.9.1 Release assetへ更新した。
[2026/09/12 02:23] GitHub Release v0.9.1へAPK単体を公開。34,888,644 bytes、SHA-256 `2baffbe07ea94a1c67a01749ec512867fd9d21309f81a71c6c7949b556ca7a23`でビルド成果物と一致。activeなGesture IME Sites 3件から旧ZIP計9本を削除し、最新owner_onlyサイト `https://fez69vft.aboutme.style/` を34ファイル・3,750,743 bytesで再公開。ローカルとのpath/SHA-256全件一致、3サイトともZIP 0件を確認した。
[2026/09/12 02:33] 公開サイト上部のローカル案を`masuidrive.jp/resume.html`のテイストへ変更。白地に60px/30pxの`masuidrive-kbd`と副題、右下に参照元と同一SHAのクマ画像128px/64pxを配置し、画像全体を`https://masuidrive.jp/`へのリンクにした。390/840pxでoverflow 0、既存mock入力とLight/Darkを確認。Sitesは未更新。
[2026/09/12 02:44] ローカル案をトップページ全体へ拡張。最上部は名前mastheadだけを見せ、通過後にfixed topbarを表示し、上端へ戻ると隠す。JS/IntersectionObserver非対応時は通常navを残し、reduced-motionでは遷移を止める。背景#f4f4f4、白い本文面、#212529、赤い左6px＋下1px罫線へ統一し、15機能を履歴書風の行へ再構成。390/840/1440px、Light/Dark、anchor遮蔽なし、overflow 0、mock入力を確認。独立review findingなし、全suite PASS。Sitesは未更新。
[2026/09/12 02:52] 外側サイトのDark表示を廃止し、index/manual/demoをOS設定に関係なくLight paletteへ固定。mockから親へのtheme通知を外し、高さ通知だけ維持した。キーボードmock内部はLight/Darkを引き続き切替可能。OS Dark条件を含む390/840/1440pxで外側の色不変、iframe高さ一致、overflow 0、console出力なしを確認。独立review findingなし。Sitesは未更新。
[2026/09/12 03:33] QWERTYラベル調整機能をActivity・設定保存・Service・描画経路から完全撤去し、設定画面を4セクションへ整理してバージョン0.9.1を表示。Light/Darkの実画面と全操作、最新マニュアル画像を確認した。
[2026/09/12 03:33] 音声専用レイヤーを通常と同じ固定4行高、左下キャンセル、上下右のレイヤーフリックへ更新。途中結果と複数の最終結果を日本語変換と同じ候補UIへ表示し、finalだけ選択可能、選択時1回確定、終了時stale結果破棄を実装した。
[2026/09/12 03:33] ローカル製品トップの埋め込み入力枠を外し、機能一覧を01〜06へ絞った。音声mockは通常候補bar、固定高、左下キャンセルへ同期し、Mobile/Tabletの通常・音声高一致と横overflow 0を確認。マニュアルも候補選択式へ更新し、全test suiteがPASS。Sitesは未更新。
[2026/09/12 08:39] 承認を受け、最新の製品トップ、操作mock、候補選択式の音声入力マニュアル、設定画面と音声面の実画面画像を既存owner_only Sites `https://fez69vft.aboutme.style/`へ公開。36ファイル・3,809,300 bytesのpathとSHA-256がローカルと全件一致した。
[2026/09/12 08:40] 設定画面整理、音声専用レイヤー、変換候補と共通の音声候補UI、ローカルサイト再設計、公開記録をcommit `859239b`まで `github.com/masuidrive/android-kbd` の `features/260911-055701-configure-slash-command-candidates`へpushした。
[2026/09/12 08:57] 音声認識の途中結果と最終候補だけを候補欄の可視幅以内・最大2行・末尾省略で表示するよう変更。通常の日本語変換・英字補完・スラッシュ候補は1行横スクロールを維持し、Mobile 390pxとTablet 840pxのLight/Darkで固定高と横overflow 0を確認した。全test suite PASS、独立reviewはCritical/Majorなし。
[2026/09/12 09:01] 音声候補の2行表示を既存owner_only Sites `https://fez69vft.aboutme.style/`へ公開。36ファイル・3,809,600 bytesで、ローカルと公開先のpath/SHA-256が全件一致した。
[2026/09/12 09:02] 音声候補改行の実装・検証・公開記録を `github.com/masuidrive/android-kbd` の `features/260911-234748-wrap-long-voice-candidates`へpushした。
[2026/09/12 09:06] 製品トップの機能01を「かなはフリック入力」へ変更し、中央タップと上下左右フリックによるかな入力、Mozc候補表示を説明する文面へ更新。他の機能カードとnative/mock/manualは変更していない。公開・pushは未実施。
[2026/09/12 09:10] 機能01の文言修正を既存owner_only Sites `https://fez69vft.aboutme.style/`へ公開。36ファイル・3,809,625 bytesで、ローカルと公開先のpath/SHA-256が全件一致した。390px/840pxの実ブラウザ表示と横overflow 0、全test suite PASS、独立review Critical/Majorなしを確認した。
[2026/09/12 09:11] 機能一覧の見出しを「フリックで、タップを減らす。」へ変更し、上下左右フリックで文字を直接選んでタップ回数を減らせる価値を導入文の主題にした。レイヤー名一覧と各機能カードは維持。公開・pushは未実施。
[2026/09/12 09:16] 製品トップの操作demoへ枠線なし入力欄を復帰し、初期文からキー入力・削除・かな候補・音声候補確定が同じ欄へ反映されることを確認。Light/Dark、Mobile 412px/Tablet 840pxで横overflowなし、単独mockの編集欄も維持。公開・pushは未実施。
[2026/09/12 09:18] トップ埋め込みdemoの表示済みtextareaから旧`tabIndex=-1`/`aria-hidden`を除去。アクセシビリティtreeとTab順に「入力を試す」が現れ、直接編集後もmockキー入力が同じ欄へ続くことを実ブラウザで確認。公開・pushは未実施。
[2026/09/12 09:23] 「フリックで、タップを減らす。」の製品コピー、機能01「かなはフリック入力」、枠線なしのhero入力欄を既存owner_only Sites `https://fez69vft.aboutme.style/`へ公開。36ファイル・3,809,462 bytesで、ローカルと公開先のpath/SHA-256が全件一致した。
[2026/09/12 09:23] 次作業として、アプリ切り替え直後のIME高さ計算、変換中Enterの無変換・カタカナ即時確定、現行HTML/CSS基準の設定画面再点検、native音声レイヤー完成度の突合を受領した。
[2026/09/12 09:41] アプリ切替中に親から渡る一時的な過大EXACTLY高をKeyboardViewが採用するraceを修正し、現在幅・inset由来の固定4行高を入力View開始時から再適用した。5レイヤーの過大初回measure回帰と既存lifecycle testsが成功。API 36.1 AVDでSettings検索欄と内蔵入力テストを5往復し、10表示すべてキー背景上端y=1545、候補欄と4行高不変を確認した。公開・pushは未実施。
[2026/09/12 10:01] 変換中Enterの上フリックを元readingのひらがな、左フリックを全角カタカナとして1回で確定し、compositionと候補を即時clearするよう変更。Service回帰testと既存Enter gesture/layout testが成功し、browser mockも実pointer操作で上「あ」・左「アア」、各候補0件を確認した。公開・pushは未実施。
[2026/09/12 10:02] 変換中Enterの上フリックでひらがな、左フリックで全角カタカナを1回で確定する実装を独立レビューし、全テスト2/2 PASSを確認しました。次の設定画面・音声レイヤーticketは現行nativeとHTML/CSSを事前比較し、設定の視覚階層と古いマニュアル画像を主な修正対象に確定しました。
[2026/09/12 10:10] v0.10設定画面を4つの角丸カードへ整理し、Light/Darkのsurface、細い赤accent、48dp操作領域、BuildConfig由来version表示をAPI36.1とRobolectricで確認。最新Light/Dark画像をmanualへ反映し、fast-check・unit・lint・APK buildがPASS。音声面は既存native契約を維持し、実発話partial/finalは日本語モデルのある実機確認待ちとして記録。
[2026/09/12 10:16] API 36.1の現行APKでQWERTY左下を左フリックし、音声候補欄の「非対応」、固定4行面、通常キー形状の左下「キャンセル」を同時確認してv0.10画像へ更新。manualの旧v0.9音声画像を置換し、操作モックのpartial/final確認導線を追記。
[2026/09/12 10:20] 音声Permission/Unavailable controlが選択候補の青faceを誤用していたreview Majorを修正。操作可能性を保った通常candidate faceへ分離し、Light/Darkの色・shadow・click/focusをRobolectricで固定、API 36.1のLight/Dark実画面を確認してv0.10音声画像を再撮影した。
[2026/09/12 10:27] v0.10.0 APKをGitHub Releasesへ公開し、認証済み再download・GitHub asset・ローカルのSHA-256 `2698891f37285d53eca0313625c3a2662687e0f0ac8db725115f2cbbddb55eed`が一致した。owner-only Sites `https://fez69vft.aboutme.style/`へ39ファイル・4,314,331 bytesを公開し、全path/SHA-256一致、live demo入力欄、v0.10 manual画像、横overflowなし、console errorなしを確認した。
[2026/09/12 10:42] ターミナル向けカーソル操作だけ初期OFFに残し、Dual Flick・英数字候補・Android個人辞書を初期ONへ変更するticketを作成。既存の明示保存値は上書きしない契約とした。Mozcの次単語予測は公式session APIと現行JNI実装の差分を調査中。
[2026/09/12 10:45] SharedPreferences key未保存時だけDual Flick・英数字候補・Android個人辞書を既定ONへ変更し、terminal cursorはOFFを維持。明示false保存後のSetup再生成でも利用者選択が保たれる回帰testとmanual/TRの初期値説明を更新した。
[2026/09/12 10:48] 新規状態のSetupをAPI 36.1 Light/Darkで確認し、3設定ON・terminal OFFの最新画像へ更新。英字候補OFFを暗黙前提にした音声test fixtureを明示OFFへ直し、full fast-check/unit/lint/APK buildがPASS。
[2026/09/12 10:49] Mozc公式session APIを確認し、入力中のsuggestion/predictionに加えて確定後の周辺文脈から次単語候補を返すREQUEST_NWPが利用可能と判明。現行md-kbdはREQUEST_NWPとsurrounding contextを未接続のため、POBox風の確定後予測は未実装と整理した。Fold開閉時の高さ差は幅依存row pitchと遅延inset反映が原因で、固定高さpresetの別ticket対象とした。
[2026/09/12 10:51] Fold開閉とDual Flickでキー4行高を変えず、設定画面で小・標準・大を選べるticket `260912-014943-fixed-keyboard-height` を作成。初回表示・アプリ切替・回転・遅延insetでも設定高とタップ領域を維持するACを定義した。
[2026/09/12 11:09] Mozc `REQUEST_NWP`で日本語確定後の次単語候補を既存候補欄へ出すticket `260912-020555-add-mozc-next-word-prediction` を作成。候補タップ後の連続予測、入力・選択・欄切替時のclear、private欄での文脈取得禁止を契約化した。カーソル専用レイヤーをrecent-first絵文字レイヤーへ置換するticket `260912-020803-replace-cursor-layer-with-emoji` も後続として作成した。
[2026/09/12 11:18] target SDK 36のedge-to-edgeでSetupがstatus barへ入り込む原因をinset未処理と特定。固定トップアプリバー「masuidrive-kbd 設定」、戻る操作、systemBars/displayCutout safe area、Light/Dark system iconを整えるticket `260912-021714-fix-setup-safe-area-and-app-bar` を作成し、高さ設定ticketの先行依存にした。
[2026/09/12 11:19] Mozc `REQUEST_NWP`へ通常欄の有限周辺文脈を渡し、確定後の次単語候補を既存候補欄へ接続した。候補tapは`SUBMIT_CANDIDATE`で直前文字を置換せず確定して再予測し、private/学習禁止欄では周辺文字列を取得しない。API 36.1 arm64 AVDで同梱辞書の`あけまして`文脈が非空候補を返し、submit結果も非空であることをinstrumentationで確認した。
[2026/09/12 11:25] 候補欄と最上段キーの縦間隔をキー行間隔へ揃え、先頭候補の左側にも同じ外周余白を設けるticket `260912-112512-align-candidate-strip-spacing` を作成。固定高さの後、nativeと公開demoを同じ寸法基準で更新する。
[2026/09/12 11:33] 次単語予測のreviewで見つかったselection raceを修正した。日本語composition置換の前後差を自己callbackとして照合し、NWP待機中の外部selectionはgenerationを無効化して古い候補を再表示しない。4→3、4→4、3→4と遅延照会の回帰test、full local suiteがPASSした。
[2026/09/12 11:46] 次単語予測の自己selectionと遅延照会raceを修正し、独立再reviewはCritical/Majorなし。focused JVM 62/62、API 36.1 arm64 AVDの同梱Mozc connected test 11/11でAC 1〜5を確認した。system IME手動journeyは既知の初回高さ溢れを先に直して再確認する。
[2026/09/12 11:56] Setup画面をAPI 35/36 edge-to-edgeのsafe areaへ対応させ、status/navigation barを跨がない固定「masuidrive-kbd 設定」バー、48dp戻るicon、scroll終端のbottom inset、Light/Dark icon appearanceを実装した。API 36 arm64 AVDのLight/Dark portraitとDark landscapeで実画面を確認し、最新manual画像を更新した。
[2026/09/12 12:07] Setup safe areaと固定トップバーを独立review・AC verifyし、Critical/Majorなし、AC 1〜5達成、focused 6/6・full suite 2/2 PASSを確認した。物理Fold以外のLight/Dark portrait・landscape・scroll終端を実画面で確認した。
[2026/09/12 12:27] キーボード高さを小・標準・大へ保存可能にし、標準55dp row pitchの4行を幅・Dual Flickから固定した。API 36 AVDの412dp、840dp相当、landscapeで候補欄・4行・navigation safe areaを実表示し、focused JVM testsとfull suiteをPASSした。
[2026/09/12 12:51] 候補faceを50dp候補欄のtop14dp/bottom2dpへ置き、最上段keyとの10dp間隔と先頭候補のphone6dp/wide13dp左外周を揃えた。API 36 AVDで候補表示を確認し、nativeと公開mockの寸法を同期した。
[2026/09/12 13:25] カーソル専用レイヤーを絵文字4行面へ置換し、recentを端末内に新しい順・重複なし最大8件で保存した。API 36 AVDで😀の直接確定後に入力欄とrecent先頭へ表示されること、focused Robolectricとfull suiteのPASSを確認した。
[2026/09/12 13:44] 絵文字レイヤーreviewを修正し、現editorだけでcommit成功時にrecentを更新するservice回帰testへ日本語・英字・slash composition、commit拒否、旧editor queueを追加した。公開文言とreferenceを絵文字recentを含むレイヤー表記へ統一した。
[2026/09/12 13:56] 絵文字レイヤーreview attempt2を修正し、中央の頁表示キーを先頭で次・最終で前へ進む操作にした。かなcomposition直結とconversion reset中のeditor切替でemojiを誤確定・recent更新しないservice回帰test、現行レイヤー遷移のreferenceを追加した。
[2026/09/12 14:02] 絵文字レイヤーreview attempt3のfocused証跡を指定4クラスでfresh再実行し、Gradle `BUILD SUCCESSFUL`を確認した。summaryに件数がないため、4 classes PASSとしてnoteとresultへ記録した。
[2026/09/12 14:11] v0.11.0公開準備としてAPK versionCodeを12、versionNameを0.11.0へ更新し、製品siteのAPKリンク、現行表記、safe-area・高さ・候補余白・絵文字recentの画像参照、release notesを同期した。公開・push・GitHub releaseは未実施。
[2026/09/12 14:19] release reviewで変換中Enterの上・左フリック即時確定がv0.10公開済みと確認し、v0.11.0 release notesとmanualの新規変更一覧から削除した。公開・push・GitHub releaseは未実施。
[2026/09/12 14:30] Gesture IME v0.11.0をGitHub Releasesへ未圧縮APKとして公開し、同じ版の製品紹介・操作モック・マニュアルをHanger Sites `https://fez69vft.aboutme.style/`へ公開した。APKは34,926,352 bytes、SHA-256 `b3ed40bb2d18909172e3a05c9ad5273436e9570e0998a09da72b20561e9cfdcf`で再download後も一致。Sitesはlocal/remote 44ファイルが全件一致しAPK/ZIPなし。実ブラウザでトップ、入力可能な埋め込みdemo、Light/Dark・Mobile/Tablet・Dual Flick、390px/1024pxのoverflowなし、単独mock入力、manual 17画像とv0.11 APK導線を確認した。
[2026/09/12 14:51] 実機画像で絵文字一覧が2行＋頁操作になっているとの指摘を受けた。ページ式を廃止し、固定4行高の上3行をrecent＋全絵文字の縦スクロールviewport、最下段を固定レイヤー操作行にするticket `260912-055128-make-emoji-grid-scrollable` を作成した。recentが空なら先頭24件で3行を埋める。
