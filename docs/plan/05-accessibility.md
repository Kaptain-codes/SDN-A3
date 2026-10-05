# Accessibility plan

This checklist targets WCAG 2.2 AA behaviors in the Swing client.

| Check | Implementation | Test |
|---|---|---|
| Keyboard access | Give every control a predictable focus order and keyboard mnemonic; do not require pointer gestures. | Complete each workflow with keyboard only. |
| Visible focus | Use a high-contrast focus border and never remove Swing focus indicators. | Tab through every screen and inspect focus. |
| Labels and instructions | Pair every input with a visible label and describe required fields and formats. | Screen-reader/accessibility inspector review and form walkthrough. |
| Contrast | Use tested text/background pairs and a non-colour status label. | Measure rendered colours and inspect status in grayscale. |
| Text resizing | Use layout managers and allow the window to resize without clipping. | Increase system font and resize to a small and large window. |
| Error guidance | Put validation and sync errors beside the affected control with a recovery action. | Trigger each validation and simulated sync failure. |
| Status updates | Announce waiting, syncing, synced, and failed-retry states as text. | Observe status changes without relying on colour. |
| Timing | Do not expire drafts or status messages without user control. | Leave each workflow idle and verify work remains available. |
| Tables and lists | Provide accessible names, row descriptions, and selection feedback. | Navigate catalogue and outbox with keyboard and inspector. |

## Issues found and fixes made

| ID | date | issue | WCAG criterion | fix | status |
|---|---|---|---|---|---|
| AC1-1 | 2026-10-04 | Activity list used record output with no accessible name or selection feedback | 2.1.1, 4.1.2 | Added keyboard single-selection behavior, accessible names/descriptions, and readable activity cell summaries | implemented |
| AC1-2 | 2026-10-04 | Plan builder input and save action had no mnemonic or inline validation message | 2.1.1, 3.3.1, 4.1.2 | Added label association, mnemonics, accessible descriptions, and text validation feedback | implemented |
| AC1-3 | 2026-10-04 | Activity details and sync status lacked explicit accessible names | 1.3.1, 4.1.2 | Added accessible names/descriptions and text-only status announcements | implemented |
