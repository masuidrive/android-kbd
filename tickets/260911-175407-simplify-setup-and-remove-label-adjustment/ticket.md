---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "QWERTYラベル調整機能を撤去し、セットアップ画面を整理してバージョンを表示する"
created_at: "2026-09-11T17:54:07Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-175407-simplify-setup-and-remove-label-adjustment

### Why
個別のQWERTYラベル調整は設定を複雑にし、標準のキー表示との整合も崩しやすい。
セットアップ画面を初期設定と日常的な入力設定に絞り、現在のアプリ版をその場で確認できるようにする。

### What / Acceptance Criteria
この ticket が終わると、利用者が迷わず初期設定と入力設定を行い、利用中のバージョンを確認できる。

- [ ] AC 1: セットアップ画面とアプリ内の導線からQWERTYラベル調整機能がなくなり、以前に保存した調整値もキー表示へ影響しない。
- [ ] AC 2: セットアップ画面に、インストールされているアプリのバージョンが表示される。
- [ ] AC 3: セットアップ画面で初期設定、入力設定、スラッシュ候補、アプリ情報のまとまりが見分けられ、各操作を48dp以上のタップ領域から実行できる。
- [ ] AC 4: Light/Darkの両テーマとスマホ・タブレット相当の幅で、文字や操作が欠けず縦スクロールで全項目へ到達できる。
- [ ] AC 5: 既存のIME有効化、IME選択、入力テスト、各トグル、マイク権限、スラッシュ候補保存、ライセンス表示が引き続き動作する。

### Architectural Invariants check
端末内で完結するIME、明示的な権限要求、入力内容を外部送信しない既存方針と矛盾しない。

### Design Decisions
- バージョンはビルド設定の `versionName` を表示し、画面文言との二重管理を避ける。
- 画面は初期設定、入力設定、スラッシュ候補、アプリ情報の順に並べ、利用頻度と作業順に沿わせる。
- QWERTYラベル調整は導線だけでなくActivity、保存API、描画への適用を撤去し、廃止後の隠れた状態依存を残さない。

### Out-of-scope
- キーボード本体の固定ラベル位置、キー配列、フリック割り当ては変更しない。
- アプリ名、applicationId、versionCode、versionNameは変更しない。
- サイト全体の再デザインや公開は行わない。ただし削除機能を説明する既存文言は整合のため除去する。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「アプリからqwertyラベル調整機能を削除」「設定ページにバージョン表示」「設定ページをもう少し良くする」。

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
