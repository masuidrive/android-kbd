---
priority: 2
base_branch: default  # Override base branch for start/close (default: use default_branch from config)
description: "Galaxy Z Fold7 向け Android native IME の基礎入力を実装する"
created_at: "2026-09-10T16:30:36Z"
started_at: 2026-09-10T16:32:51Z # Do not modify manually
closed_at: null   # Do not modify manually
canceled_at: null # Do not modify manually
---

## Galaxy Z Fold7 向け native IME を実装する

### Why
Galaxy Z Fold7 の外画面と内画面で、日本語・英数字・記号・カーソル操作を一つのジェスチャー体系から、入力内容を外部へ送らず利用できるようにする。

### What / Acceptance Criteria
この ticket が終わると、Galaxy Z Fold7 を使う人が、Android の入力方法として選んだキーボードから仕様の基礎入力をオフラインで行えるようになる。

- [ ] AC 1: 利用者が IME を有効化して入力欄を選ぶと、日本語12キー、1〜9と最下段 - 0 . のテンキー、上下左右のカーソル、QWERTY、ASCII記号の5レイヤーを使える。左下キーはタップでQWERTY形式から日本語へ・テンキー形式からQWERTYへ移動し、フリックは全レイヤーで左＝記号、上＝日本語、右＝テンキー、下＝QWERTYへ移動する。
- [ ] AC 2: 利用者が日本語の各キーを中央・左・上・右・下へ操作すると、あ行＝あいうえお、か行＝かきくけこ、さ行＝さしすせそ、た行＝たちつてと、な行＝なにぬねの、は行＝はひふへほ、ま行＝まみむめも、や行＝や・左括弧・ゆ・右括弧・よ、ら行＝らりるれろ、わ行＝わをんー〜の対応する文字が未確定文字になる。同じキーの連打は同じ文字を追加し、濁点・半濁点・小文字のキーで直前のかなを変更でき、18dp未満の初期の揺れでは別方向が確定しない。
- [ ] AC 3: 利用者がQWERTY英字をタップすると小文字、上スワイプするとその1文字だけ大文字が入力される。下フリックの副文字はq〜pに1234567890、a〜lに @ # $ & * ( ) ' "、z〜mに % - + = / ; :、カンマに !、ピリオドに ? が対応する。C/Aは上でAlt・下でCtrlが選択され、未選択時のタップは無効、選択後のタップで解除できる。半幅BSは下フリックでだけ削除し、タップ・上フリックでは入力もポップアップも発生しない。
- [ ] AC 4: 利用者が Space を方向へ動かすと、開始時に決まった横軸または縦軸を指を離すまで維持してカーソルが移動し、絵文字や結合文字を途中で分割しない。
- [ ] AC 5: 利用者がかなを入力すると Mozc 接続から候補が表示され、Space で候補を巡回し、候補タップまたは Enter で対象アプリへ確定できる。
- [ ] AC 6: 利用者が文字入力・削除・Enter・Enter下フリックのPasteを行うと入力先へ反映される。Enterは入力欄の検索・送信などの指定アクション、または改行を実行する。Ctrl+Aで全選択でき、その他のCtrl/Alt付き入力は対応アプリへ修飾キーイベントとして届き、次の1キー送信後に修飾表示が解除される。パスワード欄では候補学習とPasteが無効になる。
- [ ] AC 7: Fold7 の外画面相当と内画面相当の幅、回転、ダークテーマ、フォント倍率、アニメーション無効設定でも、キー数と操作体系を保ち、安全領域を避けて読める状態で表示される。
- [ ] AC 8: アプリはネットワーク権限を要求せず、入力文字列をログへ出さず、通常入力とかな漢字変換を端末内だけで完了する。

### Architectural Invariants check
AI-1〜AI-4（完全ローカル、原文非記録、小さな Mozc 境界、一時的な大文字・修飾状態）を実装と検証で維持する。

### Design Decisions
- package は `com.masuidrive.gestureime`、UI は Kotlin Custom View、文字送信は InputConnection を使う。
- UI・状態モデルと変換エンジンを分離し、Mozc は `start/update/nextCandidate/commit/reset` 相当の小さな境界へ閉じ込める。
- 実装・機械検証は Terra、AC 読み手・レビュー・達成確認は Sol を基本にし、最終判断は Director が行う。

### Out-of-scope
- 個人辞書、サービスのエクスポートログ取り込み、学習データ管理画面。
- ネットワーク変換、同期、バックアップ。
- HTML モックの改修または製品としての出荷。

▼ 以下は該当する情報がある場合のみ ▼

### Implementation Notes
- 最終仕様: `docs/reference/sites-native-spec.txt`
- 推奨構成: `ImeService`、`KeyboardRootView`、`KeyboardState`、`GestureInterpreter`、`TextInputController`、`ConversionEngine`、`MozcBridge`

### Dependencies
なし。

---
Work notes: `note.md`
