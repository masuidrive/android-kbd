# Work Notes: 260911-003916-add-english-candidate-buffer

## Status: PDH-implement (Service integration under review)

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
- [ ] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [x] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [x] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
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

- 起票時は将来作業として記録し、`./ticket.sh start`、ブランチ切替、実装、設定追加はいずれも未実行だった。その後のユーザー指示とDirector GOによりv0.6として開始し、現在は実装・レビュー・検証・APKおよび公開資料の準備まで完了している。
- 最終APKはSHA-256 `3411e9e0cf0ef4cc6c91fe593fe7a162bd0618d55d60907da40b5b7ea50be3ca`。新しい公開先への送信だけがユーザー承認待ちであり、ticketのhuman closeも未実施。
- Director contract reviewで、既定OFF、ASCII英数字最大64文字、英字だけのprefixへ端末内固定辞書から最大5候補、数字混在時は候補なし、入力履歴・個人学習なしを確定した。ネットワーク候補を追加しない。
- ユーザー指定の順序変更により、正本HTMLからnative描画を作り直すv0.5の実装・公開後に、本ticketをv0.6として開始する。
- v0.6開始時のDirector案: 設定は既定OFFでterminal cursor設定と独立させる。対象bufferはASCII `[A-Za-z0-9]`、最大64文字。letter-only prefixの辞書候補は最大5件とし、数字を含む場合は候補を出さずraw bufferは維持する。
- caseはtyped prefixを保持してsuffixだけを補完する。全大文字prefixは候補全体を大文字、先頭大文字prefixは候補先頭を大文字にする。Enterはbufferがある時rawだけを確定し、候補tapだけが置換する。日本語候補stateとは分離する。
- punctuation、non-ASCII、layer、modifier、cursor、paste、voice、IME hideはraw確定境界とする。editorが変わった場合は旧bufferを新editorへcommitしない。`SwitchLayer`と`SetModifier`の現行早期return経路で旧bufferを取りこぼさない状態遷移とgeneration guardを着手時に設計する。
- 検証重点は、候補tap後の重複入力なし、queue中の旧候補無効化、Backspaceでのbuffer更新、各境界での取りこぼしなし、日本語復帰、private欄の既存direct入力維持。v0.5のQAとdemo公開完了後、ユーザーの全ticket継続指示に基づくv0.6実装契約として採用した。
ユーザーの全ticket継続指示とDirectorのv0.6 GOに基づき開始した。既定OFF、ASCII英数字最大64文字、最大5候補、固定端末内辞書、非private欄だけという契約を確認した。候補と入力をネットワーク送信せず、日本語Mozc stateと分離する。
- `1057c0c`: 英数字候補設定を既定OFFで永続化し、Setupへtoggleを追加した。
- `3015684`: 28,001語の固定辞書、ライセンス、再現生成scriptを追加した。assetは225,007 bytesで、再生成後のbyte比較が一致した。
- `3b7c5ad`: state-lessなprefix lookup engineとfocused tests 4件を追加した。初回asset loadとlookupは`Dispatchers.IO`で行う。
- `e7708f4`: 候補描画tokenをclick eventへ固定し、再描画前のdetached viewによる古い候補選択を拒否できるUI契約を追加した。
- `e4e5a14`: Serviceへ英数字composition、候補生成のeditor/source/generation/prefix guard、候補確定、入力境界を接続し、InputConnection出力を観察する配線testを追加した。
- `f2d8498`: 64文字到達時にraw確定し、65文字目を新bufferへ開始して続くEnterが改行を入れない境界へ修正した。
- `53ffb3d`: 選択移動時にraw文字を維持してInputConnectionのcomposing spanを解放し、次文字が移動先へ入るよう修正した。
- focused結果: English Service 9件、Voice lifecycle、Preferencesを含む16件が成功。Candidate UI 11件、engine 4件も担当別に成功した。
- `similarity-generic`は環境に導入されていないため実行対象外。辞書再現性は生成scriptのbyte比較とengine testsで確認した。
- 独立reviewは追加Critical/Majorなし。選択逸脱時のInputConnection composing span解放と、65文字目の新buffer開始を限定再確認した。
- API 36.1 AVDで設定ON、native `pro`、候補表示、候補tapによる`problem`への重複なし置換、Enter/Space、数字、Kana境界、private欄、airplane modeを確認した。途中の`proproblem`はGboardで残したraw prefixとGesture IMEの新prefixを混同した無効観測で、fresh editorの同一IME操作で訂正した。
- final: fast 5、unit 121、lint、APK build、connected 7が全て成功。APKは34,965,858 bytes、SHA-256 `3411e9e0cf0ef4cc6c91fe593fe7a162bd0618d55d60907da40b5b7ea50be3ca`。
候補tapを描画token付きeventとして扱う構造へ更新し、英数字buffer、端末内辞書、raw確定境界、private/editor/generation guardをArchitectural Decision 13として追記した。
