---
priority: 2
base_branch: features/260911-041600-update-dual-flick-demo
description: "英数字入力を未確定bufferとして保持し、候補タップ時だけ置換確定する任意の候補表示を検討する"
created_at: "2026-09-11T00:39:16Z"
started_at: 2026-09-11T04:39:27Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-003916-add-english-candidate-buffer

### Why
日本語以外の短い英数字入力でも、候補を見て選びたい利用者が、入力原文を勝手に置換されずに候補を選べるようにする。コードやターミナルの入力を妨げないよう、候補表示は利用者が切り替えられるものにする。

### What / Acceptance Criteria
この ticket が終わると、候補表示を有効にした利用者が、英数字の入力原文を保ったまま候補を確認し、望む候補だけを選んで確定できるようになる。
- [ ] AC 1: 利用者が設定で英数字候補表示を有効または無効にでき、無効時は既存の英数字入力と確定操作を利用できる。
- [ ] AC 2: 有効時、対象となる英数字の連続入力は未確定表示として入力欄に残り、候補が表示されても入力原文が自動的に候補へ置換・確定されない。
- [ ] AC 3: 利用者が候補をタップすると、その候補が未確定の入力原文を置換して確定する。
- [ ] AC 4: 未確定の英数字入力中にSpaceを押すと、入力原文を無変換で確定してから空白を入力する。
- [ ] AC 5: 未確定の英数字入力中にEnterを押すと、入力原文だけを無変換で確定し、空白や改行を追加しない。未確定入力がないEnterは、既存どおり対象入力欄の改行または送信操作となる。
- [ ] AC 6: 候補表示が有効でも、候補生成・候補表示・確定は端末内で完結し、入力原文や候補をネットワークへ送信しない。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。既存のInputConnection経由入力と端末内処理を維持し、候補表示が入力原文を自動確定しない。

### Design Decisions
- 英数字候補は、コード・ターミナル用途を考慮して設定toggleで明示的に有効化し、既定OFFとする。ターミナル向けカーソル設定とは独立させる。
- 対象bufferはASCII英数字`[A-Za-z0-9]`、最大64文字とする。辞書候補は英字だけのprefixへ最大5件を出し、数字を含むbufferは候補なしのまま原文を維持する。
- 端末内の固定prefix辞書を用い、入力履歴や個人学習は保存しない。大文字小文字は入力prefixの表記を候補へ引き継ぐ。
- 候補タップ以外は原文を自動置換しない。SpaceとEnterの確定規則は本ticketの利用者操作として固定する。
- ネットワークを使う候補APIや同期は追加しない。

### Out-of-scope
- 日本語Mozc変換の候補・確定規則の変更。
- ネットワーク候補、クラウド辞書、入力履歴・個人学習・同期。
- 音声入力ticketの実装、および調整・Dual Flick・APK公開の残作業への変更。
- v0.5 visual fidelityと、その公開後に行うDual Flick公開demo更新。

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
