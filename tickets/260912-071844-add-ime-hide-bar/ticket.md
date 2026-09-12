---
priority: 2
base_branch: features/260912-055128-make-emoji-grid-scrollable
description: "Add a slim IME hide bar and navigation to the input test screen"
created_at: "2026-09-12T07:18:44Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-071844-add-ime-hide-bar

### Why
IMEを閉じる操作がキーボード面から見つけにくく、入力テスト画面には設定画面へ戻る上部ナビゲーションがない。
Product Briefのスマホ・タブレットで同じ操作体系を使える状態に、IMEの終了と設定・テスト間の往復を加える。
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
このticketが終わると、利用者がキーボード下端の操作でIMEを閉じ、入力テスト画面の上部ナビゲーションから設定画面へ戻れる。

- [ ] AC 1: すべてのキーボードレイヤーで、4行キーの下に中央の下向きシェブロンを持つ28dp高の閉じる操作行が表示され、候補欄と4行キー自体の高さは変わらない。
- [ ] AC 2: 閉じる操作行をタップすると、入力中のアプリを離れずにIMEウィンドウが閉じる。
- [ ] AC 3: 「入力を試す」画面の上部にsafe areaを避けた固定ナビゲーションバーが表示され、戻るボタンで直前の設定画面へ戻れる。
- [ ] AC 4: 閉じる操作行と入力テスト画面のナビゲーションはLight/Dark、412dp/840dp幅で欠けず、ステータスバーやナビゲーションバーへ重ならない。
- [ ] AC 5: 公開操作mockでも4行キー下の細い閉じる操作行を表示し、閉じた後に入力欄を選ぶとキーボードを再表示できる。
- [ ] AC 6: Androidのアプリ一覧・IME選択・設定・入力テストと、現行の製品ページ・操作mock・マニュアルで、利用者向け製品名が`masuidrive-kbd`に統一される。

### Architectural Invariants check
UIとIME lifecycleだけの変更であり、入力内容を外部へ送らず、Mozc境界やmodifier状態を変えないためAI-1〜AI-4と矛盾しない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 閉じる操作行の表示高は28dpとし、中央の下向きシェブロン以外のラベルを置かない。
- 閉じる操作はAndroidのIME終了APIを使い、対象Activityを終了しない。
- 入力テスト画面の戻る操作は画面をfinishし、既存の設定画面インスタンスへ戻す。
- 内部package/Application ID、Kotlin class名、保存済みmock state key、過去releaseのAPKファイル名は互換性のため変更しない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- Androidシステムのgesture navigation bar自体の見た目や高さの変更。
- キーボード高さpreset、候補欄、4行キーの寸法変更。
- 設定画面の項目構成やLicense Activityのナビゲーション変更。
- package/Application ID、theme/class名、既存APK asset名のrename。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「キーボードの下に閉じるボタン付けて。これよりは高さは低く」「入力を試す画面にも戻れるように上のナビバーつけて」「GestureIMEって表記はmasuidrive-kbdに統一して」。添付画像ではキーボード下の中央に下向きシェブロンがある。
<!-- ユーザの明示指示、またはユーザが会話で言及した事項のみ書く (関数名 / module 名レベルまで)。
     設計判断は「Design Decisions」に書く。
     Coding Engineer は Implementation Notes が空でも実装できる責務を持つ。
     PM が自主的に実装詳細を書いてはならない (下流の自由度を奪う)。 -->

### Dependencies
絵文字scrollを含むv0.12.0公開済みbranch `features/260912-055128-make-emoji-grid-scrollable`をbaseとする。
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
