---
priority: 2
base_branch: features/260912-071844-add-ime-hide-bar
description: "Show emoji recents and integrate the AndroidX categorized picker"
created_at: "2026-09-12T08:00:17Z"
started_at: 2026-09-12T08:08:14Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-080017-show-emoji-recents-and-expand-catalog

### Why
最近使った絵文字が一覧本体へ混ざるため、再利用する場所と全体から探す場所が分かれておらず、固定32件しか選べない。
絵文字レイヤーでは候補欄をカテゴリ選択へ置き換え、Android公式ライブラリのUIでrecent・カテゴリ・variationを含む一覧から選べるようにする。
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
このticketが終わると、利用者が絵文字レイヤー最上部のカテゴリ行からRecentや目的のカテゴリを選び、絵文字とvariationを入力できる。

- [x] AC 1: 絵文字レイヤーを開くと、通常の変換候補faceやメッセージの代わりに、候補欄と同じ最上部50dp領域へ横方向のカテゴリicon行が表示される。
- [x] AC 2: カテゴリicon行の先頭にRecentがあり、選ぶと端末内の最近使った絵文字が新しい順・重複なしで下の一覧へ最大24件表示される。
- [x] AC 3: 絵文字レイヤーは全体高を変えず、カテゴリ行の下に縦スクロールできる絵文字一覧3行、最下段に既存のレイヤー切替と削除を表示する。
- [x] AC 4: Recent以外のカテゴリをタップすると対応する一覧へ移動し、variationを持つ絵文字を長押しすると肌色・性別などの選択肢を選べる。
- [x] AC 5: 一覧またはvariationから絵文字をタップすると現在の入力欄へ1回だけ確定し、Recentの先頭へ反映される。Recentが空のときは履歴絵文字を表示しない。
- [x] AC 6: private入力欄ではRecentカテゴリへ履歴を表示せず、通常欄へ戻ると保存済み履歴を復元する。
- [x] AC 7: 候補faceを50dp候補欄内で34dpから38dpへ広げてtop10dp/bottom2dpとし、最上段keyとの10dp間隔、先頭左余白phone 6dp/wide 13dp、候補間5dp、候補内容が変わったときの横scroll先頭resetを維持する。
- [x] AC 8: Android nativeと公開操作mockで最上部50dpのカテゴリicon行、Recent内容、3行一覧、縦scroll、最下段操作が対応し、Light/Darkと412dp/840dp幅で欠けや横overflowがない。

### Architectural Invariants check
絵文字pickerとrecentは端末内だけで動作し、公開mockもブラウザ内だけへ保存する。入力内容や履歴を外部へ送らず、private欄では履歴を表示しないためAI-1〜AI-4と矛盾しない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- nativeは安定版`androidx.emoji2:emoji2-emojipicker:1.6.0`の`EmojiPickerView`を使う。Emoji 16.0、横スクロール可能なカテゴリheader、縦一覧、長押しvariation、recent providerを自前catalogの代わりに利用する。
- pickerの列数は既存レイヤーと同じ8列、一覧表示は3行とする。pickerのカテゴリheaderを通常候補欄と同じ最上部50dpへ置き、一覧をkeyboard上3行へ置く。
- pickerのRecentカテゴリは既存の端末内recent保存を移行・拡張した最大24件のproviderを使う。
- 候補欄全高50dpと最上段keyまでの10dpは維持し、faceを上へ4dp広げて空白感だけを詰める。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 絵文字検索、GIF、スタンプ、AndroidX picker自体の収録内容・カテゴリ順のfork。
- Android個人辞書やMozc候補への絵文字学習統合。
- 4行keyboard高、最下段レイヤー操作の変更。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「絵文字レイヤーには変換候補エリアのところに最近使ったやつを出して」「それに絵文字ってこれで全部？」「変換候補の上下の空白、ちょっとだけ広すぎない？」「Androidの絵文字選択ツールも使うのはどう？カテゴリ分けとかのUIとかも。一番上の行がカテゴリで」「カテゴリにRecentがあり、その中に最近使ったのが出る」「候補欄は必要ない。そこにカテゴリアイコンが並ぶイメージ、Recentもアイコン」。
<!-- ユーザの明示指示、またはユーザが会話で言及した事項のみ書く (関数名 / module 名レベルまで)。
     設計判断は「Design Decisions」に書く。
     Coding Engineer は Implementation Notes が空でも実装できる責務を持つ。
     PM が自主的に実装詳細を書いてはならない (下流の自由度を奪う)。 -->

### Dependencies
`260912-071844-add-ime-hide-bar`の3段IME rootをbaseとする。
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
