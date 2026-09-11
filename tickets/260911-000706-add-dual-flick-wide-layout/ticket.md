---
priority: 2
base_branch: features/260910-233809-fix-keyboard-interaction-details
description: "広幅画面で日本語12キーを左右に複製するDual Flick設定を追加する"
created_at: "2026-09-11T00:07:06Z"
started_at: null
closed_at: null
canceled_at: null
---

## 260911-000706-add-dual-flick-wide-layout

### Why
Galaxy Z Fold7の展開内画面を両手で使うとき、広い横幅を日本語フリック入力へ活かし、左右どちらの手からも同じ12キー配列へ届くようにする。

### What / Acceptance Criteria
この ticket が終わると、Fold7の展開内画面を使う利用者が、設定でDual Flickを有効にして左右2組の日本語12キーから両手入力を試せるようになる。
- [ ] AC 1: 設定にDual Flickのtoggleがあり、初期状態はOFFで、利用者がON/OFFを切り替えられる。
- [ ] AC 2: Dual FlickがONで利用可能幅が広い場合、日本語かな入力の3×4キー部分が左右に2セット並び、Fold7の展開内画面ではこの表示になる。
- [ ] AC 3: 左右どちらの3×4キーから入力しても同じ入力欄・同じ日本語変換状態へ入り、交互打鍵と同時touchで文字の欠落、順序の入替、gestureの競合が起きない。
- [ ] AC 4: Dual FlickがOFFの場合、または利用可能幅が閾値未満の場合、既存の単一3×4配列が表示される。
- [ ] AC 5: 折畳、回転、マルチウィンドウで利用可能幅が変わると対応する単一・左右2セット表示へ切り替わり、入力中のcompositionが失われない。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。端末内処理、同じ変換インターフェース、既存ジェスチャーの意味を維持する。

### Design Decisions
- ユーザーの「テンキーレイアウト3×4」は日本語かな入力の12キー部分を指すと解釈する。数字モードまで複製するとは扱わない。
- Dual Flickは両手打ちを試すopt-in設定としてdefault OFFを仮定する。この初期値はAC承認時に確認する。
- 正確な幅thresholdは未指定。Fold7実表示幅と左右の押しやすさをprobeし、端末名ではなく実際の利用可能幅で切り替える値を決める。
- candidate stripと周辺の編集・レイヤー切替キーを複製対象へ広げない。配置は着手時に既存5レイヤーとの整合を見て設計する。

### Out-of-scope
- 数字モードの3×4複製、狭幅での強制Dual Flick、ユーザーが指定していない候補・編集キーの複製。
- 音声入力と、Dual Flickに因果のない既存IME機能の変更。

---
Work notes: `note.md`
