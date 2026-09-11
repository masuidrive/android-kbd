# Gesture IME

Galaxy Z Fold7 のカバー画面と内画面で使う、オフライン日本語入力対応の Android IME です。5つのレイヤー（かな、テンキー、カーソル、QWERTY、記号）を Custom View で描画し、同梱した Mozc 辞書でかな漢字変換します。入力文字の保存やネットワーク送信は行いません。

## 必要な環境

- JDK 17
- Android SDK 36
- Android Emulator または Android 9（API 28）以降の arm64-v8a 端末

## ビルドとテスト

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew testDebugUnitTest lintDebug assembleDebug
```

全ローカル検証は `scripts/test-all.sh` で実行します。接続済みエミュレータで実Mozcテストも行う場合は `scripts/test-all.sh --connected` を使います。接続テストは端末へAPKを導入するため、通常の検証から明示的に分けています。

APKは `app/build/outputs/apk/debug/app-debug.apk` に生成されます。

## エミュレータで試す

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell ime enable com.masuidrive.gestureime/.ImeService
adb shell ime set com.masuidrive.gestureime/.ImeService
```

アプリ一覧から **Gesture IME** を開き、「入力を試す」を選びます。物理キーボードが接続されたエミュレータでソフトウェアキーボードが出ない場合は、エミュレータ側で画面キーボード表示を有効にしてください。

左下のレイヤーキーは、左フリックを保持すると音声入力、上で日本語、右でQWERTY、下でテンキーへ切り替えます。記号は「#!」キーで開きます。候補の有無で候補欄全体の背景は変わらず、選択中候補だけをaccent色で示します。QWERTYラベル調整は正本HTMLに合わせた既定値からの相対調整として保存されます。

## プライバシー

Manifestに `INTERNET` 権限はありません。通常欄を含め入力内容をログや永続学習へ保存しません。パスワード欄と `IME_FLAG_NO_PERSONALIZED_LEARNING` 指定欄では、Mozc候補、貼り付け、音声入力を無効にします。

英数字候補は設定で明示的にONにした場合だけ動作します。入力したASCII英数字は自動置換せず、固定された端末内辞書の候補をタップした時だけ置換確定します。Spaceは原文確定後に空白を入力し、Enterは原文だけを確定します。private入力欄では英数字候補も無効です。

Android 12以降では、左下のレイヤーキーを左へフリックして保持すると、端末内日本語モデルが利用できる端末に限り音声認識を開始します。準備完了時に2回振動し、認識途中の文字を候補欄へ表示します。指を離した後の最終結果だけを現在の入力欄へ確定します。中央の長押し切り替えはありません。マイク権限はSetup画面からユーザー操作で許可します。端末内モデルがない場合は非対応と表示し、ネットワーク認識へ自動で切り替えません。

QWERTYで「/」を入力すると、設定した最大6個のスラッシュコマンド候補を表示します。初期値は`/compact`、`/clear`、`/quit`です。候補をタップした場合だけ、入力中の「/」を置き換えて確定します。
