# Work Notes: 260911-022705-rebuild-keyboard-visual-fidelity

## Status: PDH-open (Opening)

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
- [x] PDH-implement: `scripts/test-all.sh` 全スイートパス確認済み
- [ ] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録)
- [ ] PDH-implement: ticket の AC / Architectural Invariants / out-of-scope が implementor によって書き換えられていない
- [ ] PDH-review: 確定判断が 1 件ずつ実装に落ちている（対応する実体を名指しできない判断は未実装）
- [ ] PDH-review: 指摘を直すとき、壊していない側の入力を 1 つ選んで前後の出力を記録した
- [x] PDH-review: Directorが採用したCritical/Majorが解消し、非採用findingの分類根拠を記録
- [ ] PDH-verify: AC 裏取り Agent が各 AC の実質達成を verify 済み
- [ ] PDH-verify: Surface Observer 観察済み (純 backend ticket では skip 可、判断を 1 行記録)
- [ ] PDH-verify: ドキュメント更新の要否を確認済み（必要なら `.agents/skills/pdh-update/SKILL.md` or `.claude/skills/pdh-update/SKILL.md`）
- [ ] PDH-verify: technical-reference.md 突合済み（下の「Technical reference 更新」欄に記録）
- [ ] PDH-human-review: ユーザに差分・検証結果・確認手順を提示し、人間レビューを依頼済み
- [ ] PDH-human-review: ユーザが確認手順を実施し、クローズを明示承認した
- [ ] v0.5 APK公開後、英数字候補v0.6より前に、公開demoをDual Flick対応の入力欄＋keyboard中心・可変幅表示へ更新する

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
- [x] 測る対象を洗い出し、書き手が測れるものは測って結果をここへ書いた（412/840dp、font scale 1.0/1.3/2.0、5layer、popup座標、候補背景、last-layer復元を実AVDで確認）

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->

- 正本 `docs/reference/mock-source.html` のstatic CSS/JSをselectorとline単位で抽出し、`docs/visual-reference-analysis.md` に表として記録した。browser computed値とnative比較は別証拠として混同しない。
- 後続overrideとして、かな行高=QWERTY、QWERTY BS=`⌫`、flick中の他key背景減光なし、Dual Flick、layer-key hold音声を維持する。
- Enter/Pasteの元HTMLはnavigation選択でmain labelを置換する一方、通常secondary用 `.flick-selected` transformを付けないため、down-swipe animationの位置関係が揃わないと特定した。nativeではこのbugを再現せず、意図したsecondary animationへ統合する。
- 候補表示時だけcandidate row/未選択labelが`#2a313a`へ変わる実装を確認し、候補欄・未選択候補を既存gap/keyboard背景`#29292c`へ固定した。選択候補のaccentは維持する。
- ユーザーの最新指定で全layer共通切替を左=記号、上=日本語、右=QWERTY、下=テンキーへ変更した。正本HTMLの右=テンキー、下=QWERTYは後続overrideとして採用しない。tap/1秒holdは維持する。
- ユーザーの追加指定により、最後に実際に切り替えた`KeyboardMode`を端末内設定へ保存し、IME再表示・View再作成時に復元する。設定なしまたは不正値はQWERTYとし、modifier・変換state・音声holdは保存しない。Preferences保存とService再作成回帰はfocused testで成功した。
- 色の後続overrideは、過去に共有された実装指定「基本Enter/BSは通常キーと同色、dark指定を外す」「通常keyは灰、modeは暗色」を維持する。今回のユーザー原文として再確認したものではないため、正本HTMLのside-special darkとの差は明示overrideとして扱う。Accent keyは正本HTMLに合わせて通常色にする。

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
| 1 | PopupWindow座標 | Major | `getLocationOnScreen`の絶対YをIME window内の`showAtLocation`へそのまま渡し、IME originを二重加算してpopupが画面外に出る | 採用・解消 | API 36.1実IMEで修正前frame y=3176..3614を確認。`d32e7c1`/`c4f3bf3`でwindow内座標へ補正後、frame `[111,1589][541,2027]`、5tile表示、上`う`選択、releaseで1文字入力、dismissを確認した。 |

### PDH-review-2

- 最終revision `563423f`の独立限定reviewはCritical/Majorなし。last-layer、4方向、held Enter中の他pointer維持、複合label、CandidateStripを確認した。Popup座標修正はrootのdiff reviewと上記実端末before/afterで確認した。
- `scripts/test-all.sh`はfast-check、unit 106件、lint、APK buildに成功。connected 7件も失敗・error・skip 0。APK SHA-256は`fed641ca9f948d856fb00fdb257d8c2d5fe207ca3085016cf4fd4bdf8379acb5`。

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
