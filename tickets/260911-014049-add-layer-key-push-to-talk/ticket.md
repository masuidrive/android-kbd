---
priority: 2
base_branch: features/260911-011016-add-qwerty-label-adjustment-preview
description: "左下レイヤーキーの長押し中だけ端末内音声を認識して直接入力する"
created_at: "2026-09-11T01:40:49Z"
started_at: 2026-09-11T01:41:28Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-014049-add-layer-key-push-to-talk

### Why
候補欄の音声ボタンを押し分ける代わりに、普段使う左下レイヤーキーを押している間だけ話し、指を離して入力できるようにする。

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
このticketが終わると、Gesture IME利用者が左下レイヤーキーを押している間だけ日本語音声を入力できる。
- [ ] AC 1: 左下レイヤーキーは1秒未満のtap/flickで既存レイヤー操作を維持し、単指で1秒以上holdしたときだけ端末内音声認識を開始する。
- [ ] AC 2: 認識開始時に2回振動し、指を離すと録音を止め、同じ入力欄の有効なsessionで届いた最終結果をpreviewなしで一度だけ確定する。
- [ ] AC 3: 結果がrelease前に届いた場合はreleaseまで入力せず保持し、releaseが先の場合は最終結果を待って確定する。onReady前のreleaseでは開始・振動・入力を行わない。
- [ ] AC 4: 発火前のflick/second pointer、ACTION_CANCEL、画面・mode・入力欄変更、IME非表示、通常キー入力はholdを破棄し、古い結果を別の入力欄へ入れない。
- [ ] AC 5: マイク未許可、private欄、端末内日本語モデル非対応では録音を始めず、network recognizerへfallbackしない。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->
AI-1〜AI-4と矛盾しない。v0.3の端末内recognizer限定、非保存、editor session境界を維持する。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 音声holdイベントは通常`KeyAction`と分離し、View発行request ID・voice generation・editor sessionで一度だけ完了させる。
- hold成立後のowner MOVEは無視して録音を継続し、非owner UPでは停止しない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 音声モデルのdownload、network fallback、継続的な自動再録音。

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
