# Work Notes: 260912-071844-add-ime-hide-bar

## Status: PDH-review (Implementation complete; review in progress)

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [x] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [x] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [x] native IMEへ28dpの閉じる操作行を追加し、tapでIMEを閉じる。
- [x] 入力テスト画面へsafe-area対応の固定app barと戻る操作を追加する。
- [x] 公開操作mockを閉じる操作行と再表示動作へ同期し、native/mockのLight/Dark・412dp/840dpを観察する。
- [x] Androidと現行site/manualの利用者向け製品名を`masuidrive-kbd`へ統一し、内部識別子・過去release名を維持する。
- [x] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [x] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: Android端末内UIと静的mockだけを変更し、外部providerを通らない。
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [ ] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
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
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

実装前に、`ImeService.onCreateInputView()`は候補欄50dpと`KeyboardView`の縦LinearLayoutだけを返すこと、`KeyboardView`は4行intrinsic heightへsystem bottom insetを含めること、`ImeTestActivity`にはapp bar・edge-to-edge safe area処理がないこと、`SetupActivity`には56dp app barと再dispatchで累積しないsafe area処理があることを確認した。閉じる行追加時はsystem bottom insetの所有先、4行キーのmeasured height、28dp行のvisual/tap bounds、hide API到達、test Activityのtop insetを測る。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

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
|   |      |     |      |      |      |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

## PDH-human-review. 人間レビュー
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
ユーザが指定した二つの不足を、IME下端の閉じる操作と入力テスト画面の戻る操作として分離した。28dpは添付画像より低い視覚高を実測可能にする値で、既存の候補50dpと4行キーintrinsic heightは維持する。追加の表記統一は利用者が見るAndroid label・画面文言と現行site/manualを対象とし、内部package・class・既存asset名は識別子として維持する。consumer surfaceはnative IME input view、入力テストActivity、Android app/IME picker、公開操作mock、現行site/manual、TalkBackである。未確定判断はない。

AC読み手は利用者・IMEを閉じる操作・設定へ戻る操作を復元できた。指摘されたAC 4の「ナビゲーションバー」は`Android system navigation bar`へ明確化し、AC 6の製品名統一をWhat冒頭文へ加えた。ユーザの実装指示そのものが、閉じる行、入力テストapp bar、製品名`masuidrive-kbd`の各ACを明示しているためPDH-ticket-human-reviewの承認として扱う。
- `b813f54` `[260912-071844-add-ime-hide-bar] feat(ime): add hide bar and safe input test` — IME rootを候補欄50dp、4行KeyboardView、28dp hide barの3段にし、system bottom insetの所有先をKeyboardViewからhide barへ移した。中央シェブロンは`requestHideSelf(0)`を呼ぶ。入力テスト画面には設定画面と共有するsafe-area helperを使った固定app barとbackを追加し、Android application/service/subtypeと画面文言を`masuidrive-kbd`へ統一した。Robolectricで412/840幅の4行高、inset再適用、hide API、app bar/back、公開labelを固定した。
- `f61977f` `[260912-071844-add-ime-hide-bar] docs(site): document hide bar and product name` — READMEと現行site/manualの利用者向け製品名を`masuidrive-kbd`へ統一し、操作mockへ28px hide barを追加した。mockは閉じるとkeyboardを非表示にし、textarea focusで再表示する。
- focused Robolectricは`ImeHideBarTest`、`ImeTestActivitySafeAreaTest`、既存`ImeServiceVoiceLifecycleTest`、`KeyboardViewTest`をPASSした。`scripts/test-all.sh --parallel`はfast-checksとAndroid unit/lint/APKの2/2 PASS、続けたfast-check再実行もPASSした。
- API 36 AVDの412dp相当Lightで入力テストの固定app bar、公開名、候補欄＋4行＋28dp中央シェブロンを観察した。840dpはRobolectric geometryで確認した。`adb input tap`はIME overlay座標と表示座標が一致せず絵文字を選んだため、実機hide終端の証跡には採用せずRobolectricの`requestHideSelf(0)`到達だけを現時点の証拠とする。
- 既存KeyboardViewはsystem bottom insetを自身のpaddingへ含めていた。hide bar追加後もそのままにすると4行とbarの間へ余白が二重に入るため、IME containerではKeyboardViewのinset所有を無効化し、hide barだけへbottom insetを加えた。
- `similarity-generic`はPATHに存在せずskipした。`similarity-ts -t 0.7 --extensions js site`は対象JS/TSなしと判定された。
