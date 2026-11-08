# Work Notes: 261108-024949-move-ca-tab-to-horizontal-flick

## Status: PDH-human-review

## Checklist
- [x] 利用者依頼: 英数字左端のキーは通常タップを無反応とし、左右フリックでTabを入力する。
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み - skip: 端末内IMEのキー操作であり外部APIを使わない。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (実IMEをAndroid 36.1 emulator 412dpで観察、物理端末は未確認)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（製品マニュアル・トップ・README・参照仕様を更新。PDH distribution は変更不要）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->
Why は Product Brief の同一ジェスチャー体系での入力に接続する。観察面は Android IME の英数字・記号レイヤー、ブラウザデモ、操作説明。影響レイヤーは Android app・IME service（Tab送信は既存動作）・custom view UI・unit tests・instrumentation/device tests・docs。Mozc JNI は無関係。未確定の製品判断や依存 ticket はない。利用者の指示はこの AC の実装を承認している。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 修飾キーは QWERTY と Symbols で同じ KeySpec を使い、既存コードでは中央が Tab、上下が Alt/Ctrl。左右の出力は未定義だった。実装後、attached KeyboardView の MotionEvent で中央・左右・上下・閾値未満・中央復帰・キャンセルを確認する。端末実機は利用できないためエミュレーター結果と分けて報告する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->
未確認の仮定: 修飾キーに左右方向を追加しても従来の縦方向専用 gesture router が中央復帰を扱えるか。測定の結果、縦方向専用経路では左右選択後の中央復帰が効かない。KeySpec の割当方向を見て通常の方向選択経路を使うよう修正した。

- `d90b770` Android の左右Tab・中央無反応・アクセシビリティ・MotionEventテスト。
- `13144f1` ブラウザデモと説明書・参照仕様の同期。
- `533e021` 接続テストの旧期待値と README を更新。
- `c012d78` 選択中の左右Tabで one-shot 修飾を消費し、次の文字へ漏れないよう修正。
- `741f1f0` ブラウザのキーボード矢印操作経路も同じ one-shot 処理に統一。
- `744ee4e` 左Tabの画面端到達性を修正し、接続テストを画面内座標で検証するよう変更。
- `0a8b814` ブラウザデモでも左方向だけ判定距離を画面端へ合わせ、技術参照を更新。
- `d6f147c` 左寄りのタップ誤発火を防ぐ8dp下限と指ぶれテストを追加。
- `d02a515` connected testで前ケースの修飾状態を明示リセット。
- Browser demo: phone幅412px、左フリックの選択表示 `Tab` と入力結果 `\\t`、右フリックで追加の `\\t`、中央タップ無反応を確認。狭いキーから複数文字ラベルがはみ出したため、中央表示を `↔` に変更して再表示した。
- Browser demo: Ctrl選択後の右フリックとキーボード `ArrowRight` はいずれも `{key: 'Tab', ctrlKey: true}` を発火し、次キー前に修飾選択を解除した。

途中確認コマンド（HEAD `741f1f0`、端末は Android 36.1 の412dpエミュレーター。後の実IME観察でAC2未達と判明）:

```text
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk PATH=/Users/masuidrive/Library/Android/sdk/platform-tools:$PATH scripts/test-all.sh --parallel --connected
Parallel mode: logs in /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.80tJg9zwGu
  Starting: fast-checks (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.80tJg9zwGu/fast-checks.log)
  Starting: android unit, lint, apk, connected (real Mozc) (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.80tJg9zwGu/android_unit,_lint,_apk,_connected__real_Mozc_.log)

========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk, connected (real Mozc)

Passed: 2 / 2
```

最終コードSHA `d02a515` の全体テスト:

```text
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk PATH=/Users/masuidrive/Library/Android/sdk/platform-tools:$PATH scripts/test-all.sh --parallel --connected
Parallel mode: logs in /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.39LAqGkpzy
  Starting: fast-checks (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.39LAqGkpzy/fast-checks.log)
  Starting: android unit, lint, apk, connected (real Mozc) (log: /var/folders/k8/m6dxst112gzgyk4l75g0zzsw0000gn/T/tmp.39LAqGkpzy/android_unit,_lint,_apk,_connected__real_Mozc_.log)

========================================
  Summary
========================================
  PASS: fast-checks
  PASS: android unit, lint, apk, connected (real Mozc)

Passed: 2 / 2
```

## PDH-review. 品質検証結果
<!-- PDH-review-1 / PDH-review-2 のように attempt ごとに記録する。
     独立 reviewer（1 人以上。構成と model は CLAUDE.md「チーム構成・モデル設定」）の
     実装後 review 結果を統合。
     2 attempt 同種 Critical 再発で root cause 診断 → escalation。
     実装後 review 特有 gate: Ticket 不可侵 / logical commit cadence / E2E real API / テスト全件 PASS -->

### Findings (PDH-review-1)
<!-- 記録・分類・提示の運用は pdh-dev `_review.md`
     「スコープ外問題と過剰実装の扱い」に従う。 -->

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | 接続テスト・説明 | Major | 中央tap Tabの旧期待値とREADMEの旧説明が残る | 修正済み | `533e021`。修正前の connected suite は32件中1件失敗、修正後は32件成功。 |
| 2 | one-shot修飾 | Major | Ctrl/Alt選択後の左右Tabが修飾を消費せず、次の文字へ漏れる | 修正済み | `c012d78`。修正前: Ctrl→Tabはplainで次の英字にCtrlが適用。修正後: Ctrl+Tabのdown/upを送信、選択解除、次の英字は通常入力。 |
| 3 | ブラウザキーボード経路 | Major | `ArrowLeft`/`ArrowRight` が修飾選択を残す | 修正済み | `741f1f0`。修正前はplain Tab、修正後は `virtualkey {key:'Tab', ctrlKey:true}` で解除。ポインタ操作でも同じ出力を観察。 |
| 4 | 左端の画面到達性 | Major | 0.5wキー中心から標準判定距離24dpは画面外へ出る | 修正・実IME再観察済み | `744ee4e`/`0a8b814`。修正前は実IMEの最大左フリックが無入力。修正後はブラウザphone幅・connected画面内座標・実IMEでTab。 |
| 5 | 左寄りタップの指ぶれ | Major | 判定距離を開始xの70%だけにすると端側では1～数dpまで下がる | 修正済み | `d6f147c` で8dp下限。左寄り5dpの移動はnative/mockとも無入力。 |

独立reviewerが `d02a515` までの全diffを再確認し、残存 Critical/Major なし。非採用 finding なし。壊していない側の対照は、QWERTY英字 `s` の通常タップが修飾なしの `CommitText("s")` のままであることと、上・下フリックが Alt/Ctrl のままであることを前後の集中テストで確認した。

AC裏取り Agent は `741f1f0` で AC 1〜4 を全件 VERIFIED。Android 36.1 emulatorの attached production `KeyboardView` へ実 MotionEvent を送った connected test で、412dp・840dp、QWERTY・Symbols の中央・左右・上下・閾値未満・中央復帰・キャンセルを確認。実ブラウザの英数字・記号でも左右Tabと修飾状態の消費を観察。物理端末の確認はしていない。

その後、Surface Observer が実IMEを Android 36.1 emulator（412dp）で入力欄へ表示し、中央tap無反応・右Tab成功・左Tab失敗を発見。左端0.5wキーの中心は画面端から約35pxで、標準24dpの判定距離は約63pxのため画面内では左フリックを確定できなかった。attached-view testが画面外の負座標まで動かしたため見逃した。`741f1f0` の AC2 VERIFIED 判定はこの実surface証拠で取り消した。修正後ブラウザphone幅412pxではキー中央x≈20px→画面端x=0でTab、左寄り7pxの揺れで無入力を確認。実IME再観察で中央tap無反応、左寄り約5.7dpの小揺れは無反応、キー中央から画面端までの左フリックと右フリックはいずれもTab1回でemail欄からname欄へ移動した。実 `ImeService` の表示はdumpsysでも確認した。証拠: `surface-qwerty-final.png`。物理端末は未確認。

AC裏取り Agent は最終SHA `d02a515` で AC 1〜4 を全件再検証し VERIFIED。実IMEでの左・右Tab、中央無反応、小揺れ無反応、ブラウザ操作、connected 32/32、native/mock一致を確認した。未検証ACなし。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->
`technical-reference.md` の C/A キーの中央Tabを左右Tabに置き換え、左端キーだけ開始位置と画面端の距離で左判定を調整することを追記した。ほかの ticket 由来の記述は維持した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->
最終コード `d02a515` のAPKを物理端末に入れ、英数字レイヤーの左端 C/T/A キーを入力欄で確認する。中央タップは何も送らず、左・右フリックは各1回だけTabを送る。端に近い短い指ぶれではTabを送らず、上・下フリックは従来どおりAlt・Ctrl、指定中の中央タップは解除される。記号レイヤーでも同じ操作を確認する。エミュレーターの実IMEでは中央・短い指ぶれ・左右Tabを確認済み。物理端末では未確認であり、利用者の結果と明示承認を待つ。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
