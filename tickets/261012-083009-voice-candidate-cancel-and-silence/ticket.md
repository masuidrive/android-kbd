---
priority: 2
base_branch: features/260925-094954-unassigned-flick-tap-and-number-key-hints
description: "音声候補のキャンセル操作と無音後の認識再開を整える"
created_at: "2026-10-12T08:30:09Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 261012-083009-voice-candidate-cancel-and-silence

### Why
Product Brief の音声レイヤーで連続入力する際、誤認識候補を捨てる選択肢が候補一覧にない。無音による再起動中も「認識中」と表示され、話し始めを取り逃す可能性がある。

### What / Acceptance Criteria
この ticket が終わると、音声レイヤーの利用者が候補を捨てて次の発話へ移り、認識の準備状態を見分けられるようになる。
- [ ] AC 1: 音声候補の一覧に「キャンセル」を選べる行がある。
- [ ] AC 2: [NEEDS CLARIFICATION: 候補の「キャンセル」は候補だけ捨てて認識を続けるか、音声レイヤーを終了するか？]
- [ ] AC 3: 無音後の再起動中と、実際に発話を受け付けられる状態を利用者が見分けられる。

### Architectural Invariants check
AI-1 と AI-2 を維持し、音声認識は既存の端末内 API のみを使う。AI-3 と AI-4 に影響しない。

### Design Decisions
- Android の `onReadyForSpeech` を発話開始可能な状態の判定に使う。現在の「認識中」は再起動前から表示されるため。

### Out-of-scope
- 外部音声認識サービスやネットワーク送信の追加。

---
Work notes: `note.md`
