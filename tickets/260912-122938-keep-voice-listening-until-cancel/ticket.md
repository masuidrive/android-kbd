---
priority: 2
base_branch: features/260912-080017-show-emoji-recents-and-expand-catalog
description: "Keep voice recognition active until cancel"
created_at: "2026-09-12T12:29:38Z"
started_at: 2026-09-12T12:41:26Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260912-122938-keep-voice-listening-until-cancel

### Why
候補を1件選ぶたびに音声レイヤーが閉じるため、続けて話すには毎回レイヤーを開き直す必要がある。
音声レイヤーが待機中か分かりにくいため、キャンセルまで連続して音声入力できる状態を明示する。
実機確認では長い発話が最終候補へ遷移しない、音声候補と非対応表示が通常候補stripへ混ざる、絵文字Recentとcategory幅が不安定、OSと独自の閉じる行が重複する問題も見つかったため、同じ固定高IMEの入力体験として解消する。
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
このticketが終わると、利用者が音声レイヤーで長い発話の最終候補を確認し、候補確定と次の認識をキャンセルまで繰り返せる。また、絵文字Recentを通常gridで最大100件使え、独自の閉じる行やnativeにない入力欄装飾なしでIMEと操作mockを使える。

- [x] AC 1: 連続音声入力session中（録音・認識処理・次回認識開始中）は、最下段左のキャンセルキー右隣に「認識中」と分かる表示が出る。端末非対応・権限未許可・モデルなし・認識errorはキャンセル右隣のaction badgeへ表示せず、固定候補領域へtapしても動作しないplain textとして表示し、「認識中」と同時表示しない。
- [x] AC 2: 最終結果は通常の横候補欄を使わず、固定された音声layer内へ1行1候補の縦一覧として表示する。候補のいずれかをタップすると現在の入力欄へ1回だけ確定し、一覧を消して音声layerのまま「認識中」へ戻り、追加操作なしの次発話が新しい候補として表示される。
- [x] AC 3: 対応環境の通常フローでは候補選択と次の認識を2回以上繰り返せる。キャンセルをタップすると認識・未確定候補を破棄して進入前の文字レイヤーへ戻り、この連続sessionですでに確定した文字は残す。その後の旧callbackを表示・確定しない。
- [x] AC 4: private欄では音声入力を開始しない。通常欄からprivate欄・別editorへの切替、IME非表示、別layerへの切替時は停止し、切替前の途中結果・最終結果・候補tapは入力も次の認識も起こさない。認識errorは固定候補領域のtap不能なplain textで表示し、自動再試行しない。
- [x] AC 5: 10秒以上の発話を終えると認識中のまま停止せず、認識器の最終結果、または最終結果が返らない場合は最後の有効な途中結果を、AC 2と同じ選択可能な最終候補として1行1候補の縦一覧へ表示する。候補選択後の再開始を含む2周で、Android nativeと公開操作mockの全体4行高と最下段touch targetが変わらず、412dp・840dpでキャンセル、状態表示、既存の上下右flickが欠けない。
- [x] AC 6: 絵文字layerのRecent categoryには成功確定した最近の絵文字を新しい順・重複なしで最大100件まで通常の絵文字gridへ並べ、101件目では最古の1件を除く。文字入力の候補欄shortcutとは共有表示せず、category iconは全て同じ固定幅で、categoryや選択状態によって横幅が変わらない。
- [x] AC 7: Android native IMEと公開操作mockは独自の閉じる行を表示しない。Android native IMEはOSが提供するキーボード終了操作で閉じられる。候補・category領域と4行key領域の固定高は独自行の削除後も変動しない。
- [x] AC 8: 公開操作mockの入力欄はreadonlyで通常のキーボード入力を受け付けず、focus時にもnative入力面にないborderを表示しない。mock内のキー・候補・音声候補による入力結果とカーソルは同じ欄へ引き続き反映される。
- [x] AC 9: 公開操作mockのキーボード周囲は黒ではなく外側ページになじむ灰色で表示し、上のモード切替groupには下側との釣り合いが分かる余白を確保する。Mobile・Tablet・Dual FlickとLight・Darkを切り替えても周囲色と余白が崩れない。
- [x] AC 10: 製品トップの操作mockは外周の枠線なし・白い周囲で表示し、小さいeditor label位置にtapと上下左右flickで入力できることを明示する。readonly入力欄は「ここで実際に下のキーボードを操作できます」を初期表示し、最初のmock入力でその案内を消して入力結果へ置き換える。新規にPC幅で開くとTablet幅・Dual Flick ON・日本語かなlayer、スマホ幅で開くとMobile表示になる。独立した`demo.html`は公開せず、操作mockへのサイト内導線は製品トップの`#demo`へ着地する。
- [x] AC 11: 音声入力レイヤーの最下段は、左からキャンセル、認識中の状態領域、右寄せの「、。？！」・Space・Enterを同じ高さの5列で表示する。「、。？！」・Space・Enterとそれぞれの既存フリック操作は、録音中・認識処理中・候補表示中にも現在の入力欄で使え、音声session、認識中表示、未確定の音声候補を終了・再開始・消去しない。nativeと製品トップの操作mockで同じ配置と動作になり、412dp・840dpと小・標準・大の固定4行高を維持する。
- [x] AC 12: 音声入力レイヤーの「、。？！」キーは、従来のtap「、」・左「。」・上「？」・右「！」を維持したまま、未使用だった下フリックでも「、」を直接入力できる。これにより表示中の4記号は全てフリック操作でも入力でき、録音中・認識処理中・候補表示中の音声sessionと候補を維持する。nativeと製品トップの操作mockで同じ方向・popup表示になる。

### Architectural Invariants check
端末内認識だけを使い、録音音声・途中結果・確定候補を保存または外部送信しない。private入力欄では開始せず、editor sessionが変わった結果を破棄するためAI-1〜AI-4と矛盾しない。
<!-- product-brief.md の Architectural Invariants と矛盾しないことを 1 行宣言する。
     矛盾しない場合: 「Hub stateless / Process immutable と矛盾しない」等。
     新規 Invariant を要求する場合: 実装を止めて Product Brief 更新から始める。 -->

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 候補確定後は同じeditor session tokenで新しいrecognizer sessionを開始し、前sessionのcallbackはgenerationで破棄する。
- 認識状態の表示は最下段のキャンセルキー右側を使い、既存4行高とキャンセル・上下右フリック操作を変えない。
- 端末非対応・認識不能・権限未許可・モデルなしは固定候補領域の非action plain textで表示し、自動再試行しない。
- 音声候補は通常候補stripから分離し、上3行相当の固定領域を使う縦scroll一覧とする。端末非対応・errorは同領域のplain textとして扱い、認識中表示は最下段のキャンセル右隣に置く。
- Recentの保存上限は100件とし、AndroidX Emoji PickerのRecent categoryへだけ供給する。
- OSと重複する28dpの独自hide barは削除する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 無音や一時エラー後の無限自動再試行、認識言語の切替、クラウド音声認識。
- 音声候補の編集・結合、録音履歴、認識結果の学習。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
ユーザ指示: 「音声認識をしてる時にキャンセルボタンの横ぐらいで認識中ぐらいのことを出してほしい。候補選択してもすぐ次の音声認識に入ってほしい。キャンセルを押すまで」。
<!-- ユーザの明示指示、またはユーザが会話で言及した事項のみ書く (関数名 / module 名レベルまで)。
     設計判断は「Design Decisions」に書く。
     Coding Engineer は Implementation Notes が空でも実装できる責務を持つ。
     PM が自主的に実装詳細を書いてはならない (下流の自由度を奪う)。 -->

### Dependencies
`260912-080017-show-emoji-recents-and-expand-catalog`の固定高IMEをbaseとする。
<!-- この ticket に着手するために完了が必要な他の ticket。
     「参考情報」ではなく「ブロッカー」だけ書く。なければ省略。
     ブロッカー = これが未完了だと実装・テストが物理的にできない依存。
     例: 「DB migration の ticket が先に必要」「認証 API が存在しないと結合できない」
     参考情報 (設計の参考にした ticket 等) は書かない。
     coding agent は未完了の依存がある場合、着手せず報告する。 -->

---
Work notes: `note.md`
