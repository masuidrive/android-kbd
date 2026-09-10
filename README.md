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

## プライバシー

Manifestに `INTERNET` 権限はありません。通常欄を含め入力内容をログや永続学習へ保存しません。パスワード欄と `IME_FLAG_NO_PERSONALIZED_LEARNING` 指定欄では、Mozc候補と貼り付けを無効にします。
