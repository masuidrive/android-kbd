---
priority: 2
base_branch: features/260912-014204-enable-non-terminal-settings-by-default
description: "Show Mozc next-word candidates after Japanese text is committed"
created_at: "2026-09-12T02:05:55Z"
started_at: 2026-09-12T02:09:41Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-020555-add-mozc-next-word-prediction

### Why
日本語を確定するたびに次の語を最初からフリックするとタップ数が増える。
Mozcの端末内Next Word Predictionを既存候補欄へ接続し、文脈に合う次の語を候補タップだけで続けて入力できるようにする。
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
このticketが終わると、日本語を入力する利用者が、確定した文章に続くMozc予測候補をタップして次の語を入力できる。

- [x] AC 1: 通常の日本語入力欄で変換候補、ひらがな、またはカタカナを確定すると、Mozcが直前の文章から生成した次単語候補が既存の候補欄へ表示される。
- [x] AC 2: 次単語候補をタップすると直前の確定文字を置換せず候補だけを続けて確定し、その文脈に続く次の候補へ更新される。
- [x] AC 3: 次のかな入力を始める、カーソルや選択範囲を変える、入力欄を切り替える、または予測候補がない場合、古い次単語候補が残らない。
- [x] AC 4: パスワード欄と個人学習禁止欄では次単語予測を生成・表示せず、カーソル周辺の文字列をMozcへ渡さない。
- [x] AC 5: 次単語候補は日本語変換候補と同じ外観・横スクロール・内容変更時の先頭復帰を使い、候補欄とキー4行の高さを変えない。

### Architectural Invariants check
予測処理は端末内Mozcと既存の変換interfaceを介し、入力文字列をログ・ネットワーク・外部サービスへ送らない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 日本語確定直後にInputConnectionから有限長のカーソル前後文脈を取得し、Mozcの`REQUEST_NWP`へ渡す。
- 次単語候補は自動確定せず、候補タップ時だけ入力する。日本語用の空白は自動挿入しない。
- 候補タップ後も更新済み文脈でもう一度予測し、連続して次の語を選べる。
- 設定toggleは追加せず、通常の日本語入力で初期状態から有効にする。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 英数字入力後の次単語予測。
- Mozc辞書・学習アルゴリズム・候補順位の変更。
- カーソルレイヤーの絵文字レイヤーへの置換。
- キーボード固定高さpresetの実装。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「予測変換実装して。」
Mozc公式protocolには周辺文脈からzero-query候補を返す`REQUEST_NWP`と`Context.preceding_text` / `following_text`がある。
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
