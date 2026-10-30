---
priority: 2
base_branch: features/261027-065123-make-ca-tap-tab
description: "絵文字レイヤーの再表示を安定して検証する"
created_at: "2026-10-30T09:57:08Z"
started_at: 2026-10-30T09:57:54Z
closed_at: null
canceled_at: null
---

## 絵文字レイヤーの再表示を安定して検証する

### Why
絵文字レイヤーを開き直すとRecentから使えるという既存の動作を、全体テストの失敗で確かめられない。Product Briefの6レイヤーとRecentの使い勝手を継続して検証できるようにする。

### What / Acceptance Criteria
このticketが終わると、絵文字を使う人が、別レイヤーから絵文字レイヤーを開き直してもRecentから選べる。

- [ ] AC 1: 絵文字レイヤーで別カテゴリへ移動した後、いったん別レイヤーへ切り替えて戻るとRecentカテゴリが表示される。
- [ ] AC 2: 絵文字レイヤーを表示したまま入力画面が作り直されても、新しい絵文字一覧のRecentカテゴリが表示される。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。絵文字の表示と履歴は端末内だけで扱う。

### Design Decisions
- まず既存テストの失敗がRobolectric固有か実装の欠陥かを切り分け、観測結果に応じた最小修正にする。

### Out-of-scope
- 絵文字の種類、カテゴリ構成、見た目の変更。
- 他の既知のテスト失敗をまとめて変更すること。

---
Work notes: `note.md`
