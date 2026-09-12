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

## Implementation Quality

<!-- Record recurring project-specific failures here. General implementation rules belong in pdh-coding. -->

## Development Environment

### Starting Servers

Use Java 17, Android SDK 36, and Android NDK r29 for Mozc. Use Bazelisk to build Mozc. Run Gradle with `ANDROID_HOME` set to the local Android SDK. Use emulator widths of 412dp and 840dp when a Fold7 device is unavailable, and report emulator evidence separately from physical-device evidence.

### Tests

During implementation, run focused checks for the changed behavior. Before PDH verification or release, run `scripts/test-all.sh --parallel` once on the final SHA; rerun it only after a relevant change, failure, or invalidated result. Add `--connected` when emulator or device behavior is in scope.

Follow `PDH-AGENTS.md` for completion-report and surface evidence requirements. Test design and ticket-local-test rules are in `.agents/skills/pdh-coding/SKILL.md`.

## PDH Ticket Workflow

Shared PDH rules are in `PDH-AGENTS.md`; stage execution and review structure are in `.agents/skills/pdh-dev/SKILL.md`. Keep only project-specific differences here.

### Affected Layers

List the affected project layers when creating a ticket or planning implementation and tests.

Android app · IME service · custom view UI · Mozc JNI · unit tests · instrumentation/device tests · docs

### Recurring Review Findings

<!-- Adapt these examples to the project. -->

| Category | Common omission | Check |
|---|---|---|
| Rename | Old names remain in imports, mocks, or documentation | Search for the old name with `rg` |

## Codex Workers

Keep project-specific role constraints here. Select worker models from the current user instruction and available models; model identity itself is not a quality gate.

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
