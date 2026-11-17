---
priority: 2
base_branch: features/261115-124242-separate-snippets-personal-info-buttons
description: "小キーとEnterのフリック候補を見える形に揃え、Enter左フリックでTabを送る"
created_at: "2026-11-17T00:14:26Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 261117-001426-unify-accent-enter-flick-popup

### Why
Product Brief の「同じジェスチャー体系で素早く操作したい」に沿い、小キーとEnterのフリック割当を指の移動中にも確認できるようにする。

### What / Acceptance Criteria
この ticket が終わると、日本語入力を使う人が、小キーとEnterのフリック先を表示で確認し、Enterの左フリックでTabを送れるようになる。
- [ ] AC 1: 日本語レイヤーの小キーを触ると、中央の「小」、左・上の濁点、右の半濁点が方向に対応する候補表示に出て、選んだ方向の動作が入力欄に反映される。
- [ ] AC 2: 日本語レイヤーのEnterを触ると、中央のEnter、左のTab、上のpaste、下のC-jが小キーと同じ方向候補表示に出る。左フリックで入力先にTabを1回送れる。
- [ ] AC 3: 変換中のEnterは従来の中央確定/無変換と左・上カタカナ変換を保ち、それらの候補が方向に対応して見える。未割当方向へ動かして離すと中央タップと同じ動作になる。
- [ ] AC 4: 製品ページの操作モックでも小キーとEnterの同じ方向候補、Enter左Tabを試せる。

### Architectural Invariants check
キー表示と編集イベントの変更は端末内で完結し、AI-1〜AI-4と矛盾しない。

### Design Decisions
- 小キーとEnterは、個別の文字拡大表示ではなく、割当方向を見渡せる共通の十字候補表示を使う。
- 通常Enterの上paste・下C-j・中央Enter、変換中Enterの既存動作は維持する。
- KeySpecの方向割当をイベント・候補表示・アクセシビリティ名の共通の基準にする。特殊な見た目だけの条件分岐を減らす。

### Out-of-scope
- Tabの受信先アプリ側の処理、他レイヤーのキー配置、変換エンジンは変えない。

### Dependencies
先行する定型文・個人情報ボタンの作業ブランチをbaseとする。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
<!-- ユーザの明示指示、またはユーザが会話で言及した事項のみ書く (関数名 / module 名レベルまで)。
     設計判断は「Design Decisions」に書く。
     Coding Engineer は Implementation Notes が空でも実装できる責務を持つ。
     PM が自主的に実装詳細を書いてはならない (下流の自由度を奪う)。 -->

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
