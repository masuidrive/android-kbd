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
