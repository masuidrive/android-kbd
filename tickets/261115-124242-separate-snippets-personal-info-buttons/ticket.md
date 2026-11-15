---
priority: 2
base_branch: features/261108-024949-move-ca-tab-to-horizontal-flick
description: "候補欄から定型文と個人情報を別々に開き、more..をなくす"
created_at: "2026-11-15T12:42:42Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
---

## 261115-124242-separate-snippets-personal-info-buttons

### Why
Product Brief の「同じジェスチャー体系で素早く操作したい」に沿い、定型文と個人情報が混在する一覧を探す手間をなくす。

### What / Acceptance Criteria
この ticket が終わると、登録済みの定型文と個人情報を使う人が、候補欄の別々のボタンから目的の一覧を直接開いて選べるようになる。
- [ ] AC 1: 通常の入力欄で候補欄に定型文ボタンと個人情報ボタンが並び、定型文ボタンを押すと登録済みの定型文だけがキー領域に表示される。
- [ ] AC 2: 個人情報ボタンを押すと、登録済みの氏名・メールアドレス・住所が最初からキー領域に表示され、「more..」を押さずに選べる。
- [ ] AC 3: 一覧表示中に各ボタンから一覧を切り替え、戻る操作で直前の通常レイヤーへ戻れる。最下段から「more..」は消え、レイヤー切替・句読点・Space・削除・Enterは使い続けられる。
- [ ] AC 4: private入力欄では両ボタンと登録値が表示されず、値の選択はその値だけを入力先へ送り、一覧に残って次の値を選べる。
- [ ] AC 5: 製品ページの操作モックでも候補欄の二つのボタン、分離した一覧、戻る操作、最下段の配置を試せる。

### Architectural Invariants check
登録値は従来どおり端末内だけに保存・表示し、private欄で使わないためAI-1〜AI-4と矛盾しない。

### Design Decisions
- 候補欄の左端に定型文と個人情報の二つの入口を置き、登録値と保護されたinline Autofill候補はその右に維持する。
- 個人情報一覧は氏名・メール・住所を初回からすべて表示し、定型文は含めない。定型文一覧は定型文だけを表示する。
- 一覧表示中もキーボード全体の高さを変えない。

### Out-of-scope
- 登録項目・保存形式・入力欄の種別判定・Bitwardenのinline Autofill連携は変更しない。
- 音声・絵文字・通常文字レイヤーのキー配置は変更しない。

### Dependencies
なし。個人情報一覧と定型文登録は現在の作業treeにある。

---
Work notes: `note.md`
