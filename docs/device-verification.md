# Emulator device verification

## Environment

- Date: 2026-09-11 (Asia/Tokyo)
- AVD: `Medium_Phone_API_36.1(AVD)`, Android API 36, arm64
- Serial: `emulator-5554`
- Cover profile: 1080 × 2400 px at 420 dpi, about 412dp wide
- Inner profile: 2205 × 2400 px at 420 dpi, 840dp wide
- APK under final visual verification: `app/build/outputs/apk/debug/app-debug.apk`
- Physical Galaxy Z Fold7: not connected; these results are emulator results

The test activity was opened from `SetupActivity` with **入力を試す**. `ImeTestActivity` was not launched as an exported component. Gesture IME was explicitly enabled and selected after each instrumentation install.

## Setup safe area and app bar (2026-09-12)

The debug APK for `260912-021714-fix-setup-safe-area-and-app-bar` was installed on `emulator-5554` (API 36 arm64). In portrait Light and Dark mode, the UI hierarchy contained **戻る** and **masuidrive-kbd 設定**; the 48dp arrow control and title remained below the status icons, and the four cards began below the fixed bar. `site/assets/setup-safe-area-light-v0.10.png` and `site/assets/setup-safe-area-dark-v0.10.png` are the inspected captures. In Dark landscape, the same fixed bar stayed below the status area; after scrolling to the end, version `0.10.0` and the license button remained above the bottom gesture inset. The landscape captures are retained in the ticket temporary evidence directory. These are emulator observations; a physical Fold7 was not connected.

## Input journeys

| Journey | Emulator operation | Result |
| --- | --- | --- |
| Kana conversion | Entered `にほんご` with Kana flicks | Mozc displayed `日本語`, `ニホンゴ`, `日本語と英語` and other candidates. |
| Candidate cycle and commit | Tapped Space, then Enter | The normal editor contained `日本語`; confirmed in the UI hierarchy. |
| QWERTY variants | Tapped `q`, flicked `w` up, flicked `e` down | The editor contained `qW3`, proving tap, uppercase and secondary input. |
| Half-width Backspace | Flicked the half-width BS key down after `qW3` | The editor contained `qW`. |
| Normal Paste | Copied fixed test text `CLIP`, cleared the editor, flicked Enter down | The normal editor contained `CLIP`. |
| Private Paste | Focused the password editor with the same clipboard, flicked Enter down | The password editor remained empty and the candidate strip was hidden. |
| Cursor layer | Opened the cursor pad | Start/end, four direction keys, Space, Enter and Backspace were visible without clipping. Horizontal movement and boundary behavior are additionally covered by unit tests. |

No user text or personal data was used. The only test strings were fixed values such as `にほんご`, `日本語`, `qW3` and `CLIP`.

## Display and accessibility settings

| Setting | Result | Evidence |
| --- | --- | --- |
| 412dp cover portrait | All five layers fit, with no key outside the screen. | `qwerty-cover.png`, `kana-cover.png`, `numbers-cover.png`, `cursor-cover.png`, `symbols-cover.png` |
| 840dp inner portrait | QWERTY stretched across 2205px without clipping or changing the layer structure. | `qwerty-inner-840dp.png` |
| Cover landscape | IME reopened after rotation; QWERTY fit within 2400 × 1080 px. | `qwerty-landscape-cover.png` |
| Font scale 1.3 | An initial run exposed overlapping Canvas labels. After the density-capped text fix, labels retained their 1.0 layout and remained readable. | `qwerty-font-130-fixed.png` |
| Font scale 2.0 | The key text stayed fitted. Candidate text initially clipped in its fixed 52dp strip; capping it to density rather than scaled density fixed the strip. | `kana-font-200.png`, `kana-font-200-fixed.png` |
| Reduced motion | With all three Android animation scales set to `0`, switching from QWERTY to Symbols completed and no gesture overlay remained. | `reduced-motion-symbols.png` |

After verification the emulator was restored to 1080 × 2400 px, 420 dpi, portrait rotation `0`, font scale `1.0`, and animation scales `1`.

## v0.2 adjustment and Dual Flick verification

Revision `7cf0f3fb189ee121914935dfd127fcdb4be6ec8c` was installed on the same AVD. At 412dp, QWERTY and the single Kana layout displayed without clipping. At 840dp with Dual Flick enabled, the two copies of the central Kana 3×4 keys displayed side by side; the candidate strip, cursor/layer keys, Backspace, Space and the two-row Enter remained single. The tall Enter did not overlap the bottom-row punctuation key.

The following journeys were exercised through the real IME and normal `EditText`:

- Kana flicks entered `にほんご`; Mozc showed candidates and tapping the conversion Enter committed `日本語`.
- Space left and right flicks moved the cursor across `日本語`; the next QWERTY character appeared at the resulting insertion point. Initial testing found that an oversized down flick could emit several DPAD events and move focus to the password field. Revision `7cf0f3f` changed vertical movement to clamped, newline-delimited logical-line movement through `setSelection`; an oversized up/down round trip then kept the normal editor focused. Soft-wrapped visual lines within one paragraph are treated as one logical line.
- The Kana transform key produced `は→ば` with left, `は→ぱ` with right, and `つ→っ` with up.
- Conversion Enter up showed the original hiragana preview and tap committed it. Conversion Enter left showed `ハ` for a `は` reading and tap committed it.
- QWERTY Backspace tap removed the preceding committed `ハ`. Its down-to-ESC dispatch is covered by the consumer unit test because the test `EditText` has no visible ESC behavior.
- The Dual Flick switch remained ON after returning from the test activity, demonstrating persistence. It was then restored to OFF. The emulator width was restored from 840dp to its physical 1080 × 2400 profile.

These are emulator observations. Physical vibration strength and the Galaxy Z Fold7 hardware layout remain unverified because no physical device was connected. Multi-pointer ordering and the 599/600dp layout boundary are covered by `KeyboardViewTest`; physical simultaneous two-hand touch remains a Fold7 probe.

## Mozc latency

## Next-word prediction instrumentation

On `Medium_Phone_API_36.1(AVD)` (API 36, arm64, `emulator-5554`), `MozcConversionEngineTest.bundledDictionaryReturnsAndSubmitsNextWordPrediction` sent `REQUEST_NWP` with preceding text `あけまして` and empty following text to the bundled `mozc.data`. Mozc returned at least one next-word candidate, and `SUBMIT_CANDIDATE` returned a nonblank committed value. `nextWordPredictionCandidateCanBeDeletedFromMozcHistory` also confirmed that `DELETE_CANDIDATE_FROM_HISTORY` was consumed for that prediction state. The connected suite completed 11 instrumentation tests with no failures on 2026-09-12. This verifies the local Mozc protocol path; a real IME tap journey on a Fold7 remains unverified.

`MozcLatencyTest` ran alone in a new instrumentation process. The Mozc data asset had already been copied by earlier tests, so this is a new-process initialization measurement with a warm on-disk asset, not a factory-install asset-copy measurement.

Command:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.masuidrive.gestureime.conversion.MozcLatencyTest
```

Result:

- First `update("か")`: 68.093ms
- Ten growing-prefix updates for fixed reading `きょうはいいてんきだ`: 261.993ms total
- Slowest individual update: 54.369ms
- Individual samples: 4.262, 9.738, 14.316, 29.316, 32.391, 31.969, 24.996, 28.225, 32.412, 54.369ms

This did not show a sustained or multi-second Mozc stall. The Service still serializes actions to preserve input order; the measured longest conversion occupied about 55ms on this AVD.

## Screenshot manifest

All images below were opened and checked against their filenames.

v0.2 evidence:

- `v0.2/setup.png`: Setup screen with the persisted Dual Flick switch, restored OFF
- `v0.2/qwerty-412.png`: 412dp QWERTY with BS label and CSS-aligned spacing
- `v0.2/kana-412.png`: 412dp single Kana layout
- `v0.2/dual-kana-840.png`: 840dp Dual Flick Kana layout
- `v0.2/kana-candidates-840.png`: real `にほんご` composition, Mozc candidates and conversion Enter labeled `確定`
- `v0.2/unconverted-preview-840.png`: conversion Enter up preview
- `v0.2/katakana-preview-840.png`: conversion Enter left Katakana preview

- `setup.png`: Setup and activation controls
- `qwerty-cover.png`: cover-width QWERTY layer
- `kana-cover.png`: cover-width Kana layer
- `kana-candidates-cover.png`: `にほんご` with real Mozc candidates
- `kana-committed-cover.png`: committed `日本語`
- `numbers-cover.png`: number layer
- `cursor-cover.png`: cursor layer
- `symbols-cover.png`: symbol layer
- `private-field-cover.png`: private editor with candidate strip hidden
- `qwerty-inner-840dp.png`: 840dp inner-width QWERTY
- `qwerty-landscape-cover.png`: rotated cover QWERTY
- `qwerty-font-130-fixed.png`: readable QWERTY at font scale 1.3
- `kana-font-200.png`: candidate strip clipping found at font scale 2.0
- `kana-font-200-fixed.png`: fitted candidate strip at font scale 2.0
- `reduced-motion-symbols.png`: Symbols layer with animation scales disabled

## v0.3 on-device voice input

Revision `6ec20cc` and APK SHA-256 `4e691b4003dcdc883de55dd812b27fd7fba3061b408867d561ac63b821fa2cea` were verified on the API 36 AVD. The on-device recognizer factory was available, but `installedOnDeviceLanguages` was empty; Japanese speech recognition itself could not be exercised on this emulator.

- Without `RECORD_AUDIO`, the candidate strip retained normal candidates and displayed the `許可` control. Permission was not bypassed and recording did not begin.
- After granting `RECORD_AUDIO`, hiding and reopening the same input view restored the `音声` control. Granting permission did not automatically start recording.
- Starting voice input performed the Japanese model check and changed the control to `非対応`; it did not use a network recognizer or download a model.
- In the password editor, the entire candidate and voice strip was hidden.
- Ordinary QWERTY and Kana input remained usable. Candidate preservation with both permission-required and unavailable states is also covered by `CandidateStripViewTest`.
- Closing and reopening the input view, stale callback rejection, explicit stop/cancel, preview confirmation, editor switching, and recognizer exceptions are covered by lifecycle/controller unit tests. Actual recording, preview text, and confirmation were not claimed on this model-less AVD.

v0.3 evidence:

- `v0.3/voice-permission-required.png`: no microphone permission, normal IME retained
- `v0.3/voice-permission-with-candidates.png`: Mozc candidates retained beside the permission control
- `v0.3/voice-permission-granted.png`: permission granted and input view reopened with idle voice control
- `v0.3/voice-model-unavailable.png`: Japanese model check completed with `非対応`
- `v0.3/voice-private-hidden.png`: password editor with candidate and voice strip hidden

## v0.4 emulator observations

- The QWERTY adjustment screen rendered the real `KeyboardView` at its 412 and 840 preview widths. Changing the primary-letter scale from 1.00 to 1.17 updated the preview immediately. Leaving and reopening the Activity retained 1.17; “初期値に戻して保存” restored 1.00. Evidence is under `docs/screenshots/v0.4/`.
- With terminal-compatible cursor mode enabled, Android Chrome and the local xterm.js fixture received Home (`keyCode=36`, `ESC[H`) and End (`keyCode=35`, `ESC[F`). A Space left gesture followed by `x` changed `insert-here` to `inxsert-here`, proving an interior terminal-cursor insertion. Up, down, left, and right key-pair generation is covered by unit tests; all four were not separately retained as Android browser screenshots. This mode remains default OFF and must be restored OFF after the probe.
- `docs/screenshots/v0.4-xterm-all-cursor-log.png` is the retained Home/End terminal log. Its filename reflects the intended probe set; it is not evidence that every direction was observed on screen.

## v0.5 visual fidelity verification

The Android Chrome reference captures used the earlier API 36 AVD; final native captures used the isolated API 36.1 AVD. Both used the same 1080px/420dpi 412dp profile (and the same 2205px override for 840dp). DOM geometry remains the numeric source of truth; Android captures are used to compare platform-font baselines, shadows, and visible spacing. The preserved HTML Kana height is 51px and is intentionally superseded by the later requirement that native Kana and QWERTY key heights match.

| Width | Font scale | Reference/native evidence | Result |
|---|---:|---|---|
| 412dp | 1.0 | `v0.5/html-qwerty-412-dark.png`, `v0.5/native-qwerty-412.png` | Key faces, shadows, labels, BS and navigation-bar inset visible without clipping. |
| 412dp | 1.3 | `v0.5/native-qwerty-412-font130.png` | All glyphs remained inside their key faces. |
| 412dp | 2.0 | `v0.5/native-qwerty-412-font200.png` | All glyphs remained inside their key faces; the existing keyboard font cap kept key glyph sizes unchanged. |
| 840dp | 1.0 | `v0.5/html-qwerty-840.png`, `v0.5/native-qwerty-840.png` | Wide QWERTY fit without clipping. |
| 840dp | 1.3 | `v0.5/native-qwerty-840-font130.png` | All glyphs remained inside their key faces. |
| 840dp | 2.0 | `v0.5/native-qwerty-840-font200.png` | All glyphs remained inside their key faces. |

The final APK at revision `563423f` is 34,859,489 bytes with SHA-256 `fed641ca9f948d856fb00fdb257d8c2d5fe207ca3085016cf4fd4bdf8379acb5`. The local suite reported 106 unit tests and 7 connected tests, with zero failures, errors, or skips; raw logs are `docs/verification/v0.5-test-all.log` and `docs/verification/v0.5-connected.log`.

At 412dp, QWERTY, Kana, Numbers, Symbols and Cursor screenshots were opened and matched their filenames. Kana candidates used the same `#29292c` strip background as the empty state while retaining the selected-candidate accent. At 840dp, QWERTY and the two-copy Dual Kana layout fit without overlap. The fixed popup path was exercised on the API 36.1 AVD: an up flick on `あ` displayed all five tiles with `う` selected, release inserted one `う`, and the window dismissed. Its corrected window frame was `[111,1589][541,2027]`; the earlier faulty build placed it at y=3176..3614.

Symbols persisted after hiding and reopening the IME and after focusing the second editor; the stored layer was then restored to QWERTY. The emulator was finally restored to physical 1080 × 2400 px, 420 dpi, font scale 1.0, light mode, Dual Flick OFF, terminal mode OFF, QWERTY, and Gesture IME selected. `native-kana-popup-412.png` and `native-qwerty-412-wip.png` were invalid intermediate captures (popup absent / IME absent) and are excluded from evidence.

## v0.6 English suggestions on API 36.1

The final APK SHA `3411e9e0cf0ef4cc6c91fe593fe7a162bd0618d55d60907da40b5b7ea50be3ca` was installed on `emulator-5556`. All values below were fixed QA input entered through the native keyboard.

- With the setting ON, `pro` remained composing and displayed `problem`, `probably`, `program`, and `process`. Tapping the first rendered candidate changed a fresh empty editor to exactly `problem`; `docs/verification/v0.6-candidate-tap.xml` preserves that value. An earlier `proproblem` observation mixed a Gboard-entered raw `pro` with a second Gesture IME prefix and was discarded as invalid evidence.
- `hi` followed by Enter produced exactly `hi`, without a newline. `hi` followed by Space produced `hi `.
- ASCII digits entered from the Symbols number row stayed raw, showed no dictionary candidates, and Enter added no newline.
- Switching an English prefix to Kana removed English candidates and preserved the raw prefix.
- The private editor hid the candidate strip while direct input continued.
- Airplane mode still produced the same bundled candidates for `pro`; airplane mode was then disabled.

After verification, Dual Flick, terminal cursor, and English suggestions were OFF. The display remained 1080 × 2400 px at 420 dpi, QWERTY was restored as the last layer, and Gesture IME remained selected.

## Candidate-strip spacing verification on API 36

The final ticket APK was installed on `emulator-5554` at 1080 × 2400 px / 420 dpi. With Gesture IME selected and `mInputShown=true`, the normal editor displayed English candidates. The capture visibly shows the first candidate inset from the left edge, the 5dp candidate gap, and the 10dp rhythm between candidate faces and the first key row. Evidence: `tickets/260912-112512-align-candidate-strip-spacing/tmp/kana-before-candidate.png`.

## Keyboard-height verification on API 36

The ticket build was installed on `emulator-5554` (`sdk_gphone64_arm64`, API 36). With Gesture IME explicitly selected and `show_ime_with_hard_keyboard=1`, the normal `EditText` displayed the 50dp candidate strip, four complete QWERTY rows, and the navigation safe area at the 1080 × 2400 px / 420 dpi (412dp) profile. Evidence: `tickets/260912-014943-fixed-keyboard-height/tmp/keyboard-height-standard-phone.png`.

The same focused editor was then measured at a 2205 × 2400 px override (840dp equivalent) and after landscape rotation. The wider capture has the same four row faces while its columns expand; the landscape capture still shows all four rows and the bottom navigation safe area. Evidence: `tickets/260912-014943-fixed-keyboard-height/tmp/keyboard-height-standard-inner.png` and `tickets/260912-014943-fixed-keyboard-height/tmp/keyboard-height-standard-landscape.png`.

The emulator was restored to 1080 × 2400 px, portrait rotation `0`, and `show_ime_with_hard_keyboard=0` after capture. This is emulator evidence; Fold7 hardware, hinge transitions, and physical multi-touch remain unverified.

## v0.7 Light/Dark theme on API 36.1

The final version code 7 APK SHA is `e0b58e00fd4b73c49d94bdceb2d7f7fcaa9e1f2d0d5679684b31b7a229259847`. It was installed on `emulator-5556` at 1080 × 2400 px / 420 dpi.

- Android Light mode rendered the keyboard background, regular and special keys, labels, candidate area, and selected control in the Light palette. Evidence: `docs/verification/light-mode-keyboard.png`.
- Android Dark mode rendered the same view with the existing Dark palette. Evidence: `docs/verification/dark-mode-keyboard.png`.
- The QWERTY label adjustment Activity used the same Light `KeyboardView` for its live preview. Evidence: `docs/verification/light-mode-settings-preview.png`.
- Switching the system theme caused Android to recreate the focused Activity and hide the IME; refocusing the editor showed Gesture IME in the new theme. The connected suite still passed 7 tests.

The AVD was restored to Light mode after verification. Gesture IME remained selected.
