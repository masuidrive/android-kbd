---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "未割当フリックを通常タップに戻し、テンキーの記号キーへ方向ラベルを表示する"
created_at: "2026-09-25T09:49:54Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 未割当フリックをタップにし、テンキーの方向ラベルを揃える

### Why
<!-- ユーザ価値・解きたい問題を 1〜3 行で書く。
     Product Brief の Problem / Solution のどの部分を担うか明記する。 -->
Product Briefの「同じジェスチャー体系で素早く操作したい」を満たすため、割当のない方向へ指がずれても入力を失わず、テンキーの記号キーでは利用できるフリックをキー上で把握できるようにする。

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
このticketが終わると、Android利用者が未割当方向へ指を動かしても通常タップを入力でき、テンキーの記号キーで割当済みの方向を見て使える。

- [ ] AC 1: フリック未割当方向へ動かしたキーは表示がフリック選択へ変わらず、移動に伴う追加振動もなく、離すと中央タップと同じ動作を一度だけ行う。割当済み方向、中心復帰、取消、長押しの動作は維持される。
- [ ] AC 2: テンキーの`-`キー内には左`+`・上`/`・右`*`・下`,`、`.`キー内には左`,`・右`=`が方向に対応する位置へ補助表示される。`.`の未割当上下には補助表示がない。選択した方向の表示変化はEnterキーと同じ操作体系で見える。
- [ ] AC 3: 製品ページの操作モックでも未割当方向のタップ確定とテンキー記号の方向ラベル・選択状態を同じように試せる。

### Architectural Invariants check
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->
UIの選択表示と既存入力動作だけを変更し、AI-1〜AI-4の端末内入力・非記録・Mozc境界・一回修飾状態と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 未割当方向は中央タップ相当とし、方向通過では追加振動・選択アニメーションを出さない。指を置いた瞬間の通常タップ振動は維持する。
- `-`は既存の左`+`・上`/`・右`*`・下`,`、`.`は既存の左`,`・右`=`を表示する。割当自体は変更しない。
- native表示、保存済みHTML/CSS正本、ブラウザモック、マニュアルを同じ判断へ揃える。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 新しいフリック割当、候補・変換・音声処理、キーの高さや配置、振動設定項目の追加。

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
