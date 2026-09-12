# AC verification: 260912-014943-fixed-keyboard-height

- Verification target: `02a36b23b48ea6792263950d579adf88148a7b78`
- Result: **PASS — AC 1–5 VERIFIED**
- AC blockers: none

## Acceptance Criteria

| AC | Result | Implementation evidence | Test / observation evidence |
|---|---|---|---|
| AC 1: choose Small / Standard / Large and persist after restart | **VERIFIED** | `KeyboardHeightPreset` defines 50/55/60dp row pitch in `KeyboardModels.kt:7-9`. `SetupActivity.kt:103-130` renders a vertical RadioGroup with labels 小・標準・大, 48dp minimum targets, current-value selection, and immediate SharedPreferences persistence. `ImePreferences.kt:80-94` reads/writes the preset. `ImeService.kt:95-104`, `131-154` applies it at input-view creation and every editor/view restart. | `SetupActivitySlashCommandsTest.keyboardHeightChoicesDefaultToStandardAndPersistTheSelectedPreset` passes: exact labels, default Standard, ≥48dp targets, select Large, recreate Activity, Large remains selected. `ImeServiceVoiceLifecycleTest.persistedHeightPresetIsAppliedWhenTheInputViewStartsAndReopens` passes for Large→Small lifecycle reload. `setup-height-light.png` visibly shows all three choices and Standard selected below the retained safe-area app bar. |
| AC 2: Standard has the same four-row outer height across phone/tablet widths, Dual on/off, and all layers | **VERIFIED** | `KeyboardView.onMeasure` derives height only from `heightPreset.rowPitchDp × 4 + 8dp + padding` (`KeyboardView.kt:230-241`, `291`); width and Dual only select horizontal geometry at `253-285`. All six `KeyboardMode` values have four rows in `KeyboardLayouts`. Standard is 55dp pitch, 45dp face, 10dp gap, 228dp outer key area. | `KeyboardViewTest.height presets keep every layer and dual kana at a width independent four row height` passes for all presets × 412/840 widths × every mode, plus Dual KANA on/off. It asserts Standard 228dp outer height, 45dp face, and 10dp gap. Phone and 840dp-equivalent captures each show four complete Standard rows with the same vertical geometry; the wide image changes columns only. |
| AC 3: first show, editor switch, Fold-width change, and rotation do not move keys/tap areas away from the selected height | **VERIFIED** | Oversized host measurements are capped to intrinsic preset height while smaller `EXACTLY`/`AT_MOST` constraints are honored (`KeyboardView.kt:230-241`). Size, preset, Dual, mode, and bottom-inset changes cancel gestures, rebuild hit targets, and invalidate accessibility (`114-165`, `217-221`). `ImeService` reloads the saved preset and intrinsic layout at creation/start. Bottom inset contributes only trailing safe area and is excluded from row geometry (`153-158`, `230-240`, `253-259`). | `transient exact parent height cannot move the four row keyboard` passes for five input modes at 500px→228px. `height preset survives transient exact parents and delayed bottom inset without moving keys` passes for all presets and asserts unchanged first-key bounds after inset add/repeat. `smaller parent height constrains every mode and its accessible keys` passes for 160px `AT_MOST` and `EXACTLY`, with every accessible key inside the measured view. Lifecycle reopen coverage passes. Valid API 36 captures cover 412dp, 840dp-equivalent, and rotated/landscape layouts, all with the full four-row keyboard and navigation safe area. |
| AC 4: retain candidate height, gaps, popup/flick behavior, hit/accessibility, and four-row voice layout | **VERIFIED** | Candidate strip remains fixed at 50dp and keyboard remains `WRAP_CONTENT` (`ImeService.kt:95-119`). Row gap remains 10dp and drawing/tap bounds continue to share the rebuilt row geometry (`KeyboardView.kt:253-285`). `GestureThresholds.selectionDp` and popup implementation are unchanged. VOICE remains a four-row `KeyboardLayouts` mode. | Focused `KeyboardViewTest` passes 42/42, including QWERTY geometry, gap-half hit ownership, accessibility gap resolution, spanning Enter/Dual ownership, popup undimmed behavior, flick/gesture actions, dual pointers, and four-row VOICE geometry. `ImeServiceVoiceLifecycleTest.candidateAndVoiceContentDoNotChangeInputViewHeight` passes. All three valid captures visibly retain the 50dp candidate region, four key rows, gaps, and bottom safe area. |
| AC 5: corrupt setting falls back to Standard and IME remains displayable | **VERIFIED** | `ImePreferences.getKeyboardHeightPreset` wraps wrong-type SharedPreferences reads and maps only known enum names, otherwise returning `STANDARD` (`ImePreferences.kt:80-87`). IME creation/start always consumes that safe enum (`ImeService.kt:95-104`, `131-154`). | `ImePreferencesTest.keyboardHeightPresetDefaultsToStandardPersistsAndSafelyRejectsMalformedValues` passes for absent storage, all valid presets, unknown string `too_tall`, and integer type corruption; both corrupt cases return Standard. `ImeServiceVoiceLifecycleTest.inputViewIncludesKeyboardIntrinsicHeightWithoutAnExactParent` passes with a displayable 228dp Standard keyboard and 278dp candidate+keyboard root. |

## Focused and full verification

Fresh Gradle XML at `02a36b2` records:

```text
ImePreferencesTest:              tests=6  skipped=0 failures=0 errors=0
SetupActivitySlashCommandsTest:  tests=4  skipped=0 failures=0 errors=0
KeyboardViewTest:                tests=42 skipped=0 failures=0 errors=0
ImeServiceVoiceLifecycleTest:    tests=9  skipped=0 failures=0 errors=0
Total:                           tests=61 skipped=0 failures=0 errors=0
```

The implementor's exact-target full run records:

```text
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk scripts/test-all.sh --parallel
PASS: fast-checks
PASS: android unit, lint, apk
Passed: 2 / 2
```

`git diff --check b14fd94..02a36b2` passes.

## Surface evidence

- `tmp/keyboard-height-standard-phone.png`: API 36 arm64 AVD, 412dp profile. Candidate area, four complete Standard QWERTY rows, and bottom safe area are visible.
- `tmp/keyboard-height-standard-inner.png`: API 36 arm64 AVD, 840dp-equivalent profile. The same four-row Standard height is retained while horizontal keys widen.
- `tmp/keyboard-height-standard-landscape.png`: API 36 arm64 AVD after rotation/landscape sizing. Candidate area, four complete rows, tap faces, and navigation safe area remain visible.
- `tmp/setup-height-light.png`: current Setup screen retains the prior safe-area app bar and cards and visibly offers 小・標準・大 with Standard selected.

The captures were accepted only after `mSelectedMethodId=com.masuidrive.gestureime/.ImeService` and `mInputShown=true`; IME-picker-only and no-IME intermediate images were excluded. A physical Galaxy Z Fold7 was not available. Fold open/close was therefore represented by the project-required 412dp/840dp AVD width profiles and rotation; hinge behavior and physical multi-touch remain unverified physical-device coverage, not an AC blocker.

## Regression, scope, and documentation

- Setup safe area/app bar is preserved: the height selector is inserted only inside the existing input card; root/app-bar/ScrollView inset code is unchanged. The Setup capture confirms the visible fixed app bar and safe-area placement.
- The ticket adds only local SharedPreferences and view measurement/state. No network, input content, permission, provider, or Mozc path changed.
- Candidate 50dp height, 10dp row gaps, popup, 18dp flick threshold, expanded hit bounds, accessibility virtual nodes, and VOICE four-row layout remain represented in unchanged production paths and passing tests.
- The review's one Minor documentation conflict is resolved by `02a36b2`: `docs/reference/sites-native-spec.txt`, `docs/reference/android-native-implementation.md`, and `docs/reference/mock-source.html` now state that width changes columns/horizontal margins while height comes only from the selected preset.
- README, manual, device verification, canonical references, `site/mock.html`, and `technical-reference.md` decisions 16, 17, and 29 consistently describe preset-fixed four-row geometry, Standard 228dp, external candidate/bottom safe areas, Setup persistence, and corrupt-value fallback.
