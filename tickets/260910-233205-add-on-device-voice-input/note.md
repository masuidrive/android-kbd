# Work Notes: 260910-233205-add-on-device-voice-input

## Status: PDH-ticket-review

## Checklist
<!-- stage を移るたびにこの節を見る。節を stage ごとに割らない —
     割ると「その stage の分だけ」を見て、他が残っていることに気づかない。
     ユーザに頼まれたことと、作業中に見つけた «あとでやる» もここへ足す
     （着手より先に書く。規則は PDH-AGENTS.md「Execution Model」）。
     当てはまらない項目は `- [-] ... - skip: <理由>` と書いて理由を残す（理由なしの `- [-]` は未了扱い）。
     未了の一覧は `./ticket.sh check`。 -->
- [ ] PDH-ticket-review: Why が product-brief.md に接続し、AC が観察可能で、ユーザ承認済み
- [ ] PDH-ticket-review: Design Decisions / Out-of-scope / Dependencies / Architectural Invariants check が確認済み
- [ ] PDH-implement: 実装が依存する «確かめていない仮定» を書く前に列挙し、測れるものは測った
- [ ] PDH-implement: implementor が論理単位ごとに commit し、mega-commit にしていない
- [ ] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [ ] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
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
- [ ] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

- API 36 arm64 AVDで`SpeechRecognizer.isOnDeviceRecognitionAvailable(context)`は`true`。`RecognitionService`は2件、microphone featureも存在する。
- 同AVDの`checkRecognitionSupport`はerrorなしだが`installedOnDeviceLanguages=[]`。日本語model導入済みとは扱わず、実発話認識成功はこのAVDでは未検証とする。自動model downloadやnetwork recognizer fallbackは行わない。
- raw log: `app/build/outputs/androidTest-results/connected/debug/Medium_Phone_API_36.1(AVD) - 16/logcat-com.masuidrive.gestureime.VoiceRecognitionAvailabilityTest-reportsInstalledJapaneseModelSupport.txt`

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

- ユーザーがstage1公開後に音声入力を実装して再公開する順序を明示し、stage1公開完了後の実装を開始した。起票時点だけに適用された実装保留は解除された。
- AC案は会話で示された要件候補を観察可能な形に整理したもので、個別のAC承認済みとは扱わない。
- 着手前にFold7実機で端末内日本語認識service/modelのavailabilityをprobeし、既存5レイヤーを妨げないマイク配置をticket reviewで決定する。
Candidate strip右端へ音声状態UIを置き、backendは端末内recognizer factoryだけを使用する。Serviceはvoice generationとeditor sessionを別々に検証し、通常キー・入力欄切替・取消で古いcallbackを破棄する。`RECORD_AUDIO`許可はSetupActivityでユーザー操作後に要求し、許可直後の自動録音は行わない。

- UI描画snapshot token、backend voice generation、editor session tokenの3層で古いtap/callbackを拒否する。通常キーが待機中の録音開始を追い越した場合もUI token不一致で開始しない。
- `onFinishInputView`で音声だけをcancel/destroyし、`onStartInputView`で同じViewを再利用する場合もprivacy/permission/availabilityからcontrolを復元する。通常かなcompositionはこのview lifecycle処理で変更しない。
- recognizerのcreate/support/start/stop/cancel/destroy例外はmain threadでUnavailableへ閉じ、成功扱いしない。最新Unavailable理由を保持して説明操作へ使用する。
- 重複検出 skip: `similarity-generic`が環境に導入されていない。
- 実装commitは `e82d20b`、UI追補は `30e15dc` / `648724d`、IME view lifecycle修正と回帰は `7f871a0` / `2900c38`、API 31 lint境界は `6ec20cc`。独立再reviewでCritical/Majorなし。
- `6ec20cc`でfast-check 5件、unit 62件、lint、assembleがPASS。connectedはMozc 5件と音声availability/model probe 2件の計7件がPASS。APK SHA-256は`4e691b4003dcdc883de55dd812b27fd7fba3061b408867d561ac63b821fa2cea`。
- API 36 AVDで未許可時の候補維持、許可後のhide→showでIdle復帰、model未導入時の`非対応`、private欄でvoice/candidate strip非表示を実画面確認。日本語model未導入のため、実発話、preview、確定はfake backendによる回帰までであり実音声成功とは扱わない。
- ユーザー追加指示により、音声v0.3公開後はQWERTY微調整ticket、その後は英数字候補bufferを含む残りticketを順次実装する。人間レビュー未完の既存ticketを完了扱いにはしない。
