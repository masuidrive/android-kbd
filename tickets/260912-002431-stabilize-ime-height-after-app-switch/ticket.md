---
priority: 2
base_branch: features/260912-001139-show-hero-demo-input-result
description: "Keep the IME at the correct height immediately after switching apps"
created_at: "2026-09-12T00:24:31Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-002431-stabilize-ime-height-after-app-switch

### Why
IMEを表示したままアプリを切り替えると、まれにキー群が下へずれて大きな空白が入り、キー位置と表示領域が一致しない。
入力先を切り替えた直後から、候補欄と4行キーを通常と同じ高さ・位置で操作できる必要がある。
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->

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
このticketが終わると、IME利用者がアプリ切り替え直後でも通常と同じ位置のキーを操作できる。

- [ ] AC 1: IMEを表示したまま別アプリの入力欄へ切り替えた最初の表示で、候補欄とキー群の間に余分な空白や重なりが生じない。
- [ ] AC 2: QWERTY、日本語、テンキー、記号、音声の各4行レイヤーで、切り替え直後も通常表示時と同じIME高を維持する。
- [ ] AC 3: 少なくとも10回連続で2アプリ間を切り替えても、キー群の縦位置が途中から変化しない。

### Architectural Invariants check
既存の固定候補欄高と4行キー高を維持し、入力先アプリの種類に依存しないIMEレイアウトとする。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- IME lifecycleごとに実測した利用可能幅・insetsから同じroot寸法を再適用し、前アプリの一時layout値を持ち越さない。
- ユーザ提供スクリーンショットの候補欄下からキー群までの大きな空白を再現・検出対象にする。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- キー自体の高さ、隙間、配列、ラベルの再設計。
- xterm.jsなど入力先側がカーソル操作を受けない問題。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「まだアプリ切り替え直後の高さの計算が変。」
参照画像: `/Users/masuidrive/.config/tether/uploads/tether-upload-cad3fe4f45b381ea.jpg`。
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
