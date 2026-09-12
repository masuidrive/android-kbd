# Work Notes: 260912-014204-enable-non-terminal-settings-by-default

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 端末内SharedPreferencesだけの変更で外部provider経路がない
- [x] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [x] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [x] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [x] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer観察済み
- [x] PDH-verify: ドキュメント更新の要否を確認済み
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した

## PDH-ticket-review. Ticket contract check
<!-- 実装前に ticket の契約を確認する。
     Why が product-brief.md に接続しているか、AC が観察可能か、
     Design Decisions / Out-of-scope / Dependencies が実装 agent に十分か、
     Architectural Invariants と矛盾しないか、ユーザ承認が必要な未確定判断が残っていないかを記録する。 -->

- 現行のunset defaultはDual Flick・英字候補・Android個人辞書がfalse、ターミナルカーソルがfalse。前3件だけtrueへ変更し、明示保存値は維持する。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）
  - SharedPreferencesをclearした新規状態でDual Flick・英数字候補・Android個人辞書=true、terminal cursor=falseを確認した。
  - 3設定を明示falseへ保存してSetupActivityを再生成し、3つのswitchがfalseのままになることを確認した。
  - 型不正な保存値は従来の安全側falseへfallbackし、保存値自体を上書きしないことを確認した。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `ImePreferences`の3つの`getBoolean`だけ未保存時fallbackをtrueへ変更した。setter、SharedPreferences key、terminal、slash、last layerには変更なし。
- Setupの英数字候補・Android個人辞書ラベルを既定ONへ更新し、switchのchecked値は引き続き`ImePreferences`をsingle sourceにした。
- manualとtechnical referenceの3設定の初期値説明をONへ更新し、terminal OFFは維持した。
- focused `ImePreferencesTest` / `SetupActivitySlashCommandsTest`は成功した。
- 初回full suiteでは、英字候補OFFを暗黙前提にした`ImeServiceVoiceHoldTest`が新defaultでraw `x`ではなくcomposition経路へ入り1件失敗した。音声cancelを検証するfixtureで英字候補を明示OFFにし、利用者の保存済みfalseを利用する実条件へ固定した。
- 再実行した`scripts/test-all.sh --parallel`はfast-checksとAndroid unit/lint/APK buildが成功した。
- API 36.1 emulatorのアプリdataをclearした新規状態でSetupを開き、Dual Flick・英数字候補・Android個人辞書がON、terminal cursorがOFFであることをLight/Dark双方で確認した。`docs/screenshots/v0.10/setup-{light,dark}.png`とsite assetsを同画面へ更新し、確認後night modeをLightへ復元した。
- 重複検出skip: `similarity-generic`が環境にないため。新規production helperは追加していない。

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
| 1 | 文書整合 | Major | READMEとproduct briefに「明示ONだけ」という旧defaultが残る | 採用・修正 | 新規利用者の初期挙動と矛盾するため、既定ON・設定でOFF可へ更新した |

- 壊していない側の入力: ターミナル向けカーソル操作は未保存時falseのままで、unit testとAPI 36.1 Setup表示の双方でOFFを維持した。
- 再review: README / product brief修正後はAC 1〜4 VERIFIED、残存Critical/Majorなし。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

- decisions 6/13/23のDual Flick・英数字候補・Android個人辞書の既定値をONへ更新した。terminal cursorのOFFは変更していない。
- READMEとproduct briefに残った旧「明示ON」説明も、初期ON・設定でOFF可能な現行仕様へ揃えた。

## PDH-verify. AC裏取り

- AC 1: SharedPreferences clear後の3設定trueをunit testとAPI 36.1 SetupのLight/Dark表示で確認した。
- AC 2: terminal cursorは実装default false、unit test、Setup表示のすべてでOFFを確認した。
- AC 3: 3設定を明示falseにしてSetupActivityをrecreate後もfalseを維持した。
- AC 4: Setup、manual、technical reference、README、product briefの説明を現行defaultへ一致させた。
- Surface Observer: API 36.1の新規状態でLight/Darkとも3設定ON・terminal OFFを確認し、画像を更新した。

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
