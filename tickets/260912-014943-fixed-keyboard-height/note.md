# Work Notes: 260912-014943-fixed-keyboard-height

## Status: PDH-open (Opening)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 高さpresetは端末内SharedPreferencesとView測定だけで完結し、外部provider/APIを持たない。
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
- Product BriefのFoldを閉じたスマホ幅と開いたタブレット幅で同じ入力体系へ対応する方針に接続する。AC 1〜5はrecoverableで相互矛盾なし。AC 4は数値をticketへ足さず、変更前の実装値をbaselineとして測って維持する。
- 高さpresetはユーザから具体的に実装を委任された局所判断として、小50dp・標準55dp・大60dpのrow pitchを採用する。いずれも既存10dp row gapを維持し、4行外形は208/228/248dpとなる。標準55dpは既存スマホ幅の値である。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）
  - 変更前の`KeyboardView`は400px幅でrow pitch 55dp、row gap 10dp、face 45dp、4行外形228dpであり、840px幅ではrow pitch 62dp、face 52dp、外形256dpへ拡大する。`KeyboardViewTest`の既存geometry回帰で確認した。
  - 変更前は500pxの一時`EXACTLY` parentを228pxへcapするapp-switch対策が存在するが、幅依存の62dp pitchがFold内幅で高さを増やす。preset化後は同じ500px入力、inset再配信、全layer/Dual Flickで選択presetの位置とtap boundsを維持する。
  - `similarity-generic`はリポジトリに存在しないためskipした。外部provider/APIは本ticketに存在しない。
  - 最終APKをAPI 36 `emulator-5554`へ導入し、Gesture IME選択と`mInputShown=true`を確認した。412dp、840dp相当、landscapeの各有効captureで候補欄・4行・navigation safe areaが可視である。IME pickerだけ、またはIMEが出ていない中間captureは証跡から除外した。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `ed848d3` `[260912-014943-fixed-keyboard-height] feat(keyboard): add persistent fixed height presets`: `KeyboardHeightPreset`を端末内へ保存し、Setupの48dp RadioGroup、IME lifecycle再適用、幅とDual Flickから独立した4行測定を実装した。500pxの一時`EXACTLY`、遅延bottom inset、全layer/Dual Flick、malformed preference、再生成後のSetup選択を回帰テストで覆った。
- `dc74d97` `[260912-014943-fixed-keyboard-height] docs(keyboard): document fixed four-row geometry`: README、manual、technical reference、device verificationへ固定geometryと有効なAPI 36 AVD evidenceを記録した。
- mockのinner幅だけを52pxへ伸ばすCSS overrideを削除し、標準45px rowをmobile/tablet/Dual/voiceで共有した。siteに既存browser harnessがないため、`rg`でinner override不在と基準ruleを静的確認した。
- focused JVM: `:app:testDebugUnitTest --tests ImePreferencesTest --tests SetupActivitySlashCommandsTest --tests KeyboardViewTest --tests ImeServiceVoiceLifecycleTest` PASS。full: `scripts/test-all.sh --parallel` PASS（fast-checks、android unit/lint/apk）。

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
| 1 | reference同期 | Minor | `sites-native-spec`、`android-native-implementation`、参照用mockが内画面でキー高さを増やす旧仕様を残していた。 | 採用・修正 | 高さpresetを幅とDual Flickから独立させた実装と矛盾するため、3ファイルを現行仕様へ同期した。 |

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- `technical-reference.md`の既存16、17をpreset固定の4行geometryとbottom safe areaへ更新し、Setup保存/fallbackの29を追記した。幅依存62dp/52dp記述はこのticketが置換したため更新した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- API 36 AVDは`ime set`後にSetupのIME pickerを経由するとGboardへ戻ることがあった。最終観察はGesture IMEを明示的に再選択し、`mSelectedMethodId=com.masuidrive.gestureime/.ImeService`と`mInputShown=true`を確認してから撮影した。IME pickerだけ、またはIMEなしの中間画像は証跡に使わない。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
