# Technical Reference: Android Flick Keyboard by masuidrive

Based on https://github.com/masuidrive/pdh/blob/15e6289/codex/templates/technical-reference.md

このファイルは常に「現在の姿」だけを書く。過去の経緯・置き換えられた判断は削除する（履歴は git が持つ）。
残す基準は「将来の ticket の判断を今も拘束するか」。拘束しなくなった記述は消す。
通読させる文書ではなく、検索して引く文書として書く（番号付き見出し・検索可能なリテラル）。

## 更新ルール

- ticket close 時、その ticket の差分に因果がある範囲だけを追記・上書きする。
  実装として出荷済みの挙動は確定した事実としてその場で書く。承認待ちで先送りしない
  （承認が要るのは brief の意思の変更だけで、このファイルは事実の記録）。
  自分の変更が置き換えた記述は削除してよい。他 ticket 由来の記述は消さない
  （不要と思ったら削除候補として ticket note に記録し、棚卸し ticket に送る）。
- 肥大して検索ノイズが増えたら、専用 ticket で棚卸し→圧縮→別モデルによる保全検証を行う
  （削除判断を単独 agent に任せない）。

## Architecture overview

- `ImeService` がAndroidのIME lifecycleと `InputConnection` を所有する。
- `keyboard/KeyboardView` は5レイヤーをCanvas描画し、入力意図を `KeyAction` として通知する。Editorの変更は行わない。
- `TextInputController` がcomposing、確定、削除、カーソル、Editor action、clipboardを一元化する。
- `conversion/ConversionEngine` はsuspend APIでMozc JNIを隠蔽する。JNI呼出しは単一dispatcher上で直列化する。
- `CandidateStripView` はキーボードの兄弟Viewであり、候補選択を描画tokenとindexの組でServiceへ戻す。
- Mozcの共有ライブラリ、辞書、protobuf jarをAPKへ同梱するため、変換時にネットワークを必要としない。

## Design decisions

1. package/Application IDは `com.masuidrive.gestureime` とする。Mozc JNI登録対象だけは上流互換の `com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI` に固定する。（2026-09-11 / 260910-163036）
2. compileSdk/targetSdk 36、minSdk 28、Java 17、arm64-v8aをv1の基準とする。（2026-09-11 / 260910-163036）
3. 候補待ちを含む入力actionは順序を保つ。Editor lifecycle generationと変換generationが一致しない非同期結果は破棄する。（2026-09-11 / 260910-163036）
4. Mozcはincognito/learning無効で利用し、入力内容を永続化しない。secret欄では変換処理そのものを呼ばない。（2026-09-11 / 260910-163036）
5. スマホ・タブレットの代表値として412dpと840dp相当のエミュレータ幅を使い、Fold実機を使っていない確認は実機検証と区別して報告する。（2026-09-11 / 260910-163036, 260911-153653）
6. Dual Flickは設定default ONとし、利用可能幅600dp以上の日本語かなレイヤーだけ中央12キーを左右2組にする。候補・編集・レイヤー切替キーとcompositionは共有する。Foldでは閉じたスマホ幅で1組、開いたタブレット幅で2組を使える。（2026-09-11 / 260911-000706, 260911-153653; 2026-09-12 / 260912-014204）
7. Spaceとカーソルレイヤーの上下移動は、現在Editorから得た`ExtractedText`の改行区切りlogical line間を同じ列で移動し、文書内へclampする。視覚上のsoft wrapは別行として扱わず、抽出不能時はfocus越境を避けるためno-opにする。（2026-09-11 / 260910-233809）
8. 変換中Enterは候補確定を表示し、tapで現在候補、上フリックで元readingのひらがな、左フリックで全角カタカナをそれぞれ1回で確定する。確定後はcompositionと候補を消し、次のかな入力を新しいreadingとして開始する。（2026-09-11 / 260910-233809; 2026-09-12 / 260912-002431-commit-enter-flick-conversions）
9. 音声入力はAPI 31以降の`createOnDeviceSpeechRecognizer()`だけを使用し、`ja-JP`モデルの対応を確認する。通常のnetwork recognizerへのfallbackとモデルの自動downloadは行わない。（2026-09-11 / 260910-233205）
10. 音声入力は左下レイヤーキーを左へフリックし、方向が確定した時点で専用`VOICE` modeへ移って開始する。進入前の文字layerを保持し、候補tapまたは取消で戻る。VOICE面の左下キーは中央tapで取消、上で日本語、右でQWERTY、下でテンキーへ切り替える。（2026-09-11 / 260911-014049, 260911-055701-assign-layer-left-swipe-to-voice; 2026-09-12 / 260911-160113）
11. native描画は保存済み正本HTMLのLight/Dark paletteをAndroidの`values`/`values-night`へ対応させ、端末`uiMode`へ自動追従する。文字倍率はキー境界内へfitし、QWERTYラベルは正本から決めた固定の大きさ・位置で描画する。popupは候補欄の高さを変えず、非focus・非touchのoverlayで上端キーから画面内へ表示する。（2026-09-11 / 260911-022705, 260911-053738; 2026-09-12 / 260911-175407）
12. 左下レイヤーキーは全レイヤーで左=音声入力、上=日本語、右=QWERTY、下=テンキーとし、中央tapは各キーの表示先へ切り替える。記号レイヤーは`#!`キーの中央tapから開く。候補欄の背景は候補の有無によらずkeyboard背景に固定し、選択候補のaccentは維持する。（2026-09-11 / 260911-022705, 260911-055701-assign-layer-left-swipe-to-voice）
13. 英数字候補は既定ONかつprivate欄では無効とする。ON時はASCII英数字を最大64文字のcompositionとして保持し、英字だけのprefixを固定端末内辞書へ照会する。候補tapは描画token、候補source、editor session、生成世代、prefixが一致した時だけ置換確定する。Space/Enterおよびlayer・modifier・cursor・paste・voice・IME hideはraw確定境界とし、editor切替時は旧bufferを新editorへ渡さない。（2026-09-11 / 260911-003916; 2026-09-12 / 260912-014204）
14. QWERTY文字キーの下swipeでは補助labelのvisual centerを実key高の中央へ90msで移動し、狭幅/広幅とも固定anchorから中央へ到達させる。QWERTY以外とEnter/Pasteの下swipeは従来の13dp transitionを維持する。（2026-09-11 / 260911-055701-center-qwerty-down-swipe-label; 2026-09-12 / 260911-175407）
15. 日本語変換候補と英字補完候補は共通faceを使い、高さ34dp、最小幅82dp、左右padding 14dp、gap 5dp、radius 7dp、1dp下影、HTML 15px相当の固定15dp文字で描画する。候補欄全高50dpは上8dp・左右3dp・下8dpを含み、Light/Darkの通常/選択色はHTML key/selected paletteへ一致させる。音声の許可・非対応controlは操作可能性を維持しつつ通常candidate faceを使い、候補選択中のaccentを表示しない。（2026-09-11 / 260911-062719-align-candidate-ui-with-html; 2026-09-12 / 260912-002431-align-native-settings-and-voice-ui）
16. QWERTY、記号、日本語、テンキー、カーソルの全4行layoutは縦gap 10dpとrow pitchを共有し、外画面は55dp pitch/45dp face・全高228dp、内画面は62dp pitch/52dp face・全高256dpとする。日本語の単一配列・Dual Flick・縦長Enterにも同じ縦geometryを適用し、横gap 6dpは維持する。（2026-09-11 / 260911-063048-unify-four-row-keyboard-heights, 260911-072658-unify-all-four-row-layout-heights）
17. IME入力Viewは候補欄を固定50dp、`KeyboardView`をintrinsic `WRAP_CONTENT`として縦LinearLayoutへ積む。`KeyboardView`は入力先切替中に親から一時的な過大`EXACTLY`高を受けても、現在幅とbottom insetから算出した固定4行高を採用し、`onStartInputView`で再適用する。`VOICE` modeも同じ4行高を使い、候補内容や音声状態の変更でIME root高を変えない。（2026-09-11 / 260911-063912-fix-intermittent-keyboard-vertical-offset; 2026-09-12 / 260911-160113, 260912-002431-stabilize-ime-height-after-app-switch）
18. QWERTY＋Symbolsは空白から`~`までの印字可能ASCII 95文字を網羅する。SymbolsではESCをtapのまま維持し、重複していた`"`位置をバッククォート、`/`位置を`-`の直接tapへ割り当てる。`"`と`/`はQWERTYの`l`下・`b`下へ残し、Symbolsの文字keyに方向gestureは設けない。（2026-09-11 / 260911-072234-complete-qwerty-symbol-ascii）
19. スラッシュコマンド候補は端末内設定へ6slotで保存し、初期値を`/compact`, `/clear`, `/quit`, 空3件とする。保存時はtrimし、非空で先頭`/`がなければ補う。通常欄で`/`をcomposing保持して空欄・完全一致重複を除く最大6候補を表示し、candidate token/source/generation/editor sessionが一致するtapだけで置換確定する。private欄では候補化せず`/`を直接確定する。（2026-09-11 / 260911-055701-configure-slash-command-candidates）
20. 端末内音声認識はpartial resultsを要求し、最新の途中結果を通常候補欄の共通faceへ表示する。音声の途中結果と最終候補だけは候補欄の可視幅以下で最大2行に折り返し、超過分を末尾省略する。最終結果は空文字と重複を除いた可変件数を同じ候補UIへ順序どおり表示し、描画tokenとeditor sessionが一致する候補tapだけを1回確定する。取消、error、IME終了、入力欄切替では未確定結果を破棄する。日本語変換、英字補完、スラッシュ候補は従来の1行表示と横スクロールを保つ。（2026-09-11 / 260911-060238-align-voice-status-ui-and-show-partials; 2026-09-12 / 260911-160113, 260911-234748）
21. 変換中でないEnterは上フリックでCtrl+Jを同一gesture内のkey eventとして送り、待機中に上方向ラベルを出さず、選択中だけ`C-j`を17sp相当で中央表示する。下フリックPaste、tap Enter、変換中の上無変換・左カタカナを維持する。（2026-09-11 / 260911-102026-add-enter-up-flick-ctrl-j）
22. Mozcは通常欄で`incognitoMode=false`と`DEFAULT_HISTORY`を使い、確定結果を端末内profileへ学習する。Mozc候補の長押しは描画token・候補source・index・editor sessionを再検証して`DELETE_CANDIDATE_FROM_HISTORY`を送る。private欄は変換engineへ入力を渡さない。（2026-09-11 / 260911-102026-enable-mozc-learning-and-user-dictionary）
23. Android個人辞書はSetupの既定ON toggleがONの場合だけ`UserDictionary.Words`を参照し、shortcutがreadingと完全一致し、localeが日本語または未指定の語を頻度順で最大6件、Mozc候補の前へ重複なく加える。Android個人辞書候補の長押しではprovider rowを削除しない。（2026-09-11 / 260911-102026-enable-mozc-learning-and-user-dictionary; 2026-09-12 / 260912-014204）
24. 候補barはcandidate sourceと表示文字列の並びが変わった時だけ横scrollを0へ戻し、同じ内容でtokenまたは選択indexだけが変わる候補巡回では現在位置を維持する。（2026-09-11 / 260911-102706-reset-candidate-scroll-on-content-change）
25. `KeyboardView`は描画矩形とtap矩形を分離する。横6dp・縦10dpの見える隙間は中点で隣接キーへ分け、ACTION_DOWNとaccessibility hit testで同じtap矩形を使う。EMPTY領域は入力せず、描画geometryと`GestureThresholds.selectionDp=18`を維持する。（2026-09-11 / 260911-132948-expand-key-hit-targets-through-gaps）
26. Setup画面は初期設定・入力設定・スラッシュコマンド候補・アプリ情報の4セクションをカードとして表示し、各操作領域を48dp以上にする。表示色はAndroidのLight/Darkへ追従し、バージョン表示は`BuildConfig.VERSION_NAME`を参照する。（2026-09-12 / 260912-002431-align-native-settings-and-voice-ui）
27. 日本語候補・ひらがな・カタカナの確定後は、通常欄の`InputConnection`からカーソル前後各128 code pointまでを取得してMozc `REQUEST_NWP`へ渡す。予測候補は通常候補欄で表示し、`SUBMIT_CANDIDATE`で直前文字列を置換せず確定して次の予測を求める。候補source、editor session、prediction generationを再確認し、かな入力、selection/editor変更、候補なしでは破棄する。privateまたは`IME_FLAG_NO_PERSONALIZED_LEARNING`欄では周辺文字列を取得せず予測を呼ばない。予測候補は長押しによる履歴削除対象外とする。（2026-09-12 / 260912-020555-add-mozc-next-word-prediction）

## 実装の注意・地雷

- `finishComposingText()` の直後に同じ候補を `commitText()` するとEditorによって二重入力になる。候補確定はcomposing領域へ直接 `commitText()` する。
- `ExtractedText.selectionStart/End` は抽出範囲内の相対位置である。`setSelection()` には `startOffset` を加えた絶対位置を渡す。
- `assets/mozc.data` はasset内パスのままMozcへ渡せない。アプリfiles directoryへcopyして `onPostLoad` する。
- `onPostLoad` 成功だけで辞書利用可能とは判定しない。data copy成功と空でないdata versionを確認する。
- `InputMethodService.currentInputConnection` はEditor切替で変わる。suspend処理の再開後にenqueue時のEditor generationを再確認する。
- 候補Viewのtapは表示時候補のsnapshotと現在候補が一致する場合だけ受理する。
- `SpeechRecognizer`の生成、listener設定、開始、停止、取消、破棄はmain threadで行う。`stopListening()`後は結果またはerrorまで再開始せず、`onFinishInputView`でも音声sessionを破棄する。
