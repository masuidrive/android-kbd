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
