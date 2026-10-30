---
priority: 2
base_branch: features/261027-065123-make-ca-tap-tab
description: "単語一覧のSpaceとレイヤー復帰を直す"
created_at: "2026-10-30T13:40:21Z"
started_at: 2026-10-30T13:40:51Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 単語一覧のSpaceとレイヤー復帰を直す

### Why
登録した個人情報・定型文を選ぶ単語一覧では、Spaceの上下フリックや左下キーが通常の入力レイヤーと違う結果になる。Product Briefのフリックでタップを減らす入力を、単語選択中も迷わず使えるようにする。

### What / Acceptance Criteria
このticketが終わると、単語一覧を使う人が、Spaceでカーソルを動かし、左下キーから元のレイヤーまたは指定レイヤーへ移れる。

- [ ] AC 1: 単語一覧を表示してSpaceを上下へフリックすると、可能な入力欄ではカーソルが上下へ動き、一覧が開いたままになり、レイヤーは変わらない。
- [ ] AC 2: 単語一覧の左下キーは、開く前のレイヤーを表示し、タップすると一覧を閉じてそのレイヤーへ戻る。
- [ ] AC 3: 単語一覧の左下キーを左・上・右・下へフリックすると、それぞれ音声・日本語・QWERTY・テンキーへ切り替わる。タップでは文字を入力しない。
- [ ] AC 4: 製品ページの操作モックでも、単語一覧のSpaceと左下キーが同じ操作を示す。

### Architectural Invariants check
入力内容と登録値は端末内に留まり、既存のオフライン変換・private入力欄の制限と矛盾しない。

### Design Decisions
- 「単語一覧」は登録した個人情報・定型文を選ぶ固定高の領域を指す。新しい保存形式や第7の入力レイヤーは増やさない。
- 左下キーの中央タップ先は、一覧を開いた瞬間のレイヤーとする。戻る矢印と同じ閉じる操作にする。
- Spaceの上下は既存のカーソル移動の契約に従う。カーソルを動かせない入力先での結果は既存のprivate/非private fallbackに従う。

### Out-of-scope
- 個人情報・定型文の登録画面とデータ形式の変更。
- 通常6レイヤーのSpace操作とレイヤー切替配置の変更。

### Dependencies
なし。

---
Work notes: `note.md`
