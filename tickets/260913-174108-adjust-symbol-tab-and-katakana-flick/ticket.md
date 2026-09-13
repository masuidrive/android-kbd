---
priority: 2
base_branch: features/260912-122938-keep-voice-listening-until-cancel
description: "記号レイヤーへEscとTabを直接配置し、変換中Enterの上フリックでもカタカナ確定できるようにする"
created_at: "2026-09-13T17:41:08Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260913-174108-adjust-symbol-tab-and-katakana-flick

### Why
QWERTYと記号レイヤーを行き来する利用者が、印字記号を欠かさず入力しつつ、ターミナルやフォームで必要なEscとTabを少ない操作で送れるようにする。日本語変換では上下方向の操作を揃え、カタカナ確定を迷わず実行できるようにする。Product Briefの「入力操作の分散を減らし、同じジェスチャー体系で素早く操作する」に接続する。

### What / Acceptance Criteria
QWERTY・記号・日本語変換を使う人が、必要なキーと変換操作をレイヤー内で迷わず選べるようになる。
- [ ] AC 1: 記号レイヤーに`Esc`と表示されたキーがあり、タップするとEscapeキーイベントが送られる。
- [ ] AC 2: 記号レイヤーから直接入力の`:`がなくなり、同じ位置の`Tab`をタップするとTabキーイベントが送られる。
- [ ] AC 3: `:`はQWERTYの`m`下フリックで入力でき、QWERTYと記号レイヤーを合わせてSpaceを含む印字可能ASCII 95文字をすべて入力できる。
- [ ] AC 4: 日本語変換中のEnterは、タップで元のひらがな、左または上フリックで全角カタカナを確定する。
- [ ] AC 5: Androidネイティブ、製品ページの操作mock、マニュアルで表示と操作説明が一致する。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。追加するキーイベントと変換処理は端末内で完結し、既存の小さな変換インターフェースを維持する。

### Design Decisions
- Tabはタブ文字の直接挿入ではなく、対象アプリへAndroidのTabキーイベントを送る。
- `:`はQWERTYの`m`下フリックに残す。Symbolsの10列構成と文字キーに方向フリックを設けない方針を維持する。
- 変換中Enterの左・上フリックは同じカタカナ確定に揃える。

### Out-of-scope
- 既存レイヤーの行数・キー幅・高さの変更。
- QWERTYの既存フリック割り当ての再配置。
- TabやEscapeを受け取らない対象アプリ側の挙動変更。

### Implementation Notes
ユーザは記号レイヤーの表示を`ESC`ではなく`Esc`とし、直接入力の`:`を削除して`Tab`を加えるよう指定した。また、他に不足キーがないか確認し、変換中Enterの上フリックもカタカナ変換にするよう指定した。
