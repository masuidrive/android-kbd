# Work Notes: 261012-082706-paste-clipboard-content

## Status: PDH-human-review (Awaiting user device review)

## Checklist
- [x] 画像入りクリップボードで Paste キーが動くようにする。
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み（画像入りでPasteが動かないとの修正依頼）
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み（最終 SHA `d0e6fbb` で `--parallel --connected` 3/3 PASS）
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 外部 provider を使わない Android IME のローカル処理。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（画像はcommitContent、実テキストは標準Paste/fallback）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した（text-only clip は修正前後とも標準Paste成功、画像URI-only非対応は前後とも無操作）
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録（2 Criticalとmixed MIME Majorを採用・解消、非採用なし）
- [x] PDH-verify: AC 裏取り Agent が AC1〜4 を全件 VERIFIED。コード、32件focused、別アプリ間の画像受信、非対応欄の実キー観察。
- [x] PDH-verify: Surface Observer 観察済み（directorがエミュレータ上の実キー上フリックで別アプリ間のPNG表示と非対応欄KEEP保持を観察。物理端末と一般チャットアプリは未観察）
- [x] PDH-verify: ドキュメント更新の要否を確認済み（technical-reference、sites-native-spec、site/manualを更新。PDH配布物更新は不要）
- [x] PDH-verify: technical-reference.md 突合済み（画像commitContent、混在clip実テキスト、private遮断を確認）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [x] PDH-human-review: ユーザがレビュー結果と未観察範囲の提示後に「リリースして」と指示し、クローズを明示承認した。実機での確認実施は未申告

## PDH-ticket-review. Ticket contract check
影響レイヤー: Android app、IME service、unit tests、instrumentation/device tests、docs。Custom View のキー配置・ジェスチャーは変更しない。Mozc JNI も変更しない。消費側 surface は Android InputConnection の commitContent / 標準 Paste とテキスト入力欄。
AC1 は画像を受け取れる編集欄で Paste 後に画像が表示されるか、AC2 はテキストが貼られるか、AC3 は機密欄に何も渡らないか、AC4 は画像 URI 文字列が出ないかを観察する。
確認済み: 現行 TextInputController.paste はテキスト MIME 以外を返すため、画像は入力先へ全く渡らない。Android InputConnection の公式 API は performContextMenuAction(android.R.id.paste) を提供し、入力先の既存 Paste 処理を呼べる。画像を IME が coerceToText して貼るのは不適切。Product Brief の offline/privacy invariant と矛盾せず、未完了の依存もない。
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた。現行コードは画像MIMEを排除。InputConnection.performContextMenuAction の公式仕様を確認。実機またはエミュレータで画像対応編集欄の受信は実装後に測り、受け取れなければAC1未達として止める。

## PDH-implement. 実装ログ
実装前の仮定として標準Pasteで画像対応を判定できると考えたが、独立reviewで否定されたため、directorがticketのDesign DecisionsをcommitContentへ修正した。
`14a9f19`: 標準 Paste を先に入力先へ委譲し、拒否時に文字列へフォールバック。`abdfced`: mixed MIME の URI が文字列になる経路を閉じ、回帰テストを追加。`TextInputControllerTest` 29件 PASS。`5241e4c`: reviewer Critical に従い、画像clipの標準Pasteを入力先の画像MIME宣言で制限。非対応先では文字列変換・選択文字削除を起こさない。修正前の非対応欄2件は失敗し、修正後29件PASS。
仮定と実測: Android が `InputConnection.performContextMenuAction(android.R.id.paste)` を入力先へ伝えることを API とエミュレータで確認。画像 URI-only の観測用入力欄（Paste を受け取る実装）では、IME の Enter 上フリック後に `IMAGE_RECEIVED content://paste-observer/test.png` を確認。画像非対応の標準 EditText は `onTextContextMenuItem(paste)` が true を返すが入力文字列は空で、URI 文字列は入らなかった。初回の検証用 URI は実画像を持たなかった。その後、`image/png` を `EditorInfo.contentMimeTypes` に宣言した観測アプリで有効なcontent providerを使い、IME Enter上フリック後に `IMAGE_DECODED 2x2` と赤いPNG表示を確認。非対応欄では選択文字 `KEEP` を置き、https/content両URIの画像clipに対して上フリック後も `KEEP` を維持。スクリーンショットは `tests/image-paste-emulator.png` と `tests/unsupported-paste-emulator.png`。実機・一般のチャットアプリでの画像貼り付けは未確認。
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

## PDH-review. 品質検証結果
最終コードを含む `e04fbc7` で `scripts/test-all.sh --parallel --connected` 3/3 PASS、connected 27/27 PASS。初回最終SHA実行はエミュレータ容量判定でtest APK installが失敗し、unit内の既知の絵文字テストが単発失敗。容量判定を一時調整した逐次再実行は全件PASSし、設定は検証後に復元する。
初回 `scripts/test-all.sh --parallel --connected` は fast-checks と connected (real Mozc) PASS、unit/lint/apk 内の無関係な `ImeHideBarTest.privateEditorSwitchesToItsDedicatedEmptyPickerWithoutReusingPublicPicker` が1件失敗。対象テスト単独再実行はPASS。修正後SHAで全スイートを再実行する。
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
| 1 | 入力先との対称関係 | Critical | 画像非対応の標準EditTextがPaste成功を返し、URI文字列化や選択文字削除を起こし得る | 採用・修正済み | `5241e4c`で画像MIME非対応の入力先への標準Paste委譲を停止し回帰テストを追加 |

### Findings (PDH-review-2)
| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 2 | Android rich content API | Critical | `contentMimeTypes` は context-menu Paste の画像対応を保証しない | 採用・修正済み | `90a0cc1` で MIME 一致時の画像を `InputConnection.commitContent` に切り替え、標準 Paste と分離。再レビューで解消確認。 |
| 3 | mixed Clip 回帰 | Major | 画像と文字の両MIMEで先頭項目が実テキストの場合に貼れない | 採用・修正済み | `ba95da8` で `Item.text` をURI coercionなしで保持し、回帰テストを追加。再レビューで解消確認。 |

`ba95da8`: mixed MIME の実テキスト保持と32件のfocused testを追加。`90a0cc1`: 画像 clip は先頭の `content://` URI と入力先の画像 MIME 一致を確認し、read-permission flag 付き `commitContent` へ送る。失敗時は URI テキスト化・context Paste をしない。文字clipは従来の標準 Paste と実テキスト fallback を維持。focused `TextInputControllerTest` 30/30 PASS。
最終コードのエミュレータ観測: `image/png` を宣言して `commitContent` を実装した入力欄で、Enter 上フリック後に `CONTENT_DECODED 2x2 flags=1` と赤い PNG 表示を確認。検証用入力欄は context-menu Paste を拒否するため、画像が新経路で届いたことを区別できる。画像非対応欄に選択文字 `KEEP` を置くと、同じキー操作後も `KEEP` が残った。`tests/image-paste-emulator.png` と `tests/unsupported-paste-emulator.png` を最終コードで撮り直した。最終追加観測では画像提供元 `com.example.pasteprovider`、入力先 `com.example.pasteobserver`、IME を別アプリに分離し、`CONTENT_DECODED 2x2 flags=1` と画像表示を再確認した。途中、入力先のpackage visibility未宣言で `No content provider` が出たが、検証アプリの `<queries>` を追加後に成功。一般のチャットアプリ固有UIでの表示は未確認。

## Technical reference 更新
`technical-reference.md` に画像の `commitContent` と文字列の標準 Paste・fallback、機密欄の遮断を追記。mixed MIMEで画像非対応なら実際の `Item.text` を貼る動作も追記。`docs/reference/sites-native-spec.txt` と `site/manual.html` に同じ利用者向け動作と mock の制約を反映。
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
2026/10/12、画像Pasteと音声候補取消の差分・テスト結果・端末内日本語音声モデル不足による実音声未観察を提示した後、ユーザが「リリースして」と指示。両ticketのcloseおよび公開の明示承認として扱う。ユーザ自身の実機確認は報告されていない。
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
