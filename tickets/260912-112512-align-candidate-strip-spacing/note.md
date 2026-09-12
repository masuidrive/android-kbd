# Work Notes: 260912-112512-align-candidate-strip-spacing

## Note checklist

- [x] Related Work: Product BriefのFold幅でも同じ操作体系を保つ方針、固定4行height ticket、CandidateStripViewと正本/公開mockを確認した。AC 1〜5は復元可能で相互矛盾がなく、比較基準も観察可能である。
- [x] Required Probes: native 400/840 geometry regression、candidate presentation/voice/status/control、candidate scroll resetとlong pressをfocused JVMで確認する。API 36では候補が表示された通常editorを撮影する。HTMLはbrowser MCPが利用可能でないため、現行mockとreference sourceの同一CSSを静的確認し、manualへ同一画面を記録する。
- [x] Implementation: CandidateStripViewへtop14/bottom2と実widthのphone6/wide13 horizontal insetを実装し、50dp stripと34dp face、5dp candidate gapを維持した。
- [x] Attempts: 初回focused testはTextViewの親row基準座標をstrip基準と誤認して失敗した。axis別の親座標合算へ直し、focused JVMとfull suiteをPASSした。
- [ ] Review
- [ ] Verification
- [ ] Human Review

## User statement

- 2026-09-12: 候補欄と最上段キーの間隔をキー同士と同じにし、先頭候補の左側にも余白を設ける。

## Assumptions and measured baseline

- 50dp stripと34dp candidate faceを維持すると、KeyboardViewの最上段face top（strip直後の8dp）へ10dpで接続する候補face bottomはstrip y=48dpとなる。従ってstrip paddingはtop 14dp、bottom 2dpとする。
- 最左key faceはphone幅ではkeyboard outer 3dpとkey horizontal padding 3dpの合計6dp、wide（600dp以上）ではouter 10dpとpadding 3dpの合計13dpである。CandidateStripView自身の実widthでこの水平insetを切替え、候補ScrollViewの先頭・voice/status/controlにも適用する。
- 変更前はCandidateStripView paddingが左右3dp・上下8dpで、candidate face bottomから最上段key face topまで16dp、先頭candidate leftは3dpだった。`KeyboardView.buildHitTargets()`と`CandidateStripView`を読んで確認した。
- `similarity-generic`はリポジトリに存在しないためskipする。外部provider/APIは本ticketに存在しない。

## Implementation evidence

- `2381e37` `[260912-112512-align-candidate-strip-spacing] fix(candidate): align strip faces with key gaps` はCandidateStripViewのpaddingをtop14/bottom2、実widthに応じてphone6/wide13のhorizontal insetへ更新した。400/840幅でSINGLE_LINE、VOICE、status、voice controlのface位置・34dp高、candidate second gap 5dp、root上の最上段keyとの10dp gapを測る回帰を追加した。
- focused JVM: `:app:testDebugUnitTest --tests com.masuidrive.gestureime.ui.CandidateStripViewTest --tests com.masuidrive.gestureime.ImeServiceVoiceLifecycleTest` PASS。full: `scripts/test-all.sh --parallel` PASS（fast-checks、android unit/lint/apk）。
- API 36 `emulator-5554`でGesture IME選択と`mInputShown=true`を確認し、通常editorで英字`a`の候補を表示した。有効画像は`tmp/kana-before-candidate.png`と同内容の`tmp/candidate-spacing-api36.png`だけで、候補face、先頭左余白、最上段keyを同一画面で確認できる。候補がなく「許可」controlだけの旧captureは引用しない。
- browser MCPがこのsessionで利用可能でないため、公開mockの実操作は実行できなかった。`site/mock.html`と`docs/reference/mock-source.html`の同一candidate CSSを静的確認し、manualに有効なnative screenshotを掲載した。
