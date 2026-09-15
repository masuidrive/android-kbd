---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "音声入力中のマイク音量を小さなリアルタイム波形で表示する"
created_at: "2026-09-14T13:38:49Z"
started_at: 2026-09-14T13:42:15Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260914-133849-show-realtime-voice-waveform

### Why
音声認識結果が出るまでマイクの入力状態が見えず、利用者は周囲の雑音が強いのか、声を拾えているのか判断できない。Product Briefの端末内音声レイヤーを、認識中の入力レベルも確認できるUIにする。

### What / Acceptance Criteria
この ticket が終わると、音声入力を使う人が、認識結果を待ちながらマイクの入力レベルを目で確認できるようになる。
- [x] 音声レイヤーで認識中に、`認識中`表示の横へ小さな波形が表示され、マイク入力が大きいほど波形も大きくなる。
- [x] 無音待機から発話しても波形が追従し、無音による認識の自動再開後も新しい入力レベルを表示する。
- [x] 候補選択待ち、取消、レイヤー切替、認識エラーでは波形が停止・消去され、音声候補やキーボードの高さと操作は従来どおり使える。

### Architectural Invariants check
音声を端末外へ送らず、API 31以降の端末内認識だけを使い、private欄で音声レイヤーを無効にする既存invariantと矛盾しない。

### Design Decisions
- 波形は既存の非操作`認識中`slot内に収め、音声panelや4行キーボードの高さを増やさない。
- Android音声認識が通知する入力レベルだけを描画に使い、録音音声を保存しない。
- 公開mockでは同じ位置と状態遷移を再現し、入力レベルはデモ用の変動で表現する。

### Out-of-scope
- 雑音レベルの数値表示、録音、騒音判定・警告、感度設定は追加しない。
- 音声認識候補、認識言語、無音時の自動再開条件は変更しない。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザは「ちょっとした波形がリアルタイムで出るとnoizyな環境かどうかわかる」と指定した。

### Dependencies
なし。v0.15.13の連続音声認識を基準にする。

---
Work notes: `note.md`
