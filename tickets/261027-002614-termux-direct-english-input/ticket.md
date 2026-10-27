---
priority: 2
base_branch: features/261012-114749-voice-return-cancel-status
description: "指定したアプリで英字と日本語の未確定文字を候補欄に表示する"
created_at: "2026-10-27T00:26:14Z"
started_at: 2026-10-27T01:05:43Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 261027-002614-termux-direct-english-input

### Why
Product Brief が対象とするターミナルでは、英数字候補とかな漢字変換の未確定文字が端末画面に描画されない場合がある。入力者が候補と変換を残したまま現在打っている文字と読みを確認できるようにする。

### What / Acceptance Criteria
この ticket が終わると、未確定文字を描画しないアプリの利用者が、英語候補と日本語変換を使いながら入力中の文字をキーボードの候補欄で確認できるようになる。
- [ ] AC 1: 利用者は設定画面で候補欄に未確定文字を表示する対象アプリを選び、解除できる。Termux は初期状態で対象になっている。
- [ ] AC 2: 対象アプリの端末入力欄で英数字またはかなを入力すると、未確定文字が候補欄に表示され、追加・削除に追従する。表示された文字は候補と区別できる。
- [ ] AC 3: 同じ入力欄で英語候補または日本語変換候補を選べる。英語は候補選択ならその候補、Space・Enter なら入力原文が重複なく確定し、日本語は候補選択または Enter で選ばれた文字列が一度だけ確定する。
- [ ] AC 4: 対象外アプリ、および対象アプリ内の通常テキスト欄では、従来どおり入力欄内の未確定表示と候補操作を使える。機密入力欄には候補欄プレビューを出さない。

### Architectural Invariants check
Product Brief の AI-1〜AI-4 と矛盾しない。入力内容をログやネットワークへ送らず、変換は端末内で完結する。

### Design Decisions
- `EditorInfo.packageName` で対象アプリの設定を引き、端末型の入力欄だけにプレビューを出す。描画可否を `InputConnection` の戻り値から断定しない。
- 入力方法と確定方法は変更せず、英字とかなの未確定文字を候補列内の選択できないプレビューとして見せる。実候補のタップ位置と選択は維持する。
- 対象アプリの選択は端末内設定とし、`com.termux` を初期対象にする。設定には md-kbd で入力したことのあるアプリを表示して追加・解除できるようにし、入力文字列は保存しない。
- 通常欄での二重表示と機密欄での表示を避けるため、対象アプリでも `EditorInfo.inputType` の class が `TYPE_NULL` の欄だけに限定し、既存の private 判定も適用する。`NO_SUGGESTIONS` 単独では通常のテキスト欄を誤判定しうるため根拠にしない。

### Out-of-scope
- Termux 本体に未確定文字の表示機能を追加すること。
- 英語候補辞書、Mozc の変換結果、キー配置や候補欄の高さの変更。

### Affected Layers
IME service · custom view UI · unit tests · instrumentation/device tests · browser mock · docs

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
なし。既存の候補欄実装を含む最新の作業枝から開始する。統合順序は human review 前に確認する。

---
Work notes: `note.md`
