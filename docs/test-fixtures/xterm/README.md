# xterm.js cursor-key probe

Android Chrome で、Gesture IME が `InputConnection.sendKeyEvent()` した DPAD/Home/End が xterm.js の hidden textarea に届くかを確認するローカル fixture です。実シェル、ネットワーク接続、アプリ配布物への同梱はありません。

## 起動

リポジトリのルートで次を実行します。

```sh
python3 -m http.server 8080 --directory docs/test-fixtures/xterm
```

Android Emulator の Chrome は `http://10.0.2.2:8080/`、USB 接続の実機は開発Macへ到達できるローカルURLで開きます。端末をタップしてから、Gesture IME の「ターミナル互換カーソルキー」をONにします。

←、→、↑、↓、Home、End をそれぞれ1回押します。各キーで `DOM keydown` と `xterm onData` が可視ログに残り、模擬入力行の caret が期待どおり変われば、ブラウザがAndroidのraw key eventをxterm textareaへ転送しています。`sendKeyEvent()` のtrueだけではDOM到達を示しません。

アプリケーションカーソルモードのANSI列は端末側の設定で変わり得るため、`xterm onData` の実値を記録します。このfixtureは通常モードの `ESC [ A/B/C/D` と `ESC [ H/F` / `ESC O H/F` を模擬editorへ反映します。

## 固定依存

| Item | Value |
| --- | --- |
| Origin | [official npm package `@xterm/xterm`](https://www.npmjs.com/package/@xterm/xterm) |
| Version | 5.5.0 |
| npm integrity | `sha512-hqJHYaQb5OptNunnyAnkHyM8aCjZ1MEIDTQu1iIbbTD/xops91NB5yq1ZK/dC2JDbVWtF23zUtl9JE2NqwT87A==` |
| npm tarball SHA-256 | `bd954fa721872170188cc5d7e83e88db3c83c9a18a4e8d24c2783d26491f59d2` |
| `vendor/xterm.js` SHA-256 | `1f991ac3b4b283ebf96e60ae23a00a52765dd3a2e46fa6fdda9f1aab032f7495` |
| `vendor/xterm.css` SHA-256 | `ba8e6985669488981ccf40c0cefe3aba80722cb6c92de7ad628b0bd717faf2b6` |
| License | MIT; copied verbatim in [LICENSE-xterm.txt](LICENSE-xterm.txt) |

Only the official package's browser bundle and stylesheet are copied. `vendor/xterm.js` is 289.4 kB and this documentation fixture is outside `app/`, so it is not packaged in the APK.
