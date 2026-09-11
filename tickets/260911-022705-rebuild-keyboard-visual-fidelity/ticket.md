---
priority: 2
base_branch: features/260911-014049-add-layer-key-push-to-talk
description: "正本HTMLを定量分析し、nativeキーボードの形状・影・文字・popup・swipe animationを忠実に描き直す"
created_at: "2026-09-11T02:27:05Z"
started_at: 2026-09-11T02:40:35Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 260911-022705-rebuild-keyboard-visual-fidelity

### Why
Galaxy Z Fold7で日常入力する利用者が、正本HTMLモックと一貫した形状・文字配置・操作feedbackを持つnativeキーボードを使えるようにする。現在は特にQWERTYのキー形状、角丸、影、ラベル位置、popupが正本と大きく異なり、縦swipeの表示も位置関係が崩れて見える。

### What / Acceptance Criteria
この ticket が終わると、Gesture IME利用者が、正本HTMLと視覚・動作の対応を確認できるnativeキーボードで入力できる。
- [ ] AC 1: 5レイヤーのキー外形、角丸、間隔、背景、影が正本HTMLの対応状態と視覚的に一致する。
- [ ] AC 2: 主・補助ラベルの書体、サイズ、baseline、相対位置、濃さが正本HTMLと一致し、QWERTYを含む各レイヤーでキー境界から不自然にはみ出さない。
- [ ] AC 3: flick popupの外形、位置、文字、影と、上・下swipe中の拡大文字の全frame位置関係が正本HTMLと一致する。
- [ ] AC 4: EnterからPasteを選ぶanimationが正本HTMLの意図どおり動き、HTML側にある元bugはnativeへ再現せず補正結果をHTML比較にも明記する。
- [ ] AC 5: 412dp/840dpと文字倍率1.0/1.3/2.0で比較しても、キー・ラベル・popupが切れず、操作対象が重ならない。
- [ ] AC 6: 日本語キー高、QWERTY BSの⌫、layer-key hold音声、Dual Flickなど後続の明示仕様を維持する。

### Architectural Invariants check
AI-1〜AI-4と矛盾しない。描画変更は入力内容の保存・通信・Mozc学習を追加せず、既存のnative Custom Viewを維持する。

### Design Decisions
<!-- 既知の設計判断と理由を箇条書きで明示。
     例: - データ保存形式: data URI (Files API は将来 ticket、本 ticket では不要)
     例: - 423 reject ではなく 422: validation error として扱う -->
- 正本は `docs/reference/mock-source.html` とし、推測値ではなくCSS/DOMの定量測定を先に行う。
- nativeの同一描画primitiveへ寸法・typography・shadow・popupを集約し、通常状態とanimation frameの両方を比較する。
- Enter/PasteのHTML側元bugは正本の意図を分析したうえで補正し、誤動作そのものは移植しない。

### Out-of-scope
<!-- やらないこと (scope creep 防止)。
     「ついでにやりそう」「次の ticket でやる」を明記する。 -->
- 英数字候補bufferの実装（本ticketのv0.5公開後にv0.6として実施）。
- 日本語配列、Mozc変換、音声認識方式、Dual Flickの機能変更。
- この起票時点での実装、HTML分析、ticket start、v0.4公開作業。

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

- v0.4の公開完了後に開始する。

---
Work notes: `note.md`
