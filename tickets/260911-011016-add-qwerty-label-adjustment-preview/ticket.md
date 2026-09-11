---
priority: 2
base_branch: features/260910-233205-add-on-device-voice-input
description: "実キーボードのプレビューを見ながらQWERTYラベルのサイズと位置を調整できるようにする"
created_at: "2026-09-11T01:10:16Z"
started_at: 2026-09-11T01:28:59Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-011016-add-qwerty-label-adjustment-preview

### Why
QWERTYの主ラベルと補助ラベルは端末や文字倍率によって見え方が変わり、固定値だけでは利用者の望む読みやすさへ十分合わせられない。Product BriefのネイティブCustom Viewを保ちながら、本人が実際の描画を見て文字配置を微調整できるようにする。

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
この ticket が終わると、Gesture IMEの利用者が、実キーボードのプレビューを見ながらQWERTYラベルの見た目を調整し、通常入力へ反映できるようになる。
- [ ] AC 1: 設定画面に実際のKeyboardViewを使うQWERTYプレビューが表示され、狭い画面と広い画面の見え方を確認できる。
- [ ] AC 2: QWERTYの主ラベルと補助ラベルについて、文字サイズとX/Y位置を調整するとプレビューへ即時反映される。
- [ ] AC 3: 保存した調整値が次回起動後も実IMEのQWERTY描画へ反映され、初期値へ戻す操作でv0.3.0時点の表示へ戻る。
- [ ] AC 4: 範囲外または壊れた保存値は安全な範囲へ制限され、狭幅・広幅・文字倍率変更後もラベルがキーから大きくはみ出さない。
- [ ] AC 5: 調整後もキーの入力内容、ジェスチャー判定領域、5レイヤーの配列と操作は変わらない。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->
AI-1〜AI-4と矛盾しない。入力処理と描画設定を分離し、オフライン入力、固定レイヤー、ネイティブCustom Viewの境界を維持する。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 調整対象は、英字主ラベル、上部補助ラベル、Space/Enter主ラベル、Space矢印/Enter paste、小型・複合キー（C/A・BS・あん）の5グループとする。選択中グループのサイズ倍率とX/Y位置だけを表示し、特殊キー間の既存サイズ比を保つ。
- 設定のプレビューには本番と同じKeyboardViewを使い、調整値は端末内へ永続化する。
- プレビューは入力、クリップボード、音声、レイヤー変更を発火しない。狭幅と広幅を切り替え、広幅は実寸の横スクロールで確認する。
- 初期値はv0.3.0公開時点のQWERTY描画値とする。
- 調整値は5群（通常英字の主ラベル・補助ラベル、Space/Enterの主ラベル・補助ラベル、小型複合ラベル）ごとに文字倍率とX/Y位置を持つ。各群は安全な範囲へ制限する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- ラベル文言、入力文字、キーマップ、hitbox、フリック閾値の編集。
- 日本語、数字、記号、カーソル各レイヤーのラベル調整。

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
