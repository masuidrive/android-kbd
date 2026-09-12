# AC verification: 260912-112512-align-candidate-strip-spacing

- Verification target: `5a0fa27f3eee90d85e0783b434e6a2f5b6c7cddb`
- Result: **PASS — AC 1–5 VERIFIED**
- AC blockers: none

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC 1: candidate-to-first-key gap matches row gap | **VERIFIED** | `CandidateStripView.kt:21-26,147-154` places the 34dp face at y=14..48 in the fixed 50dp strip; the first key face begins at root y=58, producing 10dp. `ImeServiceVoiceLifecycleTest.candidateFacesAndFirstKeyFacesShareTheTenDpVerticalGapAtBothWidths` passes at 400/840 widths. API 36 AVD and local browser observation show the same visual rhythm; browser DOM measured 10px in phone and wide modes. |
| AC 2: first candidate uses the normal key outer inset | **VERIFIED** | `applyFaceInsets()` selects 6dp below 600dp and 13dp at/above 600dp using actual view width, matching `KeyboardView` horizontal geometry. Candidate/lifecycle tests assert candidate and first-key left positions at both widths. The AVD capture visibly insets the first face. Browser DOM measured candidate/key left 6px in phone and equal left positions 13px inside the wide keyboard. |
| AC 3: retain candidate gap, scrolling/reset, and long-press deletion | **VERIFIED** | `candidateLayout()` retains a 5dp leading gap. `showCandidates()` resets only when candidate text content changes. `CandidateStripViewTest` passes the 5dp, scroll retain/reset, rendered-token and accepted/rejected long-press cases. `ImeServiceEnglishSuggestionTest.onlyMozcCandidateLongPressDeletesHistoryAndStaleViewsCannotDeleteIt` passes; service source/token/editor guards and `DELETE_CANDIDATE_FROM_HISTORY` route are unchanged. Browser fallback scrolled five candidates to 37px, preserved it for the same list, and reset to 0 after content removal. |
| AC 4: all presentations, themes, and widths share spacing without page overflow | **VERIFIED** | Japanese conversion, prediction, English and slash sources all enter `ImeService.showCandidateStrip()` and the same `CandidateStripView.renderCandidates()` path; VOICE uses the same path with `CandidatePresentation.VOICE`, while partial/status/permission/unavailable use the same root padding and face layout. Tests cover normal/VOICE/status/control at 400/840, Light/Dark palettes, 34dp height and bounded voice faces. Browser phone/wide Light/Dark observations reported document overflow 0. |
| AC 5: public demo and standalone mock match native | **VERIFIED** | `site/mock.html` and `docs/reference/mock-source.html` share `box-sizing:border-box; height:50px; padding:6px 3px 10px`; combined with keyboard outer padding 3px/10px, faces start at 6px/13px, end at absolute y=48px, and sit 10px above first keys. The actual composed `site/demo.html` iframe and standalone `site/mock.html` were exercised through `agent-browser`; both measured 34px faces, 10px vertical gap and aligned phone left 6px, with the composed wide mode aligned at 13px. |

## Test evidence

Fresh Gradle XML at the target SHA records:

```text
CandidateStripViewTest:        tests=23 skipped=0 failures=0 errors=0
ImeServiceVoiceLifecycleTest:  tests=10 skipped=0 failures=0 errors=0
ImeServiceEnglishSuggestionTest: tests=22 skipped=0 failures=0 errors=0
Total inspected:               tests=55 skipped=0 failures=0 errors=0
```

The implementor's exact-target full run records:

```text
PASS: fast-checks
PASS: android unit, lint, apk
Passed: 2 / 2
```

The full suite was not repeated per Director instruction. `git diff --check c950239..5a0fa27` passes.

## Surface evidence and limitations

- `tmp/candidate-spacing-api36.png` is a 1080×2400 API 36 arm64 AVD capture with Gesture IME selected and `mInputShown=true`. It shows real English candidates, the first key row and permission control together. The published manual asset is byte-identical.
- Browser MCP was unavailable. Local `agent-browser` against the repository's composed `site/demo.html` and standalone `site/mock.html` provided runtime DOM evidence instead of a detached renderer. Phone/wide, Light/Dark and overflow were checked; the composed demo also exercised candidate scrolling and reset.
- A physical Galaxy Z Fold7 was unavailable. Wide native geometry was covered by the required 840dp Robolectric/AVD substitute; hinge and physical multi-touch were not observed and are not blockers for this spacing-only AC set.

## Documentation and scope

README, manual, device verification, both canonical native references, saved/public mock CSS and `technical-reference.md` decision 15 consistently specify 10dp vertical spacing, phone 6dp and wide 13dp left alignment. The diff changes candidate/key drawing geometry and matching documentation only; candidate generation, ranking, learning, privacy, external communication, keyboard height presets and voice wrapping remain unchanged.
