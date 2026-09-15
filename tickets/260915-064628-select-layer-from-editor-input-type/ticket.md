---
priority: 2
base_branch: default
description: "呼び出し元の入力種別に合う初期レイヤーを選ぶ"
created_at: "2026-09-15T06:46:28Z"
started_at: 2026-09-15T06:51:02Z
closed_at: null
canceled_at: null
---

## 260915-064628-select-layer-from-editor-input-type

### Why

入力欄が数字・電話番号・URLなどを要求しても、現在は前回使ったレイヤーのまま開くため、入力前に不要なレイヤー切替が発生する。
Product Brief の「同じジェスチャー体系で素早く操作したい」に沿って、Android が通知する入力種別から適切な初期レイヤーを選ぶ。

### What / Acceptance Criteria

数字・電話番号・日時・URL・メール・パスワードを入力する利用者が、入力欄を開いた直後から用途に合うレイヤーで入力できるようになる。

- [ ] AC 1: 数字、符号付き数、小数、数字パスワード、電話番号、日時の入力欄では、最初からテンキーレイヤーが表示される。
- [ ] AC 2: URL、メール、Webメール、文字パスワード、可視パスワード、Webパスワード、および数字・電話・日時以外でASCIIを強制する入力欄では、最初からQWERTYレイヤーが表示される。
- [ ] AC 3: 通常の文章、名前、住所、メッセージ、未指定または未知の入力種別では、利用者が最後に明示選択したレイヤーが表示される。
- [ ] AC 4: 入力種別による自動選択だけでは最後の明示選択を変更せず、入力中は従来どおり手動で別レイヤーへ切り替えられる。
- [ ] AC 5: 最初の表示、入力欄・アプリの切替、入力ビューの再生成でも、スマホ幅とタブレット幅のキーボードの高さや安全領域が崩れない。

### Architectural Invariants check

入力種別はAndroid標準のローカル情報だけを使い、AI-1からAI-4と矛盾しない。

### Design Decisions

- `EditorInfo.inputType` のclass/variationと `IME_FLAG_FORCE_ASCII` から、入力開始時の表示レイヤーだけを決める。数字・電話・日時classを優先し、それ以外のASCII強制欄をQWERTYにする。
- 数字・電話・日時は既存テンキーレイヤーへまとめ、用途別の専用配列は増やさない。
- URL・メール・パスワード・ASCII強制は、英字と記号へ移りやすい既存QWERTYレイヤーへまとめる。
- 自動選択は保存済みの前回レイヤーを書き換えない。利用者が手動で切り替えた場合は既存どおり明示選択として保存する。

### Out-of-scope

- `imeOptions` によるEnterの「次へ」「検索」「送信」「完了」表示・動作の変更。
- 電話番号・日時専用のキー配列、初期大文字、`hintLocales`、学習可否の変更。

### Implementation Notes

入力テスト画面で数字欄とメール欄を実際に選び、初期レイヤーを観察できるようにする。

### Affected Layers

Android app · IME service · unit tests · instrumentation/device tests · docs

---
Work notes: `note.md`
