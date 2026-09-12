---
priority: 2
base_branch: features/260912-014943-fixed-keyboard-height
description: "Align candidate strip spacing with keyboard key gaps"
created_at: "2026-09-12T02:25:12Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-112512-align-candidate-strip-spacing

### Why
候補欄と最上段キーの間隔がキー同士の間隔より広く、先頭候補も左端へ寄っているため、候補とキーボードが別の寸法体系に見える。
候補欄の外周余白をキートップの間隔へ揃え、nativeと操作demoで一続きの入力UIにする。

### What / Acceptance Criteria
このticketが終わると、利用者が候補欄とキーを同じ間隔のリズムで見渡し、左端の候補も端へ詰まらず選択できる。

- [ ] AC 1: 候補キートップ下端からキーボード最上段キートップ上端までの間隔が、通常キー行同士の縦間隔と同じになる。
- [ ] AC 2: 先頭候補キートップの左側に、通常キートップ外周と同じ余白が表示される。
- [ ] AC 3: 2件目以降の候補間隔、横スクロール、候補更新時の先頭へのスクロールリセット、長押し学習削除を維持する。
- [ ] AC 4: 日本語変換、次単語予測、英字・スラッシュ・音声候補で同じ外周余白を使い、Light/Darkとスマホ・タブレット幅で横方向にはみ出さない。
- [ ] AC 5: 公開ページ内の操作demoと単独mockがnativeと同じ候補外周余白になる。

### Architectural Invariants check
候補欄とキーボードの描画寸法だけを変更し、入力内容、候補生成、端末内学習、外部通信の扱いを変えない。

### Design Decisions
- 候補欄と最上段キーの視覚的な間隔は、キーボード内の行間隔を単一の基準値として合わせる。
- 先頭候補の左余白は最左列キートップの左外周と合わせる。
- 候補キートップ自体の高さ、角丸、影、文字サイズは維持する。

### Out-of-scope
- 候補生成順、候補数、Mozc予測ロジックの変更。
- キーボード高さpresetやキー行高の変更。
- 音声候補の最大2行表示の変更。

### Implementation Notes
ユーザ指示: 「変換候補とキーボードの隙間は他のキーの隙間と同じにして。1つ目の候補の左にも隙間を開けて。」

### Dependencies
`260912-014943-fixed-keyboard-height`。固定後のキー行間隔を候補欄の基準値にする。

---
Work notes: `note.md`
