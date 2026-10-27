---
priority: 2
base_branch: features/261012-114749-voice-return-cancel-status
description: "BitwardenなどのAndroidインライン自動入力候補をIME内に表示する"
created_at: "2026-10-27T01:50:56Z"
started_at: 2026-10-27T01:51:59Z
closed_at: null
canceled_at: null
---

## 261027-015056-support-inline-autofill

### Why
Bitwardenを自動入力サービスにしても、現行IMEはAndroidのインライン候補を受け取らない。日常のログイン欄でキーボードを切り替えずに認証情報を選べるようにする。Product Briefの「一般的なAndroidのテキスト欄で使う」を担う。

### What / Acceptance Criteria
このticketが終わると、Android 11以降でBitwardenを自動入力サービスにしている利用者が、masuidrive-kbdの候補欄から認証情報を選んで入力できるようになる。

- [ ] AC 1: 利用者がBitwardenでインライン表示を有効にし、対応するログイン欄を開くと、提供された自動入力候補がキーボードの候補欄に表示される。
- [ ] AC 2: 利用者がその候補をタップするとAndroidの自動入力経由でログイン欄が埋まり、IMEが候補文字列を別途入力しない。
- [ ] AC 3: 利用者が入力欄を切り替えるか候補がなくなると、前の欄の自動入力候補は消える。通常の文字・変換候補が必要な欄では引き続き選択できる。
- [ ] AC 4: Android 9・10の利用者は従来どおり文字を入力でき、Bitwardenが使うAndroidのポップアップ自動入力をIMEが妨げない。Android 11以降でも自動入力候補が届かない欄では従来どおり文字を入力できる。

### Architectural Invariants check
AI-1〜4と矛盾しない。自動入力サービスが提供する保護された表示をAndroid経由で扱い、IMEは認証情報の値を取得・保存・ログ出力しない。

### Design Decisions
- Android 11以降の標準インライン自動入力APIを使い、提供者をBitwarden専用に固定しない。Bitwarden以外の対応サービスも同じ経路で表示する。
- 候補欄の既存50dp領域へ横並びで表示し、キーボード全高を変えない。インライン候補がある間は通常候補より優先し、消えたら通常候補を戻す。
- 認証情報の内容をIMEの文字列モデルに変換せず、Androidが渡す候補Viewを表示する。選択後の入力はAndroidと自動入力サービスに任せる。

### Out-of-scope
- Bitwardenの保管庫への直接接続、認証情報の取得・保存、パスキー管理。
- Bitwarden側の自動入力サービス設定をIMEから変更すること。
- Android 10以下にインライン表示を再実装すること。

---
Work notes: `note.md`
