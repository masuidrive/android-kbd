# Tickets Directory

This directory contains all the ticket files for the project.

## Important Guidelines

**⚠️ Always use ticket.sh commands to manage tickets:**

- **Create new tickets:** `ticket.sh new <slug>`
- **Start working on a ticket:** `ticket.sh start <ticket-name>`
- **Complete a ticket:** `ticket.sh close`

**❌ DO NOT manually merge feature branches to the default branch!**
The `ticket.sh close` command handles merging and cleanup automatically.

## Directory Structure

Each ticket lives in its own per-ticket directory:

```
tickets/
  <TICKETNAME>/
    ticket.md   # the ticket body (YAML frontmatter + Markdown)
    note.md     # working notes / log
    tmp/        # ticket-local temp helpers (auto-created; ignored via tickets/.gitignore)
  done/
    <TICKETNAME>/    # closed / canceled tickets, moved here as a whole directory
```

While a ticket is active, three symlinks in the repository root point at it:
`current-ticket/` (directory), `current-ticket.md`, and `current-note.md`.

Legacy compatibility: existing flat tickets (`<TICKETNAME>.md` +
`<TICKETNAME>-note.md`) are still recognized by every command. They are not
migrated automatically.

## Getting Help

For detailed usage instructions, run:
```bash
ticket.sh help
```

For a list of all available commands:
```bash
ticket.sh --help
```
