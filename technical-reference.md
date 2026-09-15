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
- `keyboard/KeyboardView` は6レイヤーをCanvas描画し、入力意図を `KeyAction` として通知する。Editorの変更は行わない。
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
7. Spaceフリックと直接カーソル操作の上下移動は、現在Editorから得た`ExtractedText`の改行区切りlogical line間を同じ列で移動し、文書内へclampする。視覚上のsoft wrapは別行として扱わず、抽出不能時はfocus越境を避けるためno-opにする。（2026-09-11 / 260910-233809）
8. 変換中Enterは「無変換」を表示し、tapで元readingのひらがな、左または上フリックで全角カタカナをそれぞれ1回で確定する。確定後はcompositionと候補を消し、次のかな入力を新しいreadingとして開始する。（2026-09-11 / 260910-233809; 2026-09-12 / 260912-002431-commit-enter-flick-conversions）
9. 音声入力はAPI 31以降の`createOnDeviceSpeechRecognizer()`だけを使用し、`ja-JP`モデルの対応を確認する。通常のnetwork recognizerへのfallbackとモデルの自動downloadは行わない。（2026-09-11 / 260910-233205）
10. 音声入力は左下レイヤーキーを左へフリックし、方向が確定した時点で専用`VOICE` modeへ移って開始する。進入前の文字layerを保持し、最終候補tapは同じeditor sessionで1回だけ確定した後に次のrecognizer generationを直ちに開始する。取消、layer・editor・IMEの切替、privateではserviceのsession generationを無効化し、errorではcontrollerのrecognizer generationを無効化して未確定候補と旧callbackを破棄する。VOICEの最下段はCancel、認識中専用non-action slot、句読点、Space、削除、Enterの6等分とする。句読点はtap「、」・左「。」・上「？」・右「！」・下「、」を直接commitしてMozc composingへ入れない。削除はtapだけで直前の1文字を消す。削除、Space/Enterとそれらのflickは連続session、音声候補、状態を変えない。認識中だけ専用slotへ非actionの「認識中」を描画し、TalkBackには同じ非action状態nodeを1回だけ通知する。`RecognitionListener.onRmsChanged`の有限値はrecognizer generationとeditor tokenを両方照合して`KeyboardView`へ渡し、値域非保証の値を0〜1へclampして「認識中」の横の4本波形へ描画する。RMS通知がない場合は高さ最小の波形を維持する。Preview、取消、layer・editor・IME切替、errorではlevelをnullにして消し、無音再開では新generationを0から受け直す。端末非対応、権限未許可、モデルなし、認識errorは専用音声panel内へtap不能なplain textで表示する。VOICEレイヤーの左下キーは中央tapで取消、上で日本語、右でQWERTY、下でテンキーへ切り替える。（2026-09-11 / 260911-014049, 260911-055701-assign-layer-left-swipe-to-voice; 2026-09-12 / 260911-160113, 260912-122938-keep-voice-listening-until-cancel; 2026-09-14 / 260914-133849-show-realtime-voice-waveform）
11. native描画は保存済み正本HTMLのLight/Dark paletteをAndroidの`values`/`values-night`へ対応させ、端末`uiMode`へ自動追従する。文字倍率はキー境界内へfitし、QWERTYラベルは正本から決めた固定の大きさ・位置で描画する。popupは候補欄の高さを変えず、非focus・非touchのoverlayで上端キーから画面内へ表示する。（2026-09-11 / 260911-022705, 260911-053738; 2026-09-12 / 260911-175407）
12. 左下レイヤーキーは全レイヤーで左=音声入力、上=日本語、右=QWERTY、下=テンキーとし、中央tapは各キーの表示先へ切り替える。記号レイヤーは`#!`キーの中央tapから開く。候補欄の背景は候補の有無によらずkeyboard背景に固定し、選択候補のaccentは維持する。（2026-09-11 / 260911-022705, 260911-055701-assign-layer-left-swipe-to-voice）
13. 英数字候補は既定ONかつprivate欄では無効とする。ON時はASCII英数字を最大64文字のcompositionとして保持し、英字だけのprefixを固定端末内辞書へ照会する。候補tapは描画token、候補source、editor session、生成世代、prefixが一致した時だけ置換確定する。Space/Enterおよびlayer・modifier・cursor・paste・voice・IME hideはraw確定境界とし、editor切替時は旧bufferを新editorへ渡さない。（2026-09-11 / 260911-003916; 2026-09-12 / 260912-014204）
14. QWERTY文字キーの下swipeでは補助labelのvisual centerを実key高の中央へ90msで移動し、狭幅/広幅とも固定anchorから中央へ到達させる。QWERTY以外とEnter/Pasteの下swipeは従来の13dp transitionを維持する。（2026-09-11 / 260911-055701-center-qwerty-down-swipe-label; 2026-09-12 / 260911-175407）
15. 日本語変換候補と英字補完候補は共通faceを使い、高さ38dp、最小幅82dp、左右padding 14dp、gap 5dp、radius 7dp、1dp下影、HTML 15px相当の固定15dp文字で描画する。候補欄全高50dpではfaceをtop 10dp/bottom 2dpに置き、最上段key faceまでをキー行と同じ10dpにする。先頭face左はphone幅6dp、600dp以上のwide幅13dpで最左key faceと揃え、view幅変更時にも再適用する。Light/Darkの通常/選択色と、候補内容変更時の横scroll resetを維持する。音声の候補・状態表示はこの横候補欄へ混在させない。（2026-09-11 / 260911-062719-align-candidate-ui-with-html; 2026-09-12 / 260912-002431-align-native-settings-and-voice-ui, 260912-112512-align-candidate-strip-spacing, 260912-080017-show-emoji-recents-and-expand-catalog, 260912-122938-keep-voice-listening-until-cancel）
16. QWERTY、記号、日本語、テンキー、絵文字、VOICEの全4行layoutは縦gap 10dpを共有する。高さは保存済み`KeyboardHeightPreset`で決め、小/標準/大はrow pitch 50/55/60dp、face 40/45/50dp、キー領域208/228/248dpとする。600dp以上で明示的に大を選んだ場合は、プリセット導入前のwide geometryであるrow pitch 62dp・キー領域256dpを使う。未保存または不正な保存値は標準へ解決し、明示保存済みの小・標準・大はIME再生成とアプリ切替後も保持する。単一/二組の日本語配列と縦長Enterにも同じ縦geometryを適用し、横gap 6dpを維持する。（2026-09-11 / 260911-063048-unify-four-row-keyboard-heights, 260911-072658-unify-all-four-row-layout-heights; 2026-09-12 / 260912-014943-fixed-keyboard-height; 2026-09-13 / 260911-063912-fix-intermittent-keyboard-vertical-offset）
17. 日本語・テンキー左上の`☺`は絵文字レイヤーを開く。emoji2 `EmojiPickerView`の50dpカテゴリicon header（先頭Recent）とheader下8dp、3行8列の縦scroll gridを候補欄とKeyboardView上3行の位置へ重ね、最下行は既存layer flickと削除を固定する。カテゴリholderは選択状態や再利用後も全て48dp幅に固定し、header自体を横scrollさせる。RecentはSharedPreferencesで新しい順・重複なし最大100件をpicker providerと共有し、成功したString確定だけが先頭へ保存し、101件目で最古を除く。private欄ではRecentをproviderへ渡さず保存もしない。旧`CURSOR`保存値はKANAへ安全にfallbackする。（2026-09-12 / 260912-020803-replace-cursor-layer-with-emoji, 260912-055128-make-emoji-grid-scrollable, 260912-080017-show-emoji-recents-and-expand-catalog, 260912-122938-keep-voice-listening-until-cancel）
18. IME入力Viewは通常時に候補欄固定50dpとintrinsic `WRAP_CONTENT`の`KeyboardView` 4行を積み、独自のhide barは置かずOSのIME終了操作を使う。絵文字時は候補欄を50dpカテゴリheaderへ置換してpickerを上3行に重ね、KeyboardViewは最下段controlだけを露出する。音声時は専用panelを候補欄とKeyboardView上3行の位置へ重ね、最終候補を縦scroll表示し、KeyboardViewの最下段controlだけを露出する。IME内の`KeyboardView`は選択presetのintrinsic 4行高を測る。入力先切替中のhostが前の短いIME frameを`EXACTLY`で再利用し、その子のinput rootへ短い`AT_MOST`が届いた場合も、rootは候補欄と4行のintrinsic合計高へ戻し、IME Windowを`MATCH_PARENT × WRAP_CONTENT`へ再設定してdecorの再layoutを要求する。同一queue内の要求は1回にまとめる。過大な親高は採用せず、`onStartInput`と`onStartInputView`で保存値を再適用する。絵文字と音声のoverlayは最初の子measure前にwidth specから高さを同期し、wide Largeの初回だけ60dpで配置しない。IME内ではOS下端のsafe insetを`KeyboardView`の初回measure前に4行の下へseedする。insetはrow pitchへ含めず、4行key clusterの下だけを広げる。API 30以上は初期値と後続listenerの両方で`systemBars`と`systemGestures`のbottom最大値を使い、正値を受けるたびfallbackも更新する。一時的な0通知では直前の正値を維持する。初期取得時に両方0の場合はsoftware navigation resourceへfallbackし、resourceも0ならhardware navigationとして0を維持する。API 29以下はdecorのplatform insetとnavigation bar resource fallbackを使う。単体で使う`KeyboardView`は従来どおり小さい親制約とbottom insetを尊重する。（2026-09-11 / 260911-063912-fix-intermittent-keyboard-vertical-offset; 2026-09-12 / 260911-160113, 260912-002431-stabilize-ime-height-after-app-switch, 260912-014943-fixed-keyboard-height, 260912-071844-add-ime-hide-bar, 260912-080017-show-emoji-recents-and-expand-catalog, 260912-122938-keep-voice-listening-until-cancel; 2026-09-13 / 260911-063912-fix-intermittent-keyboard-vertical-offset）
19. QWERTY＋Symbolsは空白から`~`までの印字可能ASCII 95文字を網羅する。Symbolsでは`Esc`と`Tab`を直接tapとして配置し、重複していた直接`:`を除く。`:`はQWERTYの`m`下、`"`と`/`はQWERTYの`l`下・`b`下へ残し、Symbolsの文字keyに方向gestureは設けない。Tabは文字列ではなくAndroid `KEYCODE_TAB`をdown/upで送る。（2026-09-11 / 260911-072234-complete-qwerty-symbol-ascii）
20. スラッシュコマンド候補は端末内設定へ6slotで保存し、初期値を`/compact`, `/clear`, `/quit`, 空3件とする。保存時はtrimし、非空で先頭`/`がなければ補う。通常欄で`/`をcomposing保持して空欄・完全一致重複を除く最大6候補を表示し、candidate token/source/generation/editor sessionが一致するtapだけで置換確定する。private欄では候補化せず`/`を直接確定する。（2026-09-11 / 260911-055701-configure-slash-command-candidates）
21. 端末内音声認識はpartial resultsを要求し、最新の途中結果と状態を専用音声panelへ表示する。`onEndOfSpeech`後500ms以内に最終結果がない場合、または`NO_MATCH`・`SPEECH_TIMEOUT`・`CLIENT` errorで終了した場合は、最後の有効な途中結果を選択可能な最終候補へ昇格する。有効な途中結果がない連続音声sessionで`NO_MATCH`または`SPEECH_TIMEOUT`になった場合は、errorを表示せず「認識中」を保ち、250ms後に同じeditor tokenで新しい端末内recognizerを開始する。最終候補は空文字と重複を除き、1行1候補の固定高faceとして縦scroll一覧へ順序どおり表示する。複数候補では各候補の文字列を文字単位で比較し、全候補に共通しない文字だけを太字とaccent色で示す。同一候補だけ、または1候補だけなら強調しない。描画token、editor session、連続session generationが一致する候補tapだけを1回確定して次回認識へ戻す。取消、無音以外のerror、IME終了、入力欄切替では未確定結果と再開予約を破棄し、自動再試行しない。日本語変換、英字補完、スラッシュ候補は従来の1行表示と横スクロールを保つ。（2026-09-11 / 260911-060238-align-voice-status-ui-and-show-partials; 2026-09-12 / 260911-160113, 260911-234748, 260912-122938-keep-voice-listening-until-cancel; 2026-09-14 / 260914-013111-highlight-voice-candidate-differences, 260914-075602-restart-voice-after-silence-timeout）
22. 変換中でないEnterは上フリックでCtrl+Jを同一gesture内のkey eventとして送り、待機中に上方向ラベルを出さず、選択中だけ`C-j`を17sp相当で中央表示する。下フリックPaste、tap Enterを維持する。変換中のEnterは「無変換」と表示し、tapで読みを無変換確定、左または上フリックでカタカナ確定する。（2026-09-11 / 260911-102026-add-enter-up-flick-ctrl-j）
23. Mozcは通常欄で`incognitoMode=false`と`DEFAULT_HISTORY`を使い、確定結果を端末内profileへ学習する。Mozc候補の長押しは描画token・候補source・index・editor sessionを再検証して`DELETE_CANDIDATE_FROM_HISTORY`を送る。private欄は変換engineへ入力を渡さない。（2026-09-11 / 260911-102026-enable-mozc-learning-and-user-dictionary）
24. Android個人辞書はSetupの既定ON toggleがONの場合だけ`UserDictionary.Words`を参照し、shortcutがreadingと完全一致し、localeが日本語または未指定の語を頻度順で最大6件、Mozc候補の前へ重複なく加える。Android個人辞書候補の長押しではprovider rowを削除しない。（2026-09-11 / 260911-102026-enable-mozc-learning-and-user-dictionary; 2026-09-12 / 260912-014204）
25. 候補barはcandidate sourceと表示文字列の並びが変わった時だけ横scrollを0へ戻し、同じ内容でtokenまたは選択indexだけが変わる候補巡回では現在位置を維持する。（2026-09-11 / 260911-102706-reset-candidate-scroll-on-content-change）
26. `KeyboardView`は描画矩形とtap矩形を分離する。横6dp・縦10dpの見える隙間は中点で隣接キーへ分け、ACTION_DOWNとaccessibility hit testで同じtap矩形を使う。EMPTY領域は入力せず、描画geometryと`GestureThresholds.selectionDp=18`を維持する。（2026-09-11 / 260911-132948-expand-key-hit-targets-through-gaps）
27. Setup画面は初期設定・入力設定・スラッシュコマンド候補・アプリ情報の4セクションをカードとして表示し、各操作領域を48dp以上にする。表示色はAndroidのLight/Darkへ追従し、バージョン表示は`BuildConfig.VERSION_NAME`を参照する。（2026-09-12 / 260912-002431-align-native-settings-and-voice-ui）
28. 日本語候補・ひらがな・カタカナの確定後は、通常欄の`InputConnection`からカーソル前後各128 code pointまでを取得してMozc `REQUEST_NWP`へ渡す。予測候補は通常候補欄で表示し、`SUBMIT_CANDIDATE`で直前文字列を置換せず確定して次の予測を求める。候補source、editor session、prediction generationを再確認し、かな入力、selection/editor変更、候補なしでは破棄する。privateまたは`IME_FLAG_NO_PERSONALIZED_LEARNING`欄では周辺文字列を取得せず予測を呼ばない。Mozc予測候補の長押しは履歴削除を要求し、返った予測stateを再表示する。Android個人辞書候補は長押し削除対象外とする。（2026-09-12 / 260912-020555-add-mozc-next-word-prediction）
29. `SetupActivity`はedge-to-edgeで描画し、`systemBars`と`displayCutout`の各辺の大きい方を使う。top insetと56dp app barは固定し、48dpのripple付き戻るiconは`finish()`する。設定用`ScrollView`だけをその下で動かし、左右とbottom insetはcontent paddingへ毎回基準値から反映する。Light/Darkのstatus/navigation bar icon appearanceとtransparent system barを明示する。（2026-09-12 / 260912-021714-fix-setup-safe-area-and-app-bar）
30. Setupの入力設定には48dp以上の「キーボードの高さ」RadioGroupを置き、小・標準・大を端末内SharedPreferencesへ保存する。文字列以外または未知の値は標準へ安全にfallbackする。（2026-09-12 / 260912-014943-fixed-keyboard-height）
31. 日本語候補tapは表示snapshotのindexからMozc `CandidateWord.id`を引き、`SUBMIT_CANDIDATE`でその候補を直接確定する。outputにpreeditが残る複数文節だけ`SUBMIT`で残りを確定して後ろへ連結する。候補を確定するために`SELECT_CANDIDATE`と`SUBMIT`を続けて送らない。（2026-09-13 / 260913-112709-select-exact-tapped-conversion-candidate）

## 実装の注意・地雷

- `finishComposingText()` の直後に同じ候補を `commitText()` するとEditorによって二重入力になる。候補確定はcomposing領域へ直接 `commitText()` する。
- `ExtractedText.selectionStart/End` は抽出範囲内の相対位置である。`setSelection()` には `startOffset` を加えた絶対位置を渡す。
- `assets/mozc.data` はasset内パスのままMozcへ渡せない。アプリfiles directoryへcopyして `onPostLoad` する。
- `onPostLoad` 成功だけで辞書利用可能とは判定しない。data copy成功と空でないdata versionを確認する。
- `InputMethodService.currentInputConnection` はEditor切替で変わる。suspend処理の再開後にenqueue時のEditor generationを再確認する。
- 候補Viewのtapは表示時候補のsnapshotと現在候補が一致する場合だけ受理する。
- `SpeechRecognizer`の生成、listener設定、開始、停止、取消、破棄はmain threadで行う。`stopListening()`後は結果またはerrorまで再開始せず、`onFinishInputView`でも音声sessionを破棄する。
