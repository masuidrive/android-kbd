---
priority: 2
base_branch: features/260925-094954-unassigned-flick-tap-and-number-key-hints
description: "画像を含むクリップボードでも Paste キーを入力先へ渡す"
created_at: "2026-10-12T08:27:06Z"
started_at: 2026-10-12T08:34:08Z # Do not modify manually
closed_at: 2026-10-12T11:11:49Z # Do not modify manually
canceled_at: null # Do not modify manually
---

## 261012-082706-paste-clipboard-content

### Why
Product Brief は一般的な Android アプリで同じ操作体系の編集操作を使えることを目指す。現状の Paste キーはクリップボードがテキストでないと何もせず、画像を貼れる入力先でも貼り付けられない。

### What / Acceptance Criteria
この ticket が終わると、画像の貼り付けに対応したアプリの利用者が、masuidrive-kbd の Paste キーから画像入りクリップボードを貼れるようになる。
- [x] AC 1: 画像をコピーした後、画像の貼り付けに対応した入力欄で Paste キーを使うと、その入力欄が画像を受け取る。
- [x] AC 2: 通常の文字列をコピーした後、Paste キーでその文字列を貼り付けられる。
- [x] AC 3: パスワード欄と学習禁止欄では、Paste キーでクリップボード内容を入力先へ渡さない。
- [x] AC 4: 貼り付け先が画像を受け付けない場合に、画像 URI の文字列を入力しない。

### Architectural Invariants check
AI-1 と AI-2 を維持し、クリップボード内容を IME からネットワークへ送らない。AI-3 と AI-4 に影響しない。

### Design Decisions
- 画像 clip は先頭項目が content URI で、入力先が一致する画像 MIME を宣言するときだけ Android の `commitContent` と読み取り権限フラグで渡す。画像の受け取りは入力先に任せ、失敗しても文字列Pasteへ落とさない。
- 文字 clip では入力先の標準Paste操作を優先し、受け付けない入力先には従来の実テキスト貼り付けを残す。画像 URI をテキスト化しない。

### Out-of-scope
- 画像ギャラリー、クリップボード履歴、画像編集、新規権限の追加。
- 画像貼り付けをサポートしない入力先へ強制的に画像を挿入すること。

---
Work notes: `note.md`
