# Technical Reference: Galaxy Z Fold7 向け Android 日本語キーボード

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
5. Fold7実機がない検証では412dpと840dp相当のエミュレータ幅を使い、実機未確認と区別して報告する。（2026-09-11 / 260910-163036）
6. Dual Flickは設定default OFFとし、利用可能幅600dp以上の日本語かなレイヤーだけ中央12キーを左右2組にする。候補・編集・レイヤー切替キーとcompositionは共有する。（2026-09-11 / 260911-000706）
7. Spaceとカーソルレイヤーの上下移動は、現在Editorから得た`ExtractedText`の改行区切りlogical line間を同じ列で移動し、文書内へclampする。視覚上のsoft wrapは別行として扱わず、抽出不能時はfocus越境を避けるためno-opにする。（2026-09-11 / 260910-233809）
8. 変換中Enterは候補確定を表示し、上で原ひらがな、左でカタカナをpreviewする。previewの確定はEnter tapで行い、raw readingは変換・復元用に保持する。（2026-09-11 / 260910-233809）
9. 音声入力はAPI 31以降の`createOnDeviceSpeechRecognizer()`だけを使用し、`ja-JP`モデルの対応を確認する。通常のnetwork recognizerへのfallbackとモデルの自動downloadは行わない。（2026-09-11 / 260910-233205）
10. 音声入力は左下レイヤーキーの単一pointerによる1秒holdだけで開始する。認識ready後に指を離すと最終結果を現在Editorへ自動確定し、候補欄からのStart/Stop/Confirm操作やpreview確定は提供しない。hold generationとeditor sessionを検証し、短tap/flick、入力欄切替、IME非表示、取消、通常キー入力後の古い結果を破棄する。（2026-09-11 / 260911-014049）
11. native描画は保存済み正本HTMLのLight/Dark paletteをAndroidの`values`/`values-night`へ対応させ、端末`uiMode`へ自動追従する。文字倍率はキー境界内へfitし、QWERTYの5群調整値は既定値からの相対scale/X/Yとして維持する。popupは候補欄の高さを変えず、非focus・非touchのoverlayで上端キーから画面内へ表示する。（2026-09-11 / 260911-022705, 260911-053738）
12. レイヤー切替は全レイヤーで左=記号、上=日本語、右=QWERTY、下=テンキーとする。候補欄の背景は候補の有無によらずkeyboard背景`#29292c`に固定し、選択候補のaccentは維持する。（2026-09-11 / 260911-022705）
13. 英数字候補は既定OFFかつprivate欄では無効とする。ON時はASCII英数字を最大64文字のcompositionとして保持し、英字だけのprefixを固定端末内辞書へ照会する。候補tapは描画token、候補source、editor session、生成世代、prefixが一致した時だけ置換確定する。Space/Enterおよびlayer・modifier・cursor・paste・voice・IME hideはraw確定境界とし、editor切替時は旧bufferを新editorへ渡さない。（2026-09-11 / 260911-003916）
14. QWERTY文字キーの下swipeでは補助labelのvisual centerを実key高の中央へ90msで移動する。保存済みlabel Y調整は選択終端から差し引き、狭幅/広幅とも中央を維持する。QWERTY以外とEnter/Pasteの下swipeは従来の13dp transitionを維持する。（2026-09-11 / 260911-055701-center-qwerty-down-swipe-label）
15. 日本語変換候補と英字補完候補は共通faceを使い、高さ34dp、最小幅82dp、左右padding 14dp、gap 5dp、radius 7dp、1dp下影、HTML 15px相当の固定15dp文字で描画する。候補欄全高50dpは上8dp・左右3dp・下8dpを含み、Light/Darkの通常/選択色はHTML key/selected paletteへ一致させる。（2026-09-11 / 260911-062719-align-candidate-ui-with-html）
16. QWERTY、記号、日本語の4行layoutは縦gap 10dpとrow pitchを共有し、外画面は55dp pitch/45dp face、内画面は62dp pitch/52dp faceとする。日本語の単一配列・Dual Flick・縦長Enterにも同じ縦geometryを適用し、横gap 6dpは維持する。（2026-09-11 / 260911-063048-unify-four-row-keyboard-heights）
17. IME入力Viewは候補欄を固定50dp、`KeyboardView`をintrinsic `WRAP_CONTENT`として縦LinearLayoutへ積み、windowの`AT_MOST`初回計測でも4行key clusterをroot desired heightへ含める。候補内容や音声状態の変更は候補欄内だけで描画し、IME root高を変えない。（2026-09-11 / 260911-063912-fix-intermittent-keyboard-vertical-offset）
18. QWERTY＋Symbolsは空白から`~`までの印字可能ASCII 95文字を網羅する。SymbolsではESCをtapのまま維持し、重複していた`"`位置をバッククォート、`/`位置を`-`の直接tapへ割り当てる。`"`と`/`はQWERTYの`l`下・`b`下へ残し、Symbolsの文字keyに方向gestureは設けない。（2026-09-11 / 260911-072234-complete-qwerty-symbol-ascii）

## 実装の注意・地雷

- `finishComposingText()` の直後に同じ候補を `commitText()` するとEditorによって二重入力になる。候補確定はcomposing領域へ直接 `commitText()` する。
- `ExtractedText.selectionStart/End` は抽出範囲内の相対位置である。`setSelection()` には `startOffset` を加えた絶対位置を渡す。
- `assets/mozc.data` はasset内パスのままMozcへ渡せない。アプリfiles directoryへcopyして `onPostLoad` する。
- `onPostLoad` 成功だけで辞書利用可能とは判定しない。data copy成功と空でないdata versionを確認する。
- `InputMethodService.currentInputConnection` はEditor切替で変わる。suspend処理の再開後にenqueue時のEditor generationを再確認する。
- 候補Viewのtapは表示時候補のsnapshotと現在候補が一致する場合だけ受理する。
- `SpeechRecognizer`の生成、listener設定、開始、停止、取消、破棄はmain threadで行う。`stopListening()`後は結果またはerrorまで再開始せず、`onFinishInputView`でも音声sessionを破棄する。
