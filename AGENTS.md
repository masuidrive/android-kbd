# Project Overview

Read only the context needed for the current task:

- Use `product-brief.md` when changing product behavior, scope, or user-facing decisions.
- Use `technical-reference.md` when changing implementation or architecture.
- Use `PDH-AGENTS.md` and `.agents/skills/pdh-dev/SKILL.md` for PDH ticket work and the applicable stage.
- Read `AGENTS.local.md` only when environment or device setup is relevant and the file exists.

## Directory Structure

```text
app/                             # Android application module and tests
product-brief.md                 # Product purpose and direction
technical-reference.md           # Current implementation: how it works
docs/
  product-delivery-hierarchy.md  # Ticket workflow
tickets/                         # Managed by ticket.sh
  done/
```

## Project Principles

- Prioritize technical correctness over speed.
- Build an Android-native IME for phones, tablets, and foldables. Validate both closed and open Fold layouts; Fold is a supported target, not the product's exclusive target.
- Keep text conversion and input usable offline. Product details that are still marked `[NEEDS CLARIFICATION]` in `product-brief.md` require user confirmation before implementation.
- Use `masuidrive-kbd` as the user-facing product name and `md-kbd` as its abbreviation. Internal package and class names do not need a cosmetic rename.

## Implementation Quality

- Treat `docs/reference/sites-native-spec.txt` and the preserved HTML/CSS as the visual and interaction reference. Inspect them before changing native keyboard UI. When an intentional product decision changes the reference, update the specification, native implementation, browser mock, and user documentation to the same decision.
- Keep `site/mock.html` and `docs/reference/mock-source.html` byte-identical. For behavior shared by native and mock, compare directions, popup geometry, selection thresholds and center return, state retention, and accessibility labels rather than matching only the idle screenshot.
- Make layout mutations in global-layout or attach callbacks idempotent: only replace dimensions or layout parameters when their value changes. Verify first display, editor/app switching, input-view recreation, and repeated application so a temporary measurement cannot become the final keyboard height.

## Project Records

- Append work starts, material findings or blockers, verified milestones, and changes of direction to `progress.md` in JST using exactly `[yyyy/mm/dd HH:MM] 内容。`. Keep request checklists and detailed implementation evidence in the active ticket's `note.md` instead. In final delivery reports, provide the absolute path to `progress.md`.

## Development Environment

### Starting Servers

Use Java 17, Android SDK 36, and Android NDK r29 for Mozc. Use Bazelisk to build Mozc. Run Gradle with `ANDROID_HOME` set to the local Android SDK. Use emulator widths of 412dp and 840dp when a Fold7 device is unavailable, and report emulator evidence separately from physical-device evidence.

### Tests

During implementation, run focused checks for the changed behavior. Before PDH verification or release, run `scripts/test-all.sh --parallel` once on the final SHA; rerun it only after a relevant change, failure, or invalidated result. Add `--connected` when emulator or device behavior is in scope.

For gesture-routing changes, send real `MotionEvent` sequences to an attached production `KeyboardView`. Cover tap, every assigned direction, every unassigned direction, threshold crossing, return to center, cancellation, and long-press repeat behavior when applicable. Direct `KeyAction` calls, renderer tests, browser behavior, and screenshots do not prove the native touch path.

Follow `PDH-AGENTS.md` for completion-report and surface evidence requirements. Test design and ticket-local-test rules are in `.agents/skills/pdh-coding/SKILL.md`.

## Public Release and Site

- Publish Android builds to `masuidrive/android-kbd` GitHub Releases as a directly downloadable `.apk`; do not wrap the APK in a ZIP. Keep APK or ZIP binaries out of the website repository.
- Before calling an APK published, download the release asset again, compare it byte-for-byte with the local artifact, and report its absolute local path, byte size, SHA-256, package, version, supported ABI, and relevant permissions.
- The canonical product page is `https://masuidrive.jp/products/md-kbd/`, hosted from `masuidrive/masuidrive.jp` under `docs/products/md-kbd/`. Develop it in this repository's `site/`; when a sibling checkout is available its usual destination is `../masuidrive.jp/docs/products/md-kbd/`. Before pushing the website, byte-compare `site/index.html`, `site/mock.html`, `site/manual.html`, and `site/styles.css` with the files in the publishing repository. The interactive mock lives on the product page at `#demo`; do not restore a standalone `demo.html`.
- After publishing the site, confirm that GitHub Pages built the exact pushed commit, compare the public core files with that commit, and exercise the public mock at phone and tablet widths. Check actual input gestures, missing assets, and horizontal overflow.

## PDH Ticket Workflow

Shared PDH rules are in `PDH-AGENTS.md`; stage execution and review structure are in `.agents/skills/pdh-dev/SKILL.md`. Keep only project-specific differences here.

Treat `PDH-AGENTS.md`, `.agents/skills/pdh-*`, and generic ticket tooling as upstream PDH distribution files. Do not change them to solve a product ticket. File an issue in `masuidrive/pdh` first for a generic PDH change, and apply upstream updates only through an explicitly requested `pdh-update`; keep project-specific exceptions in this file.

### Affected Layers

List the affected project layers when creating a ticket or planning implementation and tests.

Android app · IME service · custom view UI · Mozc JNI · unit tests · instrumentation/device tests · docs

### Recurring Review Findings

| Category | Common omission | Check |
|---|---|---|
| Rename | Old names remain in imports, mocks, or documentation | Search for the old name with `rg` |
| Gesture routing | A popup or action-level test passes while the production view drops a direction | Run attached-view `MotionEvent` tests for tap, all directions, center return, and no-op paths |
| Native / mock parity | One surface differs in popup, hysteresis, state retention, or accessibility | Exercise both surfaces and compare `site/mock.html` with its preserved source |
| Layout lifecycle | A provisional or repeated layout measurement changes the final height | Test first display, app/editor switch, view recreation, and idempotent listener re-entry at 412dp and 840dp |

## Codex Workers

Keep project-specific role constraints here. Select worker models from the current user instruction and available models; model identity itself is not a quality gate.

When the requested models are available, use Astra for direction or difficult evaluation and use Sol or Terra for implementation and routine work.

| Role | Project-specific constraints and focus |
|---|---|
| Coding Engineer | Kotlin/Android implementation; stop only the affected path when a user-only product or device interaction decision is unresolved, and continue independent approved work. |
| QA Engineer | Run Gradle checks once configured and verify foldable-device behavior on an emulator or physical device when available. |
| Reviewer | Focus on IME lifecycle, privacy/offline behavior, JNI safety, and foldable layouts. |
| AC Verifier | Treat the approved Product Brief, ticket AC, and observed Android behavior as canonical evidence. |
| Surface Observer | Exercise the actual IME on Android; screenshots or a web mock alone do not verify native behavior. |

Escalate to an evaluator when the normal worker repeatedly fails on the same issue or an irreversible design decision needs stronger reasoning. For an evaluation using a different model, launch a separate worker with that model and the same role rules and task context, instead of a custom agent definition that fixes the normal model.

Use a gitignored `AGENTS.local.md` for environment-specific instructions that cannot be committed. Describe how to obtain secrets or where they are stored; do not include their values.

# Based on https://github.com/masuidrive/pdh/blob/15e6289/codex/templates/AGENTS.md
