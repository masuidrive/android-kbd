# Work Notes: 261012-114749-voice-return-cancel-status

## Status: PDH-review (Implementation and focused checks)

## Checklist
- [x] 左下を前回レイヤーのラベル・タップ復帰・既存フリック切替に変更する。
- [x] 左下の隣へ候補を破棄して継続する「キャンセル」を置き、候補一覧内の旧キャンセル行を除く。
- [x] 準備中・認識中・処理中・候補待ちと波形を選択エリアへ移す。
- [x] nativeとブラウザモックを揃え、スマホ・タブレット幅の操作と高さを検証する。
- [x] 認識中のキャンセルで古い認識を破棄して再開し、callbackの混入を防ぐ。
- [x] 音声状態のTalkBack通知とフォーカスを波形更新中も保つ。
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
- Product Briefの6レイヤー共通操作体系に接続。影響層: Android app、IME service、custom view UI、unit tests、instrumentation/device tests、HTML/CSS mock、docs。Mozc JNIは無変更。
- 現状確認: `ImeService.voiceReturnMode` は既に進入前レイヤーを保持。`KeyboardLayouts`のVOICE最下段はキャンセルと非操作状態slotを使い、`VoicePanelView`は固定高の候補エリアである。したがって左下ラベル・slot差替え・status移設は既存モデル内で実装可能。
- ユーザの明示依頼は前回レイヤー復帰、隣キャンセル、状態の選択エリア表示。AC 2の確定契約では認識中のキャンセルも現在の認識を破棄して再開する。候補一覧内の旧キャンセルを下段2番目へ移す。
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
- [x] 既存の戻り先保持・VOICEの6 slot・固定高panelをコードで確認した。412dp/840dpの実寸と各方向のproduction MotionEventは実装後に測定する。認識中キャンセルはAC 2どおり破棄・再開する。

## PDH-implement. 実装ログ
- browser mock: `site/mock.html` と `docs/reference/mock-source.html` を同一化。ローカル公開構成 `site/index.html#demo` で412px・840pxを実操作し、日本語とQWERTYからの左フリック進入、戻り先ラベル（あん/Az）、左下tap復帰、候補キャンセル後の再認識、固定45px操作行、横はみ出し無しを確認。`node --check` PASS。
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- Android実装commit `415550b`、モック・文書commit `0164bed`。Coding Engineerのfocused unit 134件 PASS、`compileDebugAndroidTestKotlin` PASS。監督側が接続エミュレーターで追加 `KeyboardViewVoicePunctuationTest.voiceReturnAndCandidateCancelUseProductionMotionEventsAtPhoneAndTabletWidths` を実行し1/1 PASS。最初のinstallは/data空き219MBの容量判定で失敗し、テスト中だけstorage thresholdを1%/50MBにして再実行した。終了後に元へ戻す。
- `6971a9f` の `scripts/test-all.sh --parallel --connected` は fast-checks / Android unit・lint・APK / connected の3/3 PASS。ただし独立reviewのCritical/Major修正で再実行が必要。
- review修正commit `6b9c581`: VOICE下段Cancelを認識中にも即時generation破棄・再開。status viewを維持しRMSはinvalidateだけ、Partial更新もstatusをdetachせず、ラベル変更時だけaccessibility announcement。unitに旧callback拒否・フォーカス/通知回数を追加。focused unit PASS。
- mock・文書commit `01cb9b1`: 認識中Cancelの再開、ARIA説明、仕様、マニュアルを一致させた。ブラウザ実操作で認識中Cancel後に旧3候補ではなく次世代の1候補だけが表示されることを確認。412/840画像を目視し、マニュアル画像を新mockへ差替えた。
- 確認待ち: 最終SHAの `scripts/test-all.sh --parallel --connected`、独立review/AC再裏取り、native実音声モデルでの状態遷移・波形/Surface Observer。

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
| 1 | AC 2 / test追従 | Critical | 進行中認識ではキャンセルがinert。認識世代を破棄して再開する必要がある。 | 採用・修正済み、再review待ち | `6b9c581`で世代破棄・再開・旧callback拒否を追加。 |
| 2 | TalkBack / 対称関係 | Major | RMS更新ごとに状態nodeを再生成し、状態の読み上げとフォーカスを失う。 | 採用・修正済み、再review待ち | `6b9c581`で状態view維持と遷移時通知、Partialもdetachせず。 |
| 3 | 色 | Cosmetic | native状態ラベル色がmockより強い。 | 保留 | 上記修正後のUIを観察して判断。 |

## Technical reference 更新
- `technical-reference.md` の音声レイヤー操作・状態表示・波形描画先・候補キャンセル位置を、下段キーと `VoicePanelView` の新構成へ更新した。
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
