# Gesture IME — Android Native 実装仕様 0.2

## 目的

Galaxy Z Fold7 の外画面と内画面で使う Android IME を実装する。英字は QWERTY の各キーをタップして小文字、上スワイプでその1文字だけ大文字、下フリックで数字または ASCII 記号を入力する。日本語は連打なしの12キーフリックとし、候補列でかな漢字変換を行う。

HTMLモックは操作、寸法、状態遷移を確認するリファレンスである。製品版では `InputMethodService`、`InputConnection`、Mozc などの変換エンジンを使う。

画面と文書ではレイヤー名を「日本語」「テンキー」「カーソル」「QWERTY」「記号」に統一する。`language` と `layer` は実装上の内部状態名として扱う。

## キー配列

英字1〜3行目の基本幅を `1w` とする。2行目だけ左端と右端に `0.5w` のジェスチャー専用キーが入る。

| 行 | QWERTYレイヤー | 記号レイヤー |
|---|---|---|
| 1 | `q w e r t y u i o p` | `1 2 3 4 5 6 7 8 9 0` |
| 2 | `C/A(0.5w) a…l BS(0.5w)` | C/A、`^ _`、backslash、vertical bar、`~`、波括弧、角括弧、BS |
| 3 | `# z x c v b n m , .` | AZ、backtick、double quote、`! ? ; : < > /` |
| 4 | `あん Space Enter` | `あん Space Enter` |

`C/A` は記号の副ラベルと同じ高さになるよう、上側の `C` と下側の `A` に少し間を空けて固定する。上スワイプで Alt、下スワイプで Ctrl を次の1キーへ適用する。方向選択中と選択後は上下ラベルを消して、中央に選ばれた `A` または `C` を表示する。選択後のタップは修飾状態を解除する。未選択の`C/A`と`BS`はタップでも上スワイプでもポップアップを出さない。`BS`は下フリックだけで候補表示と削除を有効にする。QWERTYレイヤーの `#` は中央の `#` と右後ろの淡い `!` を重ねた `#!` キーで記号へ入り、記号レイヤーでは中央の `A` と右後ろの淡い `Z` を重ねた `AZ` キーでQWERTYへ戻る。どちらも暗い背景のモードキーとする。記号レイヤーは ASCII 文字だけを収録する。

4行目のレイヤー切替キーはQWERTY形式では`あん`、テンキー形式では`AZ`とする。タップはQWERTY形式から日本語レイヤーへ、テンキー形式からQWERTYレイヤーへ移動する。全レイヤーで左フリックは記号、上フリックは日本語、右フリックはテンキー、下フリックはQWERTYへ移動する。方向選択中は元の補助文字を消し、キー中央に移動先のレイヤー名だけを表示する。`#!`、`あん`、`AZ`、`19` の補助文字は主ラベルの約75%のサイズで揃える。`Enter` は上側に小さく `paste` を常時表示し、下スワイプ中は中央ラベルを小文字の `paste` へ変える。離すとクリップボード内容を確定する。QWERTYの`Space`は上側に補助表示 `←↓↑→` を置き、操作中も同じ記号列を初期表示に使う。

日本語・テンキーレイヤーは左上の`☺`から絵文字を開く。絵文字面はrecent最大8件の1行と16件ずつの一覧2行を8列に揃え、最下行の既存layer flick、前頁、頁表示、次頁、削除で4行内に収める。recentは端末内で新しい順・重複なしに保存し、空枠は空のままにする。日本語とテンキーの空白キーは`Space`とだけ表示するが、フリックで4方向カーソル移動へ入る。トラックパッドは動き始めの主方向から横軸または縦軸を選び、指を離すまで固定するため斜め移動はしない。

## 推奨するAndroid構成

```text
ImeService : InputMethodService
├── KeyboardRootView : ViewGroup
│   ├── CandidateStripView
│   ├── KeyGridView
│   └── GestureOverlayView
├── KeyboardState
│   ├── language / layer / deviceMode
│   ├── pendingModifier
│   ├── activePointers
│   └── composition
├── GestureInterpreter
├── TextInputController
└── ConversionEngine
    └── MozcBridge (JNI)
```

描画は Custom View を推奨する。キー数が少なくポインター追跡と座標制御が中心なので、Viewの再生成よりも、レイアウトデータと描画状態を分けて `invalidate()` する方が実装しやすい。候補列は横スクロール可能な独立Viewにする。

## 状態モデル

| 表示するレイヤー名 | 内部状態 |
|---|---|
| 日本語 | `language = KANA`, `layer = LETTERS` |
| テンキー | `language = KANA`, `layer = NUMBERS` |
| 絵文字 | `language = KANA`, `layer = EMOJI` |
| QWERTY | `language = ENGLISH`, `layer = LETTERS` |
| 記号 | `language = ENGLISH`, `layer = SYMBOLS` |

```kotlin
data class KeyboardState(
    val language: Language = Language.ENGLISH,
    val layer: Layer = Layer.LETTERS,
    val deviceMode: DeviceMode = DeviceMode.COVER,
    val pendingModifier: Modifier? = null,
    val composition: Composition = Composition.Empty,
    val cursorMode: Boolean = false,
)

data class Composition(
    val reading: String,
    val candidates: List<String>,
    val selectedIndex: Int,
    val converted: Boolean,
)
```

大文字はモードにせず、英字キーの1回の上スワイプ結果として扱う。Ctrl と Alt は1キーだけ有効な保留状態で、入力後またはキャンセル時に解除する。複数指入力では `pointerId` ごとに開始座標、現在方向、対象キー、タイマーを保持する。

## 日本語変換UI

日本語レイヤーではキーの上に候補列を常時確保する。未入力時は説明文、読みがある時は変換候補を横並びで表示する。候補は選択中だけアクセント色にする。候補faceと最上段key faceの間隔はキー行間と同じ10dpとし、先頭faceの左外周はphone幅6dp、600dp以上のwide幅13dpで最左key faceに揃える。

1. フリック確定ごとに読みを `setComposingText()` で更新する。
2. 変換エンジンへ読みと文脈を渡し、候補列を更新する。
3. space のタップで先頭候補を選び、続けて押すと次候補へ進む。
4. 候補タップまたは Enter で `commitText()` して変換を確定する。
5. 読みがない時の space は空白、変換中でない時の Enter は改行にする。
6. Backspace、カーソル移動、言語・レイヤー切替では、変換中状態を確定または破棄する規則を一箇所に集約する。

HTMLデモは内蔵の小さな辞書で候補UIと状態遷移だけを再現する。製品版の候補生成、文節分割、学習は Mozc 側へ委譲する。

## ジェスチャー定数

| 用途 | 値 | 動作 |
|---|---:|---|
| 軸ロック | 12 px | 縦・横の優勢方向を確定 |
| 選択開始 | 18 px | フリック先を選択 |
| 復帰ヒステリシス | 10 px | 選択後の細かな揺れを抑える |
| レイヤー移動 | 18 px | あんの上・下、Enterの下を確定 |
| spaceトラックパッド開始 | 10 px | 長押しを待たずカーソルモードへ入る |
| 水平カーソル1文字 | 8 px | 書記素単位で左右移動 |
| 垂直カーソル1行 | 24 px | 見た目の折返し行に沿って上下移動 |
| 長押し | 450 ms | 英字アクセント候補を表示 |
| 連続削除開始 | 420 ms | 以後65 ms間隔で繰り返す |

端末の物理ピクセルではなく、Android側では `dp` と実機テストから閾値を調整する。開始後に方向を変えた場合は現在位置から再判定し、指が中心へ戻ったら通常入力へ戻す。`ACTION_CANCEL`、フォーカス喪失、画面回転では入力を確定せず全ポインターを破棄する。

## フリックラベルのアニメーション

英字キーには中央の主ラベルと上端の副ラベルを別レイヤーで描く。通常時の副ラベルは主ラベルより小さく、文字色を一段落として、キーを見た瞬間に主入力と区別できるようにする。

下フリックが18pxを越えたら、副ラベルを90msの `ease` で下へ13px移動し、1.7倍へ拡大する。同時に主ラベルを下へ22px移動し、透明度を0へ落とす。中心へ10px以内に戻ったら逆再生する。上スワイプでは主ラベルを上へ3px移動し、副ラベルを透明にして、大文字のプレビューを表示する。

日本語フリックは150×150px相当の5方向ポップアップを表示し、中心と上下左右を50px角で配置する。指の方向が変わるたびに選択タイルの背景色を切り替える。ポップアップ自体は追従移動させず、選択状態だけを更新するため、視線が安定する。ダーク表示の候補は暗い面と明るい文字の関係を保ち、通常キーより一段明るい面、境界線、影で浮かせる。表示中は選択元以外のキーを42%の不透明度へ落とす。

Androidでは `ValueAnimator` または描画時の補間値で同じ状態を作る。アニメーションは入力判定から独立させ、指を離した時点の論理状態を確定値にする。システムの「アニメーションを無効化」が有効な場合は時間を0msにする。

## アプリへの文字送信

通常文字は `currentInputConnection.commitText(text, 1)`、変換中の読みは `setComposingText(text, 1)`、候補確定は `finishComposingText()` と `commitText()` を使う。PasteはAndroidの `ClipboardManager.primaryClip` から先頭のテキストを取得し、機密入力欄を除いて `commitText()` する。Backspace、Enter、矢印、Ctrl、Alt など編集キーは対象アプリとの互換性を確認し、必要な場合だけ `sendKeyEvent()` を使う。

```kotlin
fun sendModifiedKey(keyCode: Int, ctrl: Boolean, alt: Boolean) {
    val meta = (if (ctrl) KeyEvent.META_CTRL_ON else 0) or
        (if (alt) KeyEvent.META_ALT_ON else 0)
    currentInputConnection.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
    currentInputConnection.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, meta))
}
```

パスワード欄では学習、候補収集、外部ログ由来辞書を無効にする。アプリによってハードウェアキーイベントの扱いが異なるため、Chrome、Slack、LINE、ターミナル、一般的なEditTextで確認する。

## Mozcによる日本語変換

OSSで実用的な候補は Mozc。Androidアプリへ組み込む場合は、変換セッションを保持するC++層とKotlinから呼ぶJNI境界を小さく保つ。UIイベントをMozc内部型へ直接結びつけず、`start(reading)`、`update(reading)`、`nextCandidate()`、`commit(index)`、`reset()` 程度の独自インターフェースを置く。

Mozc本体、辞書、ビルド成果物のライセンス表示を配布前に確認する。AOSPや既存IMEのUIコードを流用する場合も、コードとアセットそれぞれのライセンスを確認する。

## 個人辞書とログ取り込み

端末の標準ユーザー辞書は `UserDictionary.Words` を、ユーザーが明示的に許可した場合だけ読み込む。LINE、Slack、ChatGPT、Claudeなどのログは、各サービスの公式エクスポートをユーザー自身が選択して端末内へ取り込み、頻出語と読み候補を生成する方式が扱いやすい。

取り込み前にプレビュー、対象期間、除外語を表示する。原文を保存せず、抽出した語と頻度だけを暗号化して保持できる設計にする。連絡先、URL、メールアドレス、トークン、長い数字列、パスワードらしい文字列は既定で除外する。削除と再学習の操作を設定画面に用意する。

## 描画とアクセシビリティ

Fold7外画面を狭い基準、内画面を広い基準として、幅からキー単位 `w` を計算する。内画面でもキー数とジェスチャーは変えず、幅は列と左右余白だけを変える。キー4行の高さは利用者が選んだ小・標準・大presetで決め、幅やDual Flickでは変えない。ヒンジやセーフ領域は `WindowInsets` を使って避ける。

副ラベルは色だけでなく位置とサイズでも主ラベルと区別する。押下、フリック選択、候補選択には4.5:1以上の文字コントラストを保つ。TalkBackには通常入力、上スワイプ、下フリックの結果をまとめた説明を付ける。触覚は方向確定、候補変更、削除開始に短く1回だけ返す。

## セキュリティ

IMEは全入力へ触れられるため、ネットワーク権限を持たない完全ローカル構成を初期値とする。ログへ入力文字列を出さない。学習データ、個人辞書、クラッシュ情報に原文を含めない。バックアップと同期を追加する場合は、対象データと送信先を明示し、利用者が明示的に有効化する。

## 検証計画

- 各かなキーの中央・左・上・右・下と、18px未満の揺れを検証する。
- 全英字のタップ、上スワイプ大文字、下フリック副文字を検証する。
- `C/A` と `BS` がタップでは発火せず、指定方向だけで動くことを検証する。
- あんの上下レイヤー移動、Enterの下Paste、スワイプ中だけ変わるラベルを検証する。
- 日本語左列のカーソル、#!、19、AZと、テンキー−の5方向を検証する。
- ラベルアニメーションの開始、中心復帰、方向反転、アニメーション無効設定を検証する。
- かな入力、候補生成、spaceでの候補巡回、候補タップとEnter確定を検証する。
- Spaceの軸固定された上下左右移動、斜め入力の抑止、折返し行、絵文字、結合文字、選択範囲を検証する。
- Fold7の外画面・内画面、縦横回転、ダークテーマ、フォント倍率、TalkBackを確認する。
- Chrome、Slack、LINE、ターミナル、一般的なEditTextで文字確定と編集キーを確認する。

## 実装順序

1. レイアウトデータ、状態モデル、Custom Viewを作る。
2. ポインター追跡と日本語5方向フリックを実装する。
3. 英字の上下ジェスチャー、C/A、BS、ラベルアニメーションを加える。
4. InputConnectionで通常文字、編集キー、compositionを送る。
5. 候補列UIと変換エンジン境界を作り、Mozcを接続する。
6. カーソルモード、Fold7内外レイアウト、アクセシビリティを仕上げる。
7. 実機テストで閾値、誤入力率、候補応答時間を調整する。
