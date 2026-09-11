---
priority: 2
base_branch: features/260911-022705-rebuild-keyboard-visual-fidelity
description: "公開デモをDual Flick対応の可変幅キーボード表示へ更新する"
created_at: "2026-09-11T04:16:00Z"
started_at: null  # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-041600-update-dual-flick-demo

### Why
実機を導入する前の利用者が、公開デモで通常幅と広幅Dual Flickのレイアウトを確認できるようにする。製品紹介の操作イメージと実Android UIの差を減らす。

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
この ticket が終わると、公開デモの利用者が、入力欄とキーボードを見やすい幅で操作し、広幅ではDual Flickを試せるようになる。
- [ ] AC 1: `demo.html`で、スマートフォンの縦長外枠を使わず、入力欄とキーボードが画面中央に表示される。
- [ ] AC 2: デモの表示幅が利用可能幅に応じて変わり、狭幅では単一かな配列、広幅では左右のDual Flickかな配列を操作でき、左右それぞれのpointer releaseによる入力が同じ入力欄へ保持される。
- [ ] AC 3: QWERTYを含む既存の入力・レイヤー切替デモが引き続き操作できる。
- [ ] AC 4: 公開サイトの製品紹介・マニュアル・ダウンロード導線を壊さず、モバイル幅で横overflowが発生しない。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。デモの固定テスト入力はブラウザ内だけで処理し、外部へ送信しない。Android IME本体とは独立した説明用UIに留める。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- Dual表示は実装済みnativeの境界と中央12キー2組・周辺キー1組という構造に合わせる。
- 左右の12キーはそれぞれpointerを追跡し、交互入力と同時押下後の各releaseを同じ模擬入力欄へ反映する。
- 固定のスマートフォンframeではなく、入力欄とキーボードを中心とする可変幅コンテナを使う。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- Android APK本体、IME入力処理、Mozc、音声認識の変更。
- v0.6英数字候補bufferの実装。

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

- 物理的な実装blockerはない。v0.5を先に公開するのはdelivery順序であり、新siteの外部送信承認待ち中もローカルdemo実装は進める。

---
Work notes: `note.md`
