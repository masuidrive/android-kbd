---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "音声入力中のRMS波形を、帯域ごとに独立して動く疑似スペクトラム表示へ変える"
created_at: "2026-09-16T09:56:53Z"
started_at: 2026-09-16T09:58:20Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260916-095653-animate-rms-pseudo-spectrum

### Why
音声入力中の現行4本波形は、1つのRMS値を固定比率で描くだけなので全体が同じ形で上下し、周囲の音へ反応していることが直感的に分かりにくい。
Product Briefの音声レイヤーを、キーボードの高さや端末内完結を保ったまま、入力音量へ反応する見た目に改善する。

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
このticketが終わると、音声入力を使う人が、認識中のマイク入力への反応をスペクトラム風の複数バーで見分けられる。

- [ ] AC 1: 音声認識がRMS値を通知するたびに、「認識中」の横で6本のバーが全体の音量へ追従しながら、互いに異なる高さへ変化する。
- [ ] AC 2: 同じ表示段階のRMS値が続いた場合もバーの組み合わせが動き、無音相当ではすべて最小高に戻る。
- [ ] AC 3: 候補選択待ち、取消、レイヤー・入力欄・IME切替、認識エラーでは表示が消え、再認識では初期状態から再開する。
- [ ] AC 4: 音声レイヤーの4行高、最下段6等分の操作領域、TalkBackの「認識中」状態は変わらない。
- [ ] AC 5: 公開操作モックでも同じ6本の疑似スペクトラム表示と停止条件を確認できる。

### Architectural Invariants check
SpeechRecognizerが端末内で通知するRMS値だけを描画に使い、音声の保存・送信、ネットワーク権限、Mozc境界を変更しないためAI-1〜AI-4と矛盾しない。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 実周波数解析ではなく疑似スペクトラムとする。`SpeechRecognizer.onRmsChanged`は周波数帯情報を返さず、`AudioRecord`の同時取得は端末内認識のマイク利用と競合しうるため。
- 6本のバーはRMSを全体振幅とし、callback世代内の決定的な位相とバー別係数から高さを作る。ランダム値や時間だけで動かさず、入力callbackが止まれば表示も止める。
- 視覚表示だけを変え、「認識中」のアクセシビリティnodeとタップ不能の契約は維持する。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- `AudioRecord`、FFT、実周波数帯の解析、騒音レベルの数値判定。
- 音声認識の候補生成、再試行、確定、マイク権限の挙動変更。
- APK・Webサイトの公開。

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
ユーザは「疑似でやってみて」と指定した。nativeと既存browser mockの波形を同じ見え方へ揃える。
