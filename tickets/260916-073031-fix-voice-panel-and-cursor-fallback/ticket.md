---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "音声レイヤーの操作行を常時露出し、通常編集不能な入力先だけカーソル操作を矢印キーへフォールバックする"
created_at: "2026-09-16T07:30:31Z"
started_at: 2026-09-16T07:33:17Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260916-073031-fix-voice-panel-and-cursor-fallback

### Why
音声候補パネルが最下段の操作行まで覆うと、認識結果がない間は空のレイヤーから戻れない。
また、通常入力欄の直接カーソル移動を保ちつつ、選択位置APIを提供しないターミナル等でもSpaceフリックを使える必要がある。

### What / Acceptance Criteria
音声入力とカーソル操作を使う人が、空の音声レイヤーから戻れ、入力先に合った方法でカーソルを動かせるようになる。

- [ ] 音声レイヤーでは認識結果が空でも最下段のキャンセル・状態・句読点・Space・削除・Enterが表示され、キャンセルで元のレイヤーへ戻れる。
- [ ] 通常の編集欄ではSpaceフリックが選択位置を直接変更し、DPAD矢印キーを送信しない。
- [ ] 抽出テキストを提供しない、または選択位置変更を拒否する非private入力先では、Spaceフリックが対応するDPAD矢印キーを送信する。
- [ ] private入力欄では自動フォールバックでDPAD矢印キーを送信しない。
- [ ] 既存のターミナル向けカーソル設定を有効にした場合は、従来どおり非private入力先へDPAD矢印キーを送信する。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。InputConnectionの端末内操作だけを変更し、入力内容を保存・送信しない。

### Design Decisions
- Androidにターミナルやxterm.jsを確実に識別する標準属性はないため、アプリ名の固定リストではなくInputConnectionの能力でフォールバックを判断する。
- private入力欄ではフォーカス越境を避けるため、能力不足でも矢印キーへ自動フォールバックしない。
- 手動のターミナル向け設定は自動判断が不十分な入力先の強制指定として残す。

### Out-of-scope
- アプリpackage名によるターミナル判定。
- 音声認識backend、候補内容、キーボード全体の高さの変更。
- ブラウザmock、公開サイト、リリース作業。

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
