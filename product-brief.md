# Product Brief: Android Flick Keyboard by masuidrive

Based on https://github.com/masuidrive/pdh/blob/15e6289/codex/templates/product-brief.md

本文中で「まだ決められない・確認が必要」な箇所には `[NEEDS CLARIFICATION: 具体的な問い]` を埋め込む。
coding agent はこのマーカーに触れる判断を推測で埋めず、実装を止めて確認する。解消したらマーカーを決定内容に置き換える。

## Background

スマホ幅とタブレット幅で快適に使える、端末内完結の Android 日本語 IME を作る。Foldでは閉じたスマホ幅と開いたタブレット幅の両方へ同じ操作体系で対応する。操作・寸法・状態遷移の正本は `docs/reference/sites-native-spec.txt` とする。

## Who

Androidのスマホやタブレットで、日本語と英数字を日常的に入力する人。Foldの開閉を含む幅の変化にも対応し、Chrome、Slack、LINE、ターミナル、一般的な Android のテキスト欄で使う。

## Problem

スマホとタブレットの異なる画面幅で、日本語、英字、数字、記号、カーソル操作を行き来しながら入力すると、既存キーボードでは操作が分散しやすい。入力内容を外部へ送らず、同じジェスチャー体系で素早く操作したい。

## Solution

Android の system IME として「日本語」「テンキー」「カーソル」「QWERTY」「記号」の5レイヤーを提供する。日本語は12キーフリックとかな漢字候補、英字はタップ・上スワイプ・下フリック、Space は軸固定カーソル操作を担う。Custom View で端末幅に応じて描画し、InputConnection で対象アプリへ入力し、Mozc を小さな JNI 境界の後ろへ接続する。

## Appetite

まず仕様にある5レイヤー、入力ジェスチャー、InputConnection、かな漢字変換、スマホ・タブレット幅対応を一貫して動かす。基礎入力の完成後に、端末内の変換学習と初期状態で有効なAndroid個人辞書参照を追加する。サービスログ取り込みは後続拡張とする。

## Constraints

- 対象は Android 9（API 28）以降のarm64-v8a端末で動作する Android native IME。
- Kotlin の Custom View と Mozc JNI を用いる。package は `com.masuidrive.gestureime`。
- 開発基準は Java 17、Android SDK 36、Mozc 用 Android NDK r29。Mozc の取得・ビルドには Bazelisk を使う。
- 文字入力・変換の主要経路は offline で利用できること。
- 参照資料: `https://fjsiuw2d.aboutme.style/` と `https://fjsiuw2d.aboutme.style/android-native-implementation.md`。

## Architectural Invariants

- AI-1: IME 本体はネットワーク権限を持たず、通常入力・変換を端末内だけで完結させる。
- AI-2: 入力文字列をログや端末外へ送信しない。変換学習はMozc管理の履歴、個人辞書はAndroid標準providerの参照だけを端末内で扱い、private入力欄ではどちらも利用しない。
- AI-3: UI イベントは Mozc 内部型へ直接依存せず、プロジェクト固有の小さな変換インターフェースを介する。
- AI-4: 大文字はモード化せず1回の上スワイプ結果、Ctrl/Alt は次の1キーだけの保留状態として扱う。

## Done

- Android の system IME として有効化し、仕様の5レイヤーから文字入力・編集操作ができる。
- かな入力から Mozc 候補の巡回・選択・確定までを対象アプリ上で完了できる。
- 通常欄のMozc変換を端末内で学習し、候補長押しで履歴を削除できる。初期状態でAndroid個人辞書の語を候補へ表示でき、設定で無効にできる。
- スマホ幅とタブレット幅でキー数とジェスチャーを保ったまま利用でき、Foldの開閉でも同じ操作体系を使える。
- 検証計画にある入力、回転、テーマ、文字倍率、TalkBack、対象アプリ互換の結果が記録される。

## Non-goals

- 端末外の変換 API、同期、バックアップ。
- LINE、Slack、ChatGPT、Claude 等のエクスポートログ取り込み。
- HTML モック自体を製品版として出荷すること。

## Open Questions

- Fold実機の利用可否は工程上の制約として扱う。実機未接続時はスマホ・タブレットの代表値として Android エミュレータの 412dp / 840dp 幅で検証し、実機未検証の範囲と区別して報告する。
