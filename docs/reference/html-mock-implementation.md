# Gesture IME — HTMLモック作成ガイド 0.2

この文書は、配布用の単一HTMLモックを再構築・変更するための設計資料です。画面仕様は `src/keyboard.html`、組み立て処理は `scripts/build_demo.py`、公開ページ生成は `scripts/build_site.py` に置きます。

## ファイル構成

| ファイル | 役割 |
|---|---|
| `src/keyboard.html` | キーボード本体。HTML断片、CSS、JavaScriptを1ファイルに収録 |
| `scripts/build_demo.py` | 単体で開ける `site/demo.html` を生成 |
| `scripts/build_site.py` | モックとAndroid仕様を組み込んだSites用ページを生成 |
| `docs/android-native-implementation.md` | Android Native実装仕様 |
| `docs/html-mock-implementation.md` | この作成ガイド |
| `outputs/index.html` | ローカル確認用の完成HTML |

配布HTMLを1枚に保ちつつ、ソース内ではレイアウト、状態、入力処理、描画、ポインター処理を分けます。外部ライブラリとネットワーク通信は使いません。

## DOM構造

```text
#ios-keyboard-demo
├── .device-picker
└── .phone
    ├── textarea
    ├── .feedback
    ├── .keyboard
    │   ├── .candidate-bar
    │   ├── .keys
    │   ├── .trackpad-status
    │   └── .popup
    └── .reopen
```

`.candidate-bar` は日本語変換候補、`.keys` は現在レイヤーのキー、`.trackpad-status` は移動方向、`.popup` はフリックまたは長押し候補を表示します。言語切替はキーボード内のレイヤーキーで行い、キーボード外の補助ガイドは置きません。固定HTMLは容器だけにして、キーと候補は状態から描画します。

## コードの分割方針

`keyboardRows()` は状態からキー配列データを返します。`createKeyElement()` はキー1個のDOMだけを作ります。`renderKeys()` と `renderCandidates()` は各表示領域を更新し、`render()` はこれらを呼ぶ調整役に留めます。

文字挿入、削除、カーソル移動、変換候補置換は描画処理から呼び出せる独立関数にします。ポインターイベントは状態を更新してそれらの関数を呼び、DOMの見た目だけで入力結果を決めないようにします。

## 状態

ユーザー向けのレイヤー名は「日本語」「テンキー」「絵文字」「QWERTY」「記号」「音声」とする。絵文字recentは端末内だけに新しい順・重複なしで保存する。内部状態との対応は次の通り。

| 表示するレイヤー名 | 内部状態 |
|---|---|
| 日本語 | `language = kana`, `layer = letters` |
| テンキー | `language = kana`, `layer = numbers` |
| 絵文字 | `language = kana`, `layer = emoji` |
| QWERTY | `language = english`, `layer = letters` |
| 記号 | `language = english`, `layer = symbols` |
| 音声 | `voiceDemo.active = true` |

```text
language          english / kana
layer             letters / symbols / numbers / emoji
pendingModifier   null / Control / Alt
active             pointerIdごとの押下状態
composition       reading / range / candidates / selected / converted
device            cover / inner
```

英字の大文字は永続状態にせず、そのポインターの `upperSelected` だけで決めます。Ctrl/Altは次の1キーで消費します。日本語変換は読みの開始・終了位置を保持し、候補で置換しても元の読みを失わないようにします。

## レイアウト

QWERTYの1〜3行目は基本キーを `1w` とし、2行目だけ `C/A 0.5w + a〜l 9w + BS 0.5w` にします。3行目は `# + z〜.` で合計10w。`#` は`AZ`と同じ重ね位置に淡い `!` を置く`#!`構成です。4行目の日本語切替キーも中央の `あ` と、同じ位置に重ねる淡い `ん` の`あん`構成にして、Space、Enterと並べます。`#!`、`あん`、`AZ`、`19` の補助文字は主ラベルの約75%に揃えます。記号レイヤーも同じ外形を保ち、`#` の位置は中央の `A` と右後ろの淡い `Z` を重ねたQWERTY復帰キーに変えます。Enterは上側に小さな `paste` を常時表示し、下スワイプ中は中央ラベルを小文字の `paste` へ変えて、離した時に貼り付けを実行します。

日本語・テンキーの左上`☺`は絵文字レイヤーを開きます。絵文字レイヤーはrecent最大8件と同梱catalogを空行なしで続ける上3行・8列の縦scroll viewportと、layer flick・削除の固定最下行で4行に収めます。recentが空ならcatalog先頭24件を表示し、recentはlocalStorageで新しい順・重複なしに保存します。日本語とテンキーの空白キーは `Space` と表示し、フリックすると上下左右のカーソル移動に入ります。QWERTYの`Space`は方向表示 `←↓↑→` を主ラベルの上に置き、トラックパッド開始時の中央ステータスにも同じ記号列を表示します。

Fold7外画面は最大412px、内画面は最大840pxのコンテナで確認します。内画面でも配列とキー高を変えず、列と左右余白だけを変えます。

## ジェスチャー判定

開始点から12pxで軸を固定し、18pxで選択を開始します。選択後は10pxのヒステリシスで中心復帰を判定します。日本語は距離18px以上で、横優勢なら左・右、縦優勢なら上・下を選びます。

英字は上スワイプで大文字、下フリックで副ラベルの文字です。横へ軸ロックした後は上下の入力へ変えません。`C/A` は記号の副ラベルと同じ高さになるよう少し内側へ固定表示し、下でCtrl、上でAltを選びます。選択中と選択後は中央にCまたはAを表示し、選択後のタップで解除します。未選択の`C/A`と`BS`はタップでも上スワイプでもポップアップを出しません。`BS`は下フリックだけで候補と削除を有効にします。レイヤー切替キーとEnterは方向判定中のラベルと背景色を更新し、指を離した時にレイヤー移動またはPasteを確定します。

Spaceは10px動いた時点でトラックパッドへ入ります。その時点で移動量が大きい方を横軸または縦軸として選び、指を離すまで固定します。横は8pxごとに書記素1個、縦は24pxごとに表示上の1行を移動し、斜めには動きません。移動中はキートップを隠し、中央に現在方向を表示します。

## ラベルアニメーション

各英字キーは `.key-main` と `.key-alt` を重ねます。通常時は副ラベルを上から3px、小さい11px文字、控えめな色で描きます。

下フリック選択中は `.flick-selected` を付けます。副ラベルを `translateY(13px) scale(1.7)`、主ラベルを `translateY(22px)` と `opacity:0` にし、90msの `ease` で切り替えます。中心へ戻るとクラスを外し、同じ時間で元へ戻します。

上スワイプ選択中は `.uppercase-selected` を付けます。主ラベルを `translateY(-3px)`、副ラベルを `opacity:0` にして、中央には大文字を表示します。OSの視差軽減設定に対応するため、`prefers-reduced-motion: reduce` ではtransitionを無効にします。

日本語は150×150pxのポップアップに50×50pxの5候補を十字配置します。現在方向の候補だけ `.chosen` にし、背景色を切り替えます。方向変更時にポップアップの位置は動かしません。ダーク表示では白黒を反転させず、通常キーより一段明るいダーク面と白文字を使います。表示中は選択元以外のキーを42%の不透明度に落とし、細い境界線と影で前後関係を示します。

## 日本語変換デモ

日本語レイヤーでは候補列を常時表示します。入力欄のカーソル直前から連続するひらがなを読みとして抽出し、内蔵の小さな辞書、読みそのもの、カタカナ表記を候補にします。

spaceをタップすると先頭候補へ変換し、続けてタップすると候補を巡回します。候補を直接タップすると確定します。Enterは変換中なら確定だけを行い、変換中でなければ改行します。候補置換中は通常の `input` 同期を一時停止し、読みと置換範囲を維持します。

この辞書はUI確認専用です。文節分割、予測、学習、文脈候補は実装しません。Android版では同じUI状態をMozcなどの変換エンジンへ接続します。

## イベント処理

`pointerdown` でキー、開始座標、修飾状態を保存します。`pointermove` で方向、候補、ラベルクラスを更新します。`pointerup` で現在選ばれている論理結果を1回だけ確定します。`pointercancel`、`lostpointercapture`、画面非表示では確定せず状態を破棄します。

キーボードクリックによる操作も残し、開発者ツールやアクセシビリティ操作で各キーを試せるようにします。候補ボタンは `role=option` と `aria-selected`、モードキーは `aria-pressed`、結果表示は `aria-live` を使います。

## ビルドと確認

```bash
npm run build
npm test
npm run test:browser
```

`npm run build` はソース断片から単体デモとSitesページを作ります。ローカル配布用には生成した `site/demo.html` を `outputs/index.html` としてコピーします。ブラウザ確認では日本語5方向、英字上下、C/A、BS、候補変換、Spaceカーソルの軸固定、Fold7内外、ライト・ダーク、横はみ出し、実行時エラーを確認します。
