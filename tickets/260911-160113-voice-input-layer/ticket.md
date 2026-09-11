---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Add a dedicated voice-input layer with partial results and selectable hypotheses"
created_at: "2026-09-11T16:01:14Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-160113-voice-input-layer

### Why
現在の押下保持型の音声入力は、長文を話して候補を確認してから確定する操作が分かりにくい。
左フリックで専用面へ入り、端末内認識の途中結果と複数候補を確認して送信できるようにする。

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
このticketが終わると、キーボード利用者が左フリックから音声入力を開始し、認識内容を確認して候補選択または取消できる。

- [ ] AC 1: レイヤーキーを左へフリックすると音声入力面へ切り替わり、その時点で端末内音声認識を開始する。
- [ ] AC 2: 認識中は途中結果を音声入力面へ表示し、更新される内容を入力欄へはまだ確定しない。
- [ ] AC 3: 認識エンジンが返した1件以上の最終候補を、上部候補欄ではなく音声入力面の中へ順序どおり表示する。
- [ ] AC 4: 音声入力面の候補をタップすると、その候補を入力欄へ確定して直前の文字レイヤーへ戻る。独立した送信ボタンは表示しない。
- [ ] AC 5: 「キャンセル」で認識を停止し、認識文字を入力せず直前の文字レイヤーへ戻る。
- [ ] AC 6: マイク権限拒否、端末内認識非対応、無音、認識エラーをキーボード内で表示し、キャンセルまたは再試行できる。
- [ ] AC 7: 音声データと認識結果を保存せず、ネットワーク認識へのfallbackを行わない。

### Architectural Invariants check
端末内完結とネットワーク権限なしを定めるAI-1〜AI-4を維持する。音声は端末内`SpeechRecognizer`だけを使用する。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 左フリック成立時に専用面へ遷移し、認識を開始する。押下保持と1秒待機は廃止する。
- `SpeechRecognizer.RESULTS_RECOGNITION`は最有力候補を先頭に複数文字列を返せるが、実装依存で1件の場合もあるため可変件数で表示する。
- `RecognizerIntent.EXTRA_PARTIAL_RESULTS`を要求するが、認識サービスが途中結果を返さない場合も最終結果だけで操作を完了できるようにする。
- キーボード面と同じキー形状・角丸・影・Light/Dark tokenを使う。キャンセルは左上、最終候補は発話全体の候補を1行1件で縦に並べ、通常の変換候補欄は音声候補に使わない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- ネットワーク音声認識へのfallback。
- 音声や認識履歴の保存。
- API 34の単語区間ごとの代替候補を編集するUI。まず発話全体の候補を扱う。

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
