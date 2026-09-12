# AC verification: 260912-055128-make-emoji-grid-scrollable

- Verification target: `bc9e8cbd6a6166bc828ded0d02172cf9d92f2f78`
- Result: **PASS — AC 1–5 VERIFIED**
- AC blockers: none

## Acceptance Criteria

| AC | Result | Implementation / test evidence | Surface evidence |
|---|---|---|---|
| AC 1: Emoji opens with three list rows above the fixed control row | **VERIFIED** | `KeyboardLayouts.kt:65-78` takes the first three `emojiContentRows` and appends `emojiControlRow`; `KeyboardView.kt:335-375` clips scrollable hit/draw bounds to a three-row viewport and lays controls separately. Fresh `KeyboardLayoutsTest` and `KeyboardViewTest` pass the three-row/four-row geometry cases. | The cleared API 36 AVD image shows exactly 24 catalog items in three rows plus fixed AZ/delete. Browser phone Light measured viewport 165px, four content rows, 32 buttons, and 24 visible buttons. |
| AC 2: vertical drag continuously scrolls recent + catalog without changing four-row height | **VERIFIED** | `KeyboardView.kt:739-766` starts an `EmojiScrollGesture`, crosses the 12dp threshold, discards the pending key gesture, and updates float offset; `335-366` derives range from actual row pitch and clamps it. Fresh view tests pass normal and constrained-host scroll, drag non-commit, fixed controls, and four-row geometry. | AVD upward drag moved from catalog 1–24 to 9–32, exposing the final eight while the control row and total height stayed fixed. Browser drag moved `scrollTop` 0→110 and left the textarea at its pre-drag single `😀`, proving zero drag commits. |
| AC 3: empty recent fills 24; recent is newest-first, distinct, max eight, then catalog | **VERIFIED** | `KeyboardLayouts.kt:69-73,176` filters empty values, keeps distinct order, takes eight, concatenates the 32-entry catalog, and chunks by eight. `ImePreferencesTest.legacyCursorModeFallsBackToKanaAndEmojiRecentsStayOrderedDistinctAndBounded` covers promotion, duplicate removal and the eight-item cap; service tests gate persistence on a successful current-editor commit. Fresh layout tests cover empty/recent ordering and row widths; the exact-target full suite passed these unchanged service/preference tests. | With app data cleared, AVD showed catalog 1–24. Tapping 🌸 committed it, and reopening Emoji displayed 🌸 as the first item followed by the catalog. Browser began with 32 catalog buttons and, after one 😀 tap, rendered recent 😀 first followed by the catalog. |
| AC 4: pager is absent while tap, delete and bottom-row layer flick remain | **VERIFIED** | Searches find no `ChangeEmojiPage`, `emojiPage`, `emoji-prev`, `emoji-page`, or `emoji-next`. `KeyboardLayouts.emojiControlRow` keeps the layer key and Backspace; `ImeService.kt:306-311` directly commits the emoji and records recent only on accepted current-editor commit. Fresh tests pass catalog tap after scroll and control retention. | Native 🌸 tap committed directly, Backspace removed the test `q`, and AZ upward flick returned from Emoji to Kana. Both native captures have no pager controls. Browser exact pager selector count was 0, 😀 tap changed the textarea once, and drag added nothing. |
| AC 5: native and public mock share the three-row continuous viewport in themes and widths with no horizontal overflow | **VERIFIED** | Native uses the same eight-column `emojiContentRows` and clipped viewport for all widths/themes. `site/mock.html:98-99,602-609,794-800,867-981` creates the corresponding eight-column overflow viewport, fixed controls and pointerup/drag cancellation. `docs/reference/mock-source.html` carries the same behavior. | API 36 AVD observations cover 412dp Light, 412dp Dark and 840dp Light; all show three list rows plus fixed controls contained horizontally. Browser observations cover phone/tablet × Light/Dark; `document`, root and keyboard overflow were 0 in every measured combination, viewport stayed 165px/three rows, and exact pager count stayed 0. |

## Fresh focused verification

At the target commit:

```text
ANDROID_HOME=/Users/masuidrive/Library/Android/sdk ./gradlew \
  :app:testDebugUnitTest --rerun-tasks \
  --tests com.masuidrive.gestureime.keyboard.KeyboardLayoutsTest \
  --tests com.masuidrive.gestureime.keyboard.KeyboardViewTest

BUILD SUCCESSFUL in 5s
29 actionable tasks: 29 executed
```

Fresh XML summaries:

```text
KeyboardLayoutsTest: tests=13 skipped=0 failures=0 errors=0
KeyboardViewTest:    tests=46 skipped=0 failures=0 errors=0
Total:               tests=59 skipped=0 failures=0 errors=0
```

The implementor's exact-target `scripts/test-all.sh --parallel` record remains fresh after the final docs-only commit: `PASS: fast-checks`, `PASS: android unit, lint, apk`, `Passed: 2 / 2`. Per Director instruction, the full suite was not repeated. `git diff --check 8e58f0a..bc9e8cb` passed before this verification-only documentation update.

## Native surface evidence

The current debug APK was built, installed with `adb install -r`, selected as the system IME, and driven through the non-exported test activity on API 36 arm64 `emulator-5554`.

- `emoji-scroll-api36-empty.png`: fresh app data, 412dp Light, catalog 1–24 in three rows and fixed AZ/delete.
- `emoji-scroll-api36-scrolled.png`: after upward drag, catalog 9–32 including the final eight; fixed controls and four-row height remain.
- `emoji-scroll-api36-reopened-recent.png`: 🌸 is first after a successful direct commit and reopening the layer.
- `emoji-scroll-api36-layer-flick.png`: AZ upward flick returns to the Kana layer; the preceding capture also confirms Backspace removed `q`.
- `emoji-scroll-api36-wide.png`: 840dp-equivalent Light width, three rows and fixed controls contained horizontally.
- `emoji-scroll-api36-dark.png`: 412dp Dark, the same rows/controls with readable colors and no clipping.

The empty-recent capture is copied to `site/assets/emoji-scroll-api36-v0.12.png` and referenced by the current manual. The emulator was restored to 1080×2400 Light mode after verification.

## Public mock surface evidence

The repository's actual `site/mock.html` was served at localhost and operated in a real browser. No detached renderer was used.

```text
phone Light, empty recent:
  viewportHeight=165, scrollHeight=220, contentRows=4,
  emojiButtons=32, visibleButtons=24, pagerCount=0,
  document/root/keyboard overflow=0

tap 😀:
  textarea="😀", status="絵文字😀"

upward pointer drag:
  scrollTop=110, textarea="😀" (additional commits=0)

phone Dark / tablet Light / tablet Dark:
  three-row viewport retained, exact pagerCount=0,
  document/root/keyboard overflow=0
```

Temporary viewport overrides were reset after the checks.

## Documentation, invariant and limitations

- `technical-reference.md` decision 17 matches the observed implementation: local newest-first distinct recent max eight, catalog concatenation without an empty row, three-row vertical viewport, fixed layer/delete controls, direct String commit, and legacy `CURSOR`→KANA fallback.
- `site/manual.html` describes the current scroll operation and now includes the inspected API 36 capture. `docs/device-verification.md` records the native journey, device profiles and scope. `docs/reference/mock-source.html` describes and implements the same continuous viewport. The historical v0.11 change log correctly retains the released pager behavior.
- Emoji catalog data remains bundled in the APK and recents remain in private SharedPreferences. The change adds no network or external provider path.
- A physical Galaxy Z Fold7 was unavailable. Fold open/close was represented by the project-required 412dp and 840dp AVD width profiles; hinge and physical multitouch remain device-coverage limitations, not AC blockers.
- TalkBack could not be operated physically. Fresh Robolectric coverage verifies that only visible clipped emoji nodes are exposed, only currently possible forward/backward actions are advertised, available actions move one row, and edge no-ops return false.
