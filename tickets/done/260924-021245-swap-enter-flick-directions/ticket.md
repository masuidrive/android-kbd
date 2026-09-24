---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "EnterのPasteとCtrl+Jのフリック方向を入れ替える"
created_at: "2026-09-24T02:12:45Z"
started_at: 2026-09-24T02:13:46Z # Do not modify manually
closed_at: 2026-09-24T13:58:53Z # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260924-021245-swap-enter-flick-directions

### Why
Product Briefの「同じジェスチャー体系で素早く操作したい」に沿い、Enterから使うPasteとCtrl+Jを利用者が意図する方向へ揃える。

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
このticketが終わると、利用者が変換中でないEnterを上フリックしてPaste、下フリックしてCtrl+Jを実行できる。

- [x] AC 1: 日本語、テンキー、QWERTY、記号、音声入力の変換中でないEnterで、上フリックはPaste、下フリックはCtrl+Jになり、待機中のキーは上にC-j、中央にEnter、下にpasteを離して表示し、選択中のラベルも実行内容と一致する。
- [x] AC 2: Enterの中央タップは従来どおりEnterで、未割当の左右フリック、中心復帰、cancelはPasteもCtrl+Jも実行しない。
- [x] AC 3: 日本語変換中のEnterは、中央の無変換・確定と左または上フリックのカタカナ確定を維持する。
- [x] AC 4: 操作モック、正本仕様、技術仕様、利用者向けマニュアルが上Paste・下C-jの操作と、待機中の上C-j・下paste表示へ揃う。
- [x] AC 5: 記号レイヤー3行目のTabが右端にあり、タップで従来どおりTabを送る。ほかの記号キーの割当は維持する。

### Architectural Invariants check
キー割当と表示だけを変更し、AI-1〜AI-4の端末内処理、ログ禁止、小さな変換境界、一回修飾状態と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 変更対象は変換中でないEnterの上下だけとし、上=Paste、下=Ctrl+Jへ入れ替える。
- ユーザ追加指示によりEnterの待機中表示は上C-j・中央Enter・下paste、記号3行目のTabは右端とする。
- Pasteはprivate入力欄で無効のまま、Ctrl+Jは既存の同一gesture内key eventを維持する。
- 日本語変換中のEnter方向は既存契約を維持する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- Tabの記号3行目内の位置以外のキー割当、Paste/Ctrl+Jの実装方式、変換候補挙動、レイヤー構成の変更。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
<!-- ユーザの明示指示、またはユーザが会話で言及した事項のみ書く (関数名 / module 名レベルまで)。
     設計判断は「Design Decisions」に書く。
     Coding Engineer は Implementation Notes が空でも実装できる責務を持つ。
     PM が自主的に実装詳細を書いてはならない (下流の自由度を奪う)。 -->

- ユーザ指示: 「enterのフリックの上下逆にして。pasteとC-jの方向逆」。
- ユーザ追加指示: 「ルール的には上にC-j、下にpasteじゃない？今は中央寄りだけど、上下に離して」。操作の上下は維持し、待機中のラベル配置を更新する。
- ユーザ追加指示: 「記号レイヤーのtabは一番右にずらして」。

### Dependencies
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
