---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Prototype the dedicated voice-input layer in the browser mock"
created_at: "2026-09-11T16:09:43Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-160943-prototype-voice-input-layer-in-browser

### Why
左フリック中だけ認識する現在の操作より、専用面で途中結果と候補を確認して送信する案の方が長文入力に適する可能性がある。
native実装を変える前に、公開ブラウザモックで操作感と情報配置を触って判断できるようにする。

### What / Acceptance Criteria
<!-- 完了を判定できる条件。プロダクトの観察可能な振る舞いだけを書く。
     読み手はこの ticket を承認する人であり、実装する agent ではない。

     箇条書きを書き始める前に、この節の冒頭へ 1 文を書く:
       「この ticket が終わると、〈誰〉が、いままでできなかった〈何〉をできるようになる」
     各 AC はその 1 文の分割として書く。非退行の AC だけは例外で、〈誰〉のみ必須。
     読めているかの判定は pdh-dev skill の「AC に書いてよいもの / 書いてはいけないもの」に従う。

     例: 「新しく登録したユーザが、一覧の画面に出る」
     例: 「画面幅 375px 以下でメニューがハンバーガーに切り替わる」

     プロセス要件 (レビュー済み、テストパス等) はここには書かない。
     ワークフロー (SKILL.md) と作業ノート (note) が保証する。

     runtime で UX/Security invariant を強制する ticket では、AC に「runtime enforce の
     保証メカニズム」を 1 行明記する (例: editor 警告だけでなく 422 reject されること)。 -->
このticketが終わると、閲覧者がブラウザモックで専用音声入力面の開始から候補選択、取消まで試せる。

- [x] AC 1: 文字レイヤーのレイヤーキーを左へフリックすると音声入力面へ移り、その時点で模擬認識を開始する。
- [x] AC 2: 更新される途中結果を通常の変換候補バーと同じUIへ表示し、最終結果ができるまで入力欄へ文字を追加しない。
- [x] AC 3: 最終結果は通常の変換候補バーと同じUIへ複数表示する。
- [x] AC 4: 音声入力面の候補をタップすると、その候補を入力欄へ1回だけ追加して直前の文字レイヤーへ戻る。独立した送信ボタンは表示しない。
- [x] AC 5: 左下の「キャンセル」をタップすると文字を追加せず直前の文字レイヤーへ戻り、同じキーの上下右フリックで日本語・テンキー・QWERTYへ切り替えられる。
- [x] AC 6: トップ埋め込みと独立demoのMobile/Tablet、Light/Dark、Dual Flickを壊さず、音声面でも横overflowがない。

### Architectural Invariants check
ブラウザ上の操作プロトタイプであり、nativeの端末内完結とネットワーク権限なしを定めるAI-1〜AI-4を変更しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 実マイクは使わず、時間経過で途中結果と複数の最終候補を表示する。
- 音声面から戻る先は進入前の文字レイヤーとする。
- 音声面は通常レイヤーと同じ4行高を維持し、状態見出しを置かない。キャンセルは左下、途中結果と最終候補は通常の変換候補バーへ表示する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- AndroidのSpeechRecognizer実装変更。
- 実際のマイク権限や音声認識。
- Sitesへの公開。

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
