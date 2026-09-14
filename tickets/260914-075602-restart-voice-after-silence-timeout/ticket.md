---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "無音で終了した音声認識を自動再開して次の発話を待てるようにする"
created_at: "2026-09-14T07:56:02Z"
started_at: 2026-09-14T07:57:46Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260914-075602-restart-voice-after-silence-timeout

### Why
音声入力レイヤーでしばらく黙ると Android のエラー7（NO_MATCH）で認識が終了し、その後の発話を入力できない。
Product Brief の6レイヤーを同じジェスチャー体系で素早く使える状態を保ち、キャンセルするまで連続して音声入力できるようにする。

### What / Acceptance Criteria
この ticket が終わると、音声入力レイヤーの利用者が、無言の時間を挟んでもキャンセルするまで次の発話を続けて入力できるようになる。

- [x] AC 1: 音声入力レイヤーで有効な途中結果がないまま無音系エラー6（SPEECH_TIMEOUT）または7（NO_MATCH）が発生すると、エラー表示へ切り替わらず「認識中」のまま次の発話を待つ。
- [x] AC 2: 無音系エラーの前に有効な途中結果がある場合は、従来どおりその結果を選択可能な候補として残し、候補を選ぶと入力して次の認識へ進む。
- [x] AC 3: キャンセル、レイヤー切替、入力欄切替、IME終了後は無音系エラーから再開せず、終了済み認識の遅延結果も入力しない。
- [x] AC 4: 権限不足、端末内日本語モデル不足、認識器処理中など無音以外のエラーは従来どおり利用者へ表示し、自動再開しない。

### Architectural Invariants check
端末内 SpeechRecognizer のセッション制御だけを変更し、AI-1 のネットワーク非使用、AI-2 の入力非送信、AI-3 の小さな変換境界と矛盾しない。

### Design Decisions
- 自動再開は通常の無音終了を表すエラー6・7だけに限定する。権限やモデル不足など、待っても直らない異常の再試行ループを避けるため。
- 無音終了した認識器は破棄し、短い待機後に同じ入力欄tokenで新しい端末内認識器を開始する。キャンセル可能な待機にして認識器の連続生成を抑えるため。
- 再開待機中も音声レイヤーは「認識中」を維持する。無言を利用者操作が必要な失敗として見せないため。

### Out-of-scope
- ネットワーク音声認識への切替。
- 無音以外のエラーの自動再試行。
- 音声候補の形状、並び、差分強調、確定操作の変更。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes

- ユーザ報告は「無言が続くと音声が認識できませんエラー7が出るけど、そうしたら続きが入力できない」。

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
