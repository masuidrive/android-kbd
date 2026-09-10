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
