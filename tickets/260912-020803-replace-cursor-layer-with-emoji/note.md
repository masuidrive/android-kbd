# Work Notes: 260912-020803-replace-cursor-layer-with-emoji

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: APK内catalogと端末内SharedPreferencesだけを使い、外部providerを設けないticketである。
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

Why は product brief のオフライン入力と、同じキーボード内で編集・入力を完結する方針に接続する。AC 1–4、6 は画面・入力・再表示で観察できる。AC 5 は旧保存値のfallback、AC 3 はページ方式とrecent件数が未指定だったため、承認済みの次の実装判断で復元可能にした。既存の固定4行、Spaceフリック、直接カーソル操作、端末内保存、外部送信なしと矛盾しない。

### Contract correction

ユーザーのカーソルレイヤー削除指示と既存Space操作維持の会話をDirectorが再確認し、AC 5を「専用カーソルレイヤーは表示経路と保存済み最終レイヤーから削除されるが、Spaceフリックによる上下左右移動と`TextInputController`のcursor key処理は維持」へ訂正した。「既存の直接カーソルキー」は専用CURSORレイヤーのUIを残す意味ではない。このnoteは承認済みcontract correctionの実装記録である。

### Design Decisions

- カテゴリではなく2ページのAPK内emoji catalogを使う。1ページにつき2行のキーを表示し、最下行の前後キーで移動する。
- recentはSharedPreferencesへ新しい順・重複なしで最大8件を保存する。1行目は最大8枠を表示し、足りない枠はemptyのままにして4行外形を保つ。
- 旧`CURSOR`保存値は例外を投げずKANAへfallbackする。新規保存値にはCURSORを書かない。
- 絵文字キーはStringをそのまま`commitText()`し、unicode code point単位へ分割しない。最下行は既存`layerKey`、前頁、頁表示、次頁、Backspaceで構成し、layerKeyの左/上/右/下フリック契約を継続する。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（測る対象が無いなら `- [-] ... - skip: <理由>`）

既存実装を測定した結果、全keyboard modeは`KeyboardView`の4 row pitchを共有し、KANA/NUMBERSの左上`↔`だけがCURSORへ遷移していた。保存された最終modeはenum名をそのまま読むため、CURSOR削除時の明示fallbackが必要である。focused unit testで4行、最大8 recent、ページ、順序・永続化・複数コードポイント、layer flick、legacy fallbackを測り、API 36で実IMEの4行と入力を観察する。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- `bd6f557` `[260912-020803-replace-cursor-layer-with-emoji] feat(keyboard): replace cursor layer with local emoji recents` — `KeyboardMode.CURSOR`をEMOJIへ置換した。KANA/NUMBERSの左上をemoji入口にし、4行をすべて8 width unitへ揃えた。recentは順序を保つ8スロットで保存し、`CommitEmoji`がStringをそのまま確定してrecentを更新する。旧`CURSOR`保存値はKANAへfallbackする。
- `fd0b846` `[260912-020803-replace-cursor-layer-with-emoji] docs(emoji): document local recent emoji layer` — nativeとmockのemoji面、manualのAPI 36 recent証跡、reference mirrorを同期した。
- focused: `ImePreferencesTest`、`KeyboardLayoutsTest`、`KeyboardViewTest` PASS。recentの重複除去・上限・legacy fallback、複数code pointの`❤️`、2ページ、全row幅、右端到達、layer flickとtap dispatchを確認した。
- full: `scripts/test-all.sh --parallel` PASS（fast-checks、Android unit/lint/APK）。

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
| 1 | AC reader | Info | AC 1–4/6は復元可能。AC 3の方式・recent件数とAC 5のlegacy fallbackが未指定。 | 採用 | page方式、recent 8件、CURSOR→KANAをDesign Decisionsへ固定した。 |
| 2 | Review | Major | `CommitEmoji`のservice経路、commit失敗時recent非更新、公開文書の旧5レイヤー表記が未固定。 | 修正 | service integration test、commit成功Boolean、product brief/site/referenceの6レイヤー同期で解消した。 |
| 3 | Review | Minor | technical referenceの5→6とDecision番号重複、layout test名が旧表記。 | 修正 | architectureを6レイヤーへ変更し、17以降を連番化、test名を6 modeへ更新した。 |

### Findings (PDH-review-2)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | Review | Major | `CommitEmoji`の成功判定がeditor sessionとcommit resultを一体で固定しておらず、slashのraw composition cleanupもcoverageにない。 | 修正 | 現editor tokenだけで`commitText()`し、成功時だけrecentと`KeyboardView`を更新する実service testを追加した。日本語・英字・slashのcomposition、commit拒否、旧editor queueを固定した。 |
| 2 | Review | Major | product brief、site、referenceに旧5レイヤー・カーソルレイヤー表記が残る。 | 修正 | product brief、公開mock/manual/index/demo、reference mirrorを絵文字recentを含む現行レイヤーへ同期し、mock sourceにもrecent localStorage永続化を加えた。 |
| 3 | Review | Minor | 公開文言の「面」がユーザーの用語契約に反する。 | 修正 | 音声入力・絵文字の公開文言を「レイヤー」へ統一した。内部の`KeyboardMode`とlegacy `CURSOR` migration値は保持する。 |

- review-2 focused: `ImeServiceEnglishSuggestionTest`、`ImePreferencesTest`、`KeyboardLayoutsTest`、`KeyboardViewTest` — 87 tests PASS。
- review-2 full: `scripts/test-all.sh --parallel` — fast-checks、Android unit/lint/APK PASS。

### Findings (PDH-review-2 / attempt 2)

| # | 観点 | Sev | 要旨 | 判定 | 理由 |
|---|---|---|---|---|---|
| 1 | Review | Major | referenceに専用カーソルレイヤー時代の検証・実装順序と、現行と逆のレイヤーフリック説明が残る。 | 修正 | 両native referenceとsource mirrorを、絵文字レイヤー・Spaceカーソル操作・文字レイヤー/音声入力レイヤーそれぞれの実コードどおりの遷移へ訂正した。 |
| 2 | Review | Major | 中央の`emoji-page`は`ChangeEmojiPage(0)`で見た目だけのno-opだった。 | 修正 | 先頭頁では次、最終頁では前を送るtoggle actionにし、nativeとmockのページ表示キー、TalkBackの次/前説明、layout/view testを同期した。 |
| 3 | Review | Major | `CommitEmoji`のconversion reset中にeditorが切り替わる競合と、かなcomposition直結のcleanupが実サービステストにない。 | 修正 | resetをsuspendするfakeでeditor token再確認を固定し、かなcompositionからの直接emoji確定・recent更新も固定した。 |

- attempt2 counterexamples: page 0の中央`emoji-page`が`ChangeEmojiPage(0)`をdispatchしてcatalogが変わらなかった。旧referenceは左=記号・右=テンキー・下=QWERTYと記述していた。reset待機中にeditor tokenが変わる経路は、reset後の再確認なしなら旧editorへのcommitとrecent更新を許す。
- attempt2 focused: `ImeServiceEnglishSuggestionTest`、`KeyboardLayoutsTest`、`KeyboardViewTest` PASS。
- attempt2 full: `scripts/test-all.sh --parallel` — fast-checks、Android unit/lint/APK PASS。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->

`technical-reference.md`に絵文字レイヤーの8 unit・端末内recent・legacy fallbackを追記し、README、manual、native/mock reference、device verificationをEMOJIへ同期した。manualにはAPI 36のrecent先頭証跡を追加した。review-2ではarchitectureを6レイヤーへ訂正し、Decision番号を連番化した。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->

- emojiのrecent行だけを8 unit、catalogを5 unitのままにすると`KeyboardView`のshared width計算でcatalogが左5/8に縮む。catalogを各16件・8列、navを合計8 unitへ変更して全行を同じ左右端まで伸ばした。
- API 36 AVDではcustom keyboardのvirtual keysがuiautomator treeへ出ないため、通常inputをfocusしadb座標tapで確認した。`emoji-api36.png`は空recentを含む4行、`emoji-api36-recent.png`は😀の直接確定とrecent先頭を同時に示す。有効証跡は後者を使用する。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
