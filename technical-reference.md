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
- `CandidateStripView` はキーボードの兄弟Viewであり、候補選択をindexでServiceへ戻す。
- Mozcの共有ライブラリ、辞書、protobuf jarをAPKへ同梱するため、変換時にネットワークを必要としない。

## Design decisions

1. package/Application IDは `com.masuidrive.gestureime` とする。Mozc JNI登録対象だけは上流互換の `com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI` に固定する。（2026-09-11 / 260910-163036）
2. compileSdk/targetSdk 36、minSdk 28、Java 17、arm64-v8aをv1の基準とする。（2026-09-11 / 260910-163036）
3. 候補待ちを含む入力actionは順序を保つ。Editor lifecycle generationと変換generationが一致しない非同期結果は破棄する。（2026-09-11 / 260910-163036）
4. Mozcはincognito/learning無効で利用し、入力内容を永続化しない。secret欄では変換処理そのものを呼ばない。（2026-09-11 / 260910-163036）
5. Fold7実機がない検証では412dpと840dp相当のエミュレータ幅を使い、実機未確認と区別して報告する。（2026-09-11 / 260910-163036）

## 実装の注意・地雷

- `finishComposingText()` の直後に同じ候補を `commitText()` するとEditorによって二重入力になる。候補確定はcomposing領域へ直接 `commitText()` する。
- `ExtractedText.selectionStart/End` は抽出範囲内の相対位置である。`setSelection()` には `startOffset` を加えた絶対位置を渡す。
- `assets/mozc.data` はasset内パスのままMozcへ渡せない。アプリfiles directoryへcopyして `onPostLoad` する。
- `onPostLoad` 成功だけで辞書利用可能とは判定しない。data copy成功と空でないdata versionを確認する。
- `InputMethodService.currentInputConnection` はEditor切替で変わる。suspend処理の再開後にenqueue時のEditor generationを再確認する。
- 候補Viewのtapは表示時候補のsnapshotと現在候補が一致する場合だけ受理する。
