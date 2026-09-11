# Work Notes: 260911-063912-fix-intermittent-keyboard-vertical-offset

## Status: PDH-human-review (Awaiting close approval)

## Checklist
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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: IME local layoutだけの変更で外部providerを使用しない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [x] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [x] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- ユーザの実画面報告と「後で処理」の指示を、再現後に修正する承認として記録した。
- WhyはProduct Briefの安定した外/内画面入力へ接続し、ACはscreen boundsとIME lifecycleで観察できる。
- `[PDH-open] -> [PDH-ticket-review] -> [PDH-ticket-human-review] -> [PDH-implement]`。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

- 添付画像ではかなkey cluster上にcandidate相当の空きがあり、保存かなlayerと旧QWERTYで4行intrinsic高も16dp異なっていた。前ticketでlayer間height差を解消した。
- 修正版前のAPKをAPI 36.1 AVDへ導入し通常欄を初回focusすると、candidateの「許可」だけが画面下端に現れ、KeyboardView全体が画面外へclipされる状態を観察した。
- 実装測定: onCreateInputViewの縦LinearLayoutでKeyboardViewが`height=0, weight=1`。IME windowがwrap/at-most測定する経路ではweighted childのintrinsic 228dpが親desired heightへ安定して寄与せず、candidate 50dpだけのwindowが成立し得る。
- 仮定: KeyboardViewをWRAP_CONTENTにして自身のonMeasure 228/256dpをroot desired heightへ必ず含めれば、host appの初回measure modeに依存しない。
- 反例基準: candidate 50dp、KeyboardView intrinsic高、last layer復元、bottom inset、voice control lifecycleを維持する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `0647b58`: `KeyboardView`を`height=0, weight=1`から`WRAP_CONTENT`へ変更し、IMEの`AT_MOST`計測でもintrinsic高をroot desired heightへ含めるようにした。
- `9dd8a10`: private editorでcandidate stripを`GONE`にしていた別の50dp移動を`INVISIBLE`へ変更し、通常→password→通常のroot/keyboard高不変を固定した。
- `b7e551c`: private editorから始まるlifecycleでもinput view生成時点からstripを`INVISIBLE`にし、候補内容を描画せず50dpを保持するtestを追加した。
- Robolectricで候補欄50px、keyboard 228px、root 278pxを固定し、日本語候補・英字候補・音声Recording・PermissionRequiredの各内容でroot高が変わらない回帰testを追加した。
- API 36.1 AVDの修正前はcandidateの「許可」だけが下端へ出てkey clusterがclipされた。修正後は候補欄とQWERTY 4行が全表示され、初回表示とhide/showのkeyboard crop MD5はいずれも`eed456f4787b50f7cc3da06b21444638`、hide/show後と入力欄切替後の全画面PNGも同一だった。
- `scripts/test-all.sh`: fast-check 5件、全unit、lint、debug APK buildが成功。`connectedDebugAndroidTest`: API 36.1 AVD 7件成功。

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
| 1 | private editor切替 | Major | `GONE`でrootが50dp縮み、key上端が動く | 採用・解消 | `INVISIBLE`へ変更し通常→password→通常を回帰test化 |
| 2 | private editor初回生成 | Minor | `onStartInput`が先行すると新規stripが既定VISIBLE | 採用・解消 | `onCreateInputView`でもprivate visibilityを設定 |

- review-2: `0647b58` + `9dd8a10`で前Major解消、新規Critical/Majorなし。Minorも`b7e551c`で解消した。
- AC4は`KeyboardView.onSizeChanged`がwidth/height変更時にtargetを再構築し、`updateBottomInset`がpadding更新後にrebuild/requestLayoutする既存経路とWRAP_CONTENT rootが整合することを独立reviewで確認した。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md` Design decision 17へ、候補欄固定50dpとKeyboardView intrinsic `WRAP_CONTENT`によるIME root高の契約を追記する。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

- 確認手順: 任意アプリで通常欄をfocusし、IMEをhide/showする。別入力欄とpassword欄へ切り替えても候補欄50dpと4行key clusterの上端・下端が動かないことを確認する。
- ユーザの明示close承認まではticketを閉じない。

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
