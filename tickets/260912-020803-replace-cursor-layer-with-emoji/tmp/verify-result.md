# AC verification: 260912-020803-replace-cursor-layer-with-emoji

- Verification target: `b4f3d762ecf1cb354e0280d307d7d567fc5fd080`
- Contract correction included: `48bc46e`
- Result: **PASS — AC 1–6 VERIFIED**
- AC blockers: none

## Acceptance Criteria

| AC | Result | Implementation and test evidence | Surface evidence |
|---|---|---|---|
| AC 1: KANA/NUMBERS entry opens a fixed four-row Emoji layer | **VERIFIED** | `KeyboardMode.EMOJI` replaces CURSOR; `KeyboardLayouts.kt:42,58,64-80,131` maps both left-top keys to Emoji and builds exactly four 8-unit rows. Layout/View tests cover entry actions, all row widths and the Standard 228dp four-row height. | API 36 captures show KANA's ☺ entry and the complete four-row Emoji surface. The composed browser demo entered Emoji by tapping the left-top key from both Japanese and Numbers. |
| AC 2: local newest-first distinct recents are directly tappable | **VERIFIED** | `ImePreferences.kt:115-127` stores eight SharedPreferences slots, removes duplicates and promotes the tapped String. `KeyboardLayouts.kt:66-67,133-135` creates recent `CommitEmoji` keys. Preference, View and service tests cover ordering, cap, multi-code-point `❤️`, direct action and commit-success gating. | The AVD image shows 😀 in the editor and recent slot 1. In the demo, tapping ☕ and 💯 directly appended them, then tapping recent ☕ produced value `☕💯☕` and recent order `☕, 💯` without duplication. |
| AC 3: catalog is paged within unchanged keyboard height | **VERIFIED** | `EmojiCatalog` has two 16-item pages; Emoji rows are recent + 8 + 8 + navigation. `emojiPageToggle()` advances on page 1 and returns on page 2; `KeyboardView.changeEmojiPage()` clamps bounds. Tests assert four rows, 8 units per row, second-page contents, clickable accessibility description and 228dp height. | API 36 shows recent/catalog/catalog/navigation within the normal IME height. Browser page-indicator tap changed 1/2 to 2/2 and displayed the second catalog beginning 💯; row count stayed four and overflow was zero. |
| AC 4: successful input promotes recents and survives IME redisplay | **VERIFIED** | `ImeService.kt:101-103,132-156` reloads recents on create/start/start-view; `306-311` records only after a current editor accepts commit. Service tests cover Japanese/English/slash cleanup, accepted/rejected commit, stale queued editor and editor change during suspended reset. Preference tests prove persistent order. | AVD shows the committed emoji and recent together. Browser reload preserved Emoji mode, page 2 and recent order `☕, 💯`. The AVD image predates only the page-indicator action fix, which does not affect its direct-input/recent observation. |
| AC 5: dedicated Cursor layer is gone while Space cursor movement remains | **VERIFIED** | `KeyboardMode` has KANA, NUMBERS, EMOJI, QWERTY, SYMBOLS, VOICE and no CURSOR. `ImePreferences.getLastKeyboardMode()` maps legacy stored `CURSOR` to KANA; all new saves accept only current enum values. `KeyboardLayouts.space()` still maps left/up/right/down to `MoveCursor`; existing gesture tests pass and no obsolete UI/state path remains. | Browser Space left flick changed caret from 2 to 0 while keeping value `ab`; dedicated Cursor is absent from the UI. |
| AC 6: Emoji layer flicks reach Japanese/QWERTY/Numbers/Voice | **VERIFIED** | `KeyboardLayouts.layerKey()` on the Emoji navigation row maps left=`VoiceHold`, up=KANA, right=QWERTY, down=NUMBERS. Layout tests assert all four actions; shared gesture/voice tests pass. | Actual browser pointer flicks from Emoji produced mode labels 日本語, QWERTY, テンキー and 音声入力 for up/right/down/left respectively. |

## Test evidence

Fresh Gradle XML at the target SHA records:

```text
ImePreferencesTest:                tests=7  skipped=0 failures=0 errors=0
ImeServiceEnglishSuggestionTest:  tests=25 skipped=0 failures=0 errors=0
KeyboardLayoutsTest:              tests=13 skipped=0 failures=0 errors=0
KeyboardViewTest:                 tests=44 skipped=0 failures=0 errors=0
Total:                            tests=89 skipped=0 failures=0 errors=0
```

The implementor's exact-code-state full run records fast-checks and Android unit/lint/APK as PASS. The full suite was not repeated. `git diff --check 220480b..b4f3d76` passes.

## Surface evidence and freshness

- `tmp/emoji-api36-recent.png`: 1080×2400 API 36 arm64 AVD with Gesture IME selected. It shows 😀 committed in the normal editor and the same value in recent slot 1, plus the complete Emoji keyboard and safe area.
- `tmp/emoji-api36.png`: empty-recent four-row surface; useful only as supporting layout evidence.
- `tmp/emoji-kana.png` and `tmp/emoji-qwerty.png`: supporting entry/return screenshots.
- The final `b4f3d76` changed the page indicator's action and accessibility description without changing the captured rendering, direct commit or recent behavior. The images remain valid for those unchanged observations; the indicator behavior is verified by fresh target-SHA unit tests and browser interaction.
- The repository's composed `site/demo.html` was served locally and driven with `agent-browser`, including recent promotion/reload, two pages, page-indicator tap, all four layer flicks, four-row layout, overflow and Space cursor movement.
- A physical Galaxy Z Fold7 was unavailable. Hinge/open-close and physical multi-touch remain unobserved physical-device coverage; they do not block these ACs.

## Documentation and scope

README, Product Brief, site index/manual/demo/mock, both canonical native references, HTML mock reference and `technical-reference.md` consistently describe the six layers, fixed four-row Emoji geometry, two catalog pages, local recents and Space cursor movement. Searches found no current `KeyboardMode.CURSOR`, dedicated cursor-layer wording or mock cursor state. The legacy string `CURSOR` remains solely as the required migration input. Emoji data stays in the APK and private SharedPreferences; no network, Mozc learning, search, variation picker, GIF or external history path was added.
