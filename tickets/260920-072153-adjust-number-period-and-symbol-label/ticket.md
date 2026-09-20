---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "テンキーの小数点へ左右フリックを追加し、記号レイヤー表示を実際の記号に合わせる"
created_at: "2026-09-20T07:21:53Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260920-072153-adjust-number-period-and-symbol-label

### Why
テンキーの`.`から`,`と`=`を直接入力できず、記号レイヤー切替キーの`#!`も利用できる記号を十分に表していない。
Product Briefの「フリックを使ってタップ回数を減らす」と、キー上で操作結果を判断できる一貫した表示を担う。

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
このticketが終わると、テンキー利用者が小数点キーの左右フリックで`,`と`=`を入力でき、利用者が`?}`表示から記号レイヤーへ移動できる。

- [ ] AC 1: テンキーの`.`は中央tapで`.`、左フリックで`,`、右フリックで`=`を入力し、上・下フリックには何も割り当てない。
- [ ] AC 2: 日本語、テンキー、QWERTY、絵文字railにある記号レイヤー切替キーを`?}`表示へ統一し、tapすると従来どおり記号レイヤーを開く。
- [ ] AC 3: native、操作mock、正本仕様、使い方ページが同じ割当と表示を示す。

### Architectural Invariants check
端末内処理と共通KeySpec/KeyAction境界を維持し、AI-1〜AI-4と矛盾しない。通信権限は追加しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- テンキー`.`は専用の3方向KeySpecとし、左・右だけを明示して未割当方向へ入力をfallbackさせない。
- `?}`は2文字複合ラベルとして、既存の主文字＋淡い補助文字の描画を使う。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 記号レイヤー内の文字配置、QWERTYの記号割当、他のテンキー、次版公開。

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
