# App-switch IME height verification

API 36.1 AVD (`1080x2400`, density 420)で、Android Settingsの検索欄とGesture IMEの入力テストを5往復した10表示を保存した。
各PNGはアプリを前面へ戻し、入力欄へfocusした直後の安定frameである。

- `settings-1.png`〜`settings-5.png`: Settings検索欄
- `test-1.png`〜`test-5.png`: Gesture IME通常入力欄
- `modes/`: QWERTY、日本語、テンキー、記号、音声の各4行表示

10表示すべてでkeyboard背景の最初の連続scanlineは物理座標`y=1545`だった。候補欄とキー群の間に追加の空白・重なりはなく、5レイヤーの上端・下端も一致した。
