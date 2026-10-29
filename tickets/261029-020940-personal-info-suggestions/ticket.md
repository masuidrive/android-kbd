---
priority: 2
base_branch: features/261027-065123-make-ca-tap-tab
description: "設定した氏名・メールアドレス・住所をキーボードから選んで入力する"
created_at: "2026-10-29T02:09:40Z"
started_at: 2026-10-29T02:11:37Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## 261029-020940-personal-info-suggestions

### Why
氏名・メールアドレス・住所を繰り返し手入力する負担を減らす。Product Brief の「同じジェスチャー体系で素早く操作したい」と「入力内容を外部へ送らない」に沿う。

### What / Acceptance Criteria
この ticket が終わると、masuidrive-kbd の利用者が、設定に登録した氏名・メールアドレス・住所を、入力先に応じた候補または候補欄のアイコンから選んで入力できるようになる。

- [ ] AC 1: 利用者は設定画面で氏名・メールアドレス・住所をそれぞれ登録、変更、削除でき、再起動後も登録値を使える。
- [ ] AC 2: 利用者がメール・氏名・住所の種類を明示した通常の入力欄を開くと、登録済みの該当項目が候補欄に現れ、タップするとその欄へ入力される。
- [ ] AC 3: 利用者が種類を明示していない通常の入力欄を開いても、候補欄の個人情報アイコンから登録済み項目を選んで入力できる。空欄の項目は選択肢に出ない。
- [ ] AC 4: 利用者がパスワード欄などの非公開入力欄を開いても、登録した個人情報とそのアイコンは候補欄に出ない。
- [ ] AC 5: 利用者は Bitwarden などの既存のインライン自動入力候補を、従来どおり選択できる。

### Architectural Invariants check
AI-1〜AI-4 と矛盾しない。登録値は端末内に保持し、入力先へは利用者が選んだ時だけ送る。非公開欄では登録値を候補化しない。

### Design Decisions
- 登録枠は氏名・メールアドレス・住所を各1件とし、空値の保存で削除する。複数プロファイルは今回扱わない。
- 入力先の種類は `EditorInfo.inputType` の明示情報だけで自動判定する。一般的なWebフォームのラベルや名前から推測しない。
- 種類を判定できない欄には、登録内容を自動で並べず、候補欄のアイコンから利用者に選ばせる。
- 既存のインライン Autofill はサービス側の候補表示とし、登録した個人情報の候補と区別する。

### Out-of-scope
Bitwarden の保存情報の取得、Android の AutofillService の実装、SMS/Gmail の一時コード、カード情報、登録値の端末間同期は扱わない。

▼ 以下は該当する情報がある場合のみ ▼

---
Work notes: `note.md`
