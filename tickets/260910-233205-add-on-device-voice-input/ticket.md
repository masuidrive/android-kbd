---
priority: 2
base_branch: default
description: "端末内の日本語音声認識結果を確認して入力欄へ挿入できるようにする"
created_at: "2026-09-10T23:32:05Z"
started_at: null
closed_at: null
canceled_at: null
---

## 260910-233205-add-on-device-voice-input

### Why
Product Brief の「入力内容を外部へ送らず、同じIMEから日常入力を行う」を音声入力へ広げる。利用者がキーボードを離れず、日本語の発話を確認して現在の入力欄へ渡せるようにする。

### What / Acceptance Criteria
この ticket が終わると、対応端末の利用者が、IME内で日本語を録音し、端末内の認識結果を確認して現在の入力欄へ挿入できるようになる。
- [ ] AC 1: Android 12以降の端末内音声認識が利用できる場合、IME内のマイクボタンをタップして録音を開始し、もう一度タップして停止できる。
- [ ] AC 2: 録音停止後、日本語の認識結果がpreviewに表示され、利用者が「確定」を選ぶと現在の入力欄へ挿入され、「取消」を選ぶと挿入せず破棄される。
- [ ] AC 3: 録音中または認識待ちに入力欄を切り替えると処理が取消され、切替前の遅延した認識結果は新しい入力欄にもpreviewにも現れない。
- [ ] AC 4: 端末内認識serviceまたは日本語modelを利用できない場合、その旨が表示され、通信を使う音声認識へ自動的に切り替わらない。
- [ ] AC 5: マイク権限がない場合は録音せずAndroidの権限要求を表示し、拒否後も通常の5レイヤーIMEを利用できる。
- [ ] AC 6: Android 9〜11では音声入力を非対応として案内し、既存の文字入力・変換・ジェスチャーを引き続き利用できる。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。音声と認識結果を端末外へ送信・永続記録せず、既存の5レイヤー操作と変換境界を維持する。

### Design Decisions
- Android 12 / API 31以上では `SpeechRecognizer.createOnDeviceSpeechRecognizer` と端末内認識のavailability確認を使い、認識言語は日本語、`RecognizerIntent.EXTRA_PREFER_OFFLINE` も指定する。
- `RECORD_AUDIO` はruntime permissionとして要求する。通信型recognizerへの自動fallbackは設けない。
- Fold7上の日本語service/model可用性は実機probeで確認する。非対応時は利用者へ状態を表示する。
- マイクボタンの配置は、既存5レイヤーのジェスチャーを壊さないことを条件に着手時のticket reviewで決定する。

### Out-of-scope
- 通信を使う音声認識、録音音声・認識結果の保存、認識履歴・学習・同期。
- この起票時点での実装、既存ticketのclose、音声入力を理由としたAndroid 9以上の基本IME対応範囲の縮小。

### Implementation Notes
- 公式参照: https://developer.android.com/reference/android/speech/SpeechRecognizer
- 公式参照: https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_PREFER_OFFLINE

---
Work notes: `note.md`
