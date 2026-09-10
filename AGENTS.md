# Project Overview

Read `product-brief.md` and `PDH-AGENTS.md`. If `AGENTS.local.md` exists, also read its environment-specific context.

## Directory Structure

```text
app/                             # Android application module (implementation pending)
product-brief.md                 # Product purpose and direction
technical-reference.md           # Current implementation: how it works
docs/
  product-delivery-hierarchy.md  # Ticket workflow
tickets/                         # Managed by ticket.sh
  done/
```

## Project Principles

- Prioritize technical correctness over speed.
- Build an Android-native IME for Galaxy Z Fold7; do not substitute the reference web UI for the native product.
- Keep text conversion and input usable offline. Product details that are still marked `[NEEDS CLARIFICATION]` in `product-brief.md` require user confirmation before implementation.
- If a user message ends with two question marks (`??` or `？？`), answer the question only; do not edit files or execute commands.

## Implementation Quality

<!-- Record recurring project-specific failures here. General implementation rules belong in pdh-coding. -->

## Development Environment

### Starting Servers

Use Java 17, Android SDK 36, and Android NDK r29 for Mozc. Use Bazelisk to build Mozc. Run Gradle with `ANDROID_HOME` set to the local Android SDK. Use emulator widths of 412dp and 840dp when a Fold7 device is unavailable, and report emulator evidence separately from physical-device evidence.

### Tests

Until the Android project is scaffolded, `scripts/test-all.sh` runs only the PDH structural checks. Add Gradle unit, instrumentation, and lint commands with the scaffold.

Run the full suite through `scripts/test-all.sh`. Use `--parallel` for parallel execution.

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
| DB migration | Schema changes have no migration | Plan the schema change and migration together |

## Codex Workers

Configure the model and reasoning-effort override examples in `.codex/agents/pdh-*.toml` for the project's roles. Keep project-specific constraints here.

| Role | Project-specific constraints and focus |
|---|---|
| Coding Engineer | Kotlin/Android implementation; stop on unresolved product or device interaction decisions. |
| QA Engineer | Run Gradle checks once configured and verify foldable-device behavior on an emulator or physical device when available. |
| Reviewer | Focus on IME lifecycle, privacy/offline behavior, JNI safety, and foldable layouts. |
| AC Verifier | Treat the approved Product Brief, ticket AC, and observed Android behavior as canonical evidence. |
| Surface Observer | Exercise the actual IME on Android; screenshots or a web mock alone do not verify native behavior. |

Escalate to an evaluator when the normal worker repeatedly fails on the same issue or an irreversible design decision needs stronger reasoning. For an evaluation using a different model, launch a separate worker with that model and the same role rules and task context, instead of a custom agent definition that fixes the normal model.

Use a gitignored `AGENTS.local.md` for environment-specific instructions that cannot be committed. Describe how to obtain secrets or where they are stored; do not include their values.

# Based on https://github.com/masuidrive/pdh/blob/15e6289/codex/templates/AGENTS.md
