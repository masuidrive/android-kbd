---
priority: 2
base_branch: features/261027-015056-support-inline-autofill
description: "C/A キーの中央タップで Tab を入力する"
created_at: "2026-10-27T06:51:23Z"
started_at: null
closed_at: null
canceled_at: null
---

## 261027-065123-make-ca-tap-tab

### Why
QWERTY と記号レイヤーで Tab を見つけやすくし、タップ回数を減らす。Product Brief の同じジェスチャー体系で素早く操作する目的に沿う。

### What / Acceptance Criteria
この ticket が終わると、QWERTY または記号レイヤーを使う人が、C/A キーの中央タップで Tab を入力できるようになる。

- [ ] AC 1: QWERTY と記号レイヤーの C/A キーは中央に「T」を表示し、中央タップで入力先へ Tab キーを送る。
- [ ] AC 2: 同じキーの上フリックは次の1キーに Alt、下フリックは次の1キーに Ctrl を適用し、選択中の A/C 表示と解除操作を維持する。
- [ ] AC 3: ブラウザの操作デモでも中央「T」と Tab の動作を確認できる。

### Architectural Invariants check
AI-1〜AI-4 と矛盾しない。Tab は端末内の InputConnection へ送るキーイベントで、Ctrl/Alt の次の1キーだけの状態は維持する。

### Design Decisions
- 「T」はキーの中央ラベルとして使い、既存の上下 A/C ラベルは残す。
- Ctrl/Alt 選択後の中央タップは解除を優先する。修飾状態での意図しない Tab 送信を避ける。

### Out-of-scope
- ほかのキーの配置と Tab キーイベントの受け取り側アプリでの挙動は変更しない。

---
Work notes: `note.md`
