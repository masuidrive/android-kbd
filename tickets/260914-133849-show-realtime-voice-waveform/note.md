# Work Notes: 260914-133849-show-realtime-voice-waveform

## Status: PDH-implement (In progress 2026-09-14 22:41 JST)

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
- [-] PDH-implement: 外部 provider 経由 path は実 API 200 確認済み (deferred の場合は明示記録) - skip: 既存のAndroid端末内SpeechRecognizerだけを使い、外部provider pathはない
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
- `[PDH-open] -> [PDH-ticket-review]` — 既存の端末内音声レイヤーへ、ユーザが明示したリアルタイム波形を追加する単一work unitとして整理した。
- AC読み手は、音声レイヤーへ入って無音・発話・自動再開・候補待ち・取消を順に観察すれば全ACを再生できる。影響surfaceはAndroid音声recognizer、IME service、custom KeyboardView、unit/instrumentation tests、公開mock、native spec、manual。
- 未確定のproduct判断とDependencyはない。既存`認識中`slot内の小表示なので高さを変えない。
- `[PDH-ticket-review] -> [PDH-ticket-human-review]` — ユーザの「ついでにちょっとした波形がリアルタイムで出るとnoizyな環境かどうかわかる」を、上記ACの実装承認として記録した。

## Required Probes
<!-- AC ごとに「達成できると確かめたか」を判定し、確かめていなければ確かめる手段をここへ書く。
     PDH-ticket-human-review の前に実行して結果を書く。
     実行できないもの（実装しないと分からないこと）は、AC に結果を書かず
     「測って記録する＋この値を下回ったら止めて報告する」の形にする。
     この節は close の必須グループ（`require_checklist_groups`）なので、消すと close が止まる。
     途中で要求するときは `./ticket.sh check --require "Required Probes"`。 -->
- [x] Android `RecognitionListener.onRmsChanged(rmsdB)`が現行実装で受信できるが`Unit`へ捨てられていること、既存`voice-status`が6分割最下段の非操作slotであることを実コードで確認した。実端末ごとのRMS範囲は固定保証されないため描画側でclampし、テストで負値・中間値・過大値を測る。

## PDH-implement. 実装ログ
<!-- 1 agent が investigate + implement + tests を 1 session で完遂する。
     実コードを読みながら直接実装し、設計判断 / scope 拡張・縮小の判断 / 実コードで発見した事実をここに append する。
     論理単位ごとの commit hash 一覧も記録する (mega-commit 禁止。commit 数は gate ではない)。 -->
- `[PDH-ticket-human-review] -> [PDH-implement]` — ユーザの明示依頼とRequired Probes完了により実装開始。
- 書く前の仮定: `onRmsChanged`はmain threadのactive recognizer generation中に複数回届くが端末ごとの値域は一定でない。旧recognizerのcallback、候補表示中、取消後、別editor sessionでは描画へ反映してはならない。RMS通知が一度もない端末でも既存の認識状態と入力操作は維持する。
- Android 36 SDKの`RecognitionListener.onRmsChanged` Javadocを確認し、sound level feedback用だがcallback自体と値域には保証がないことを確認した。有限値だけを渡し、描画側で0〜1へclampし、通知前は高さ最小の4本波形を表示する設計を採用した。
- `VoiceRecognitionController`がrecognizer generationを照合したRMSだけを`ImeService`へ通知し、serviceがeditor token、private状態、VOICE layerを再照合して`KeyboardView`へ渡す。Preview、取消、layer/editor/IME切替、errorはlevelをnullへ戻し、無音自動再開は新generationを0から開始する。
- 既存6等分`voice-status` slot内へ「認識中」と4本バーを横並びで描画した。hit target、候補panel、4行の測定値、TalkBack nodeは変更していない。
- 公開mockは実マイクを使わず140ms周期のデモ値で同じ4本波形を動かし、Preview/取消/layer切替でtimerと表示を破棄する。`site/mock.html`と`docs/reference/mock-source.html`はbyte-identical。
- commit `3504748`: controller→service→viewのRMS経路、有限値/generation guard、clamp描画、native regression tests。
- commit `49c3ef2`: browser mock、native spec、technical reference、README、manualの整合。
- focused test: `ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew testDebugUnitTest --tests 'com.masuidrive.gestureime.voice.VoiceRecognitionControllerTest' --tests 'com.masuidrive.gestureime.keyboard.KeyboardViewTest' --tests 'com.masuidrive.gestureime.ImeServiceVoiceHoldTest'` PASS。負値/中間/過大RMS、音量と高さの単調増加、Preview、無音再開、非無音error、取消、旧generation callback、4行高さ不変を確認した。
- code SHA `49c3ef2`で`scripts/test-all.sh --parallel` PASS（fast-checks、Android unit/lint/apk、2/2）。後続のreview修正`b9603a1`でこの証拠は更新し、同SHAで再実行した。`similarity-generic`はPATHに未導入のため重複検出skip（環境制約）。
- mock surface: ローカル`mock.html?voiceDelayMs=1200`をagent-browserで実際に左フリックし、412pxで波形4本が`4.625/6.766/5.438/4px`へ変化、Previewで0本、2番目候補tap後に再び4本、keyboard高さ278px維持、`scrollWidth=innerWidth=412`を確認した。840pxでも高さ278pxと横overflowなしを確認した。スクリーンショットは`/tmp/md-kbd-voice-waveform.png`（一時検証物、commit対象外）。

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
| 1 | AC1 / RMS正規化 | Major | `rmsDb / 10` clampでは負値`-40`と`-10`がともに0になり、入力が大きいほど波形も大きくなる契約を満たさない | 採用・修正済み | 値域保証なしという既知条件に直接関係する。`atan(rms/20)/π+0.5`で全有限値を有界かつ単調に写して24段階へ量子化した |
| 2 | 描画頻度 | Minor | `onRmsChanged`のたびに全Viewをinvalidateすると高頻度callbackで不要な再描画が生じる | 採用・修正済み | 同じ表示段階の通知を捨てれば見た目を変えず負荷を抑えられる。setterの戻り値で同一段階が更新なしであることを固定した |

- before反例: `normalizeVoiceInputLevel(-40f) == 0f`かつ`normalizeVoiceInputLevel(-10f) == 0f`で、異なる負の入力レベルが同じ最小波形になった。
- after: `-40f -> 0.1667`、`-10f -> 0.3333`、`10f -> 0.6667`。`-Float.MAX_VALUE -> 0`から`Float.MAX_VALUE -> 1`まで代表16点が範囲内かつ非減少であることをtestした。quiet/common rangeの描画高もbaseline < -40 < -10 < 10となる。
- 未通知時はraw `0dB`と混同せず`showVoiceInputLevelBaseline()`で表示level 0を設定する。NaNと±Infinityは現在の表示を変えず、Controllerでもserviceへ通知しない。
- 描画量子化は24段階。`-10f`と`-10.1f`のように同じ段階へ入る通知では`setVoiceInputLevel`がfalseを返し、state更新と`invalidate()`を行わない。
- 壊していない側の確認: Preview、無音再開、非無音error、取消の既存状態遷移を含む`ImeServiceVoiceHoldTest`を修正前後でPASS確認し、候補・認識sessionの出力は不変。
- fix commit `b9603a1`: 負値を保持する単調有界変換、未通知baseline分離、24段階量子化、非有限値無視、回帰test。
- focused test: `ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew testDebugUnitTest --tests 'com.masuidrive.gestureime.keyboard.KeyboardViewTest' --tests 'com.masuidrive.gestureime.ImeServiceVoiceHoldTest'` PASS。
- final code SHA `b9603a1`で`scripts/test-all.sh --parallel`を再実行し、fast-checks、Android unit/lint/apkの2/2 PASS。

## Technical reference 更新
<!-- この ticket の差分に因果がある追記・上書きの内容、または「該当なし」＋理由を 1 行以上必ず書く。
     他 ticket 由来の記述を消したくなったら、消さずにここへ削除候補として記録する。 -->
- `technical-reference.md` 10へ、RMS callbackのgeneration/editor guard、表示側clamp、状態別clear、無音再開時の再初期化を追記した。`docs/reference/sites-native-spec.txt`、README、manual、browser mockも同じ表示契約へ揃えた。

## PDH-human-review. 人間レビュー
<!-- agent は PDH-verify まで自動で進め、この stage で人間レビューを依頼する。
     ユーザの明示承認なしに PDH-close へ進まない。
     途中で疑問・判断不能・blocker・完了見込みなしが出た場合は、この stage まで待たずユーザに確認する。 -->

## Discoveries
<!-- 実装中に発見した想定外の事実を記録する。
     例: API の未文書化の挙動、ライブラリの制約、既存コードの隠れた依存関係。
     Implementation で対応した場合は実装ログに合わせて、ticket に書き戻しが必要な場合は PM に flag する。 -->
- `onRmsChanged`は音声入力レベル用途だが、Android APIはcallback頻度・発生・数値範囲を保証しない。したがって波形は環境の相対的な目安であり、騒音判定や数値表示には使わない。

## Open Questions
<!-- 実装中の可逆な迷いと採用した default 値を検出時点で append する
     （運用は pdh-coding「Open Questions protocol」に従う）。 -->

## Resume Point
<!-- 中断時の最終 commit・理由・再開手順を記録する（pdh-coding「中断手順」に従う）。 -->
