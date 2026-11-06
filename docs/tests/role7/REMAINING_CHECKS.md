# Remaining checks

## M01 Live Java server SQLite restart

Requirements: FR5, QR1. Status: BLOCKED HERE.

Steps: Start the Java server with its SQLite JDBC dependency. Save a synthetic plan through the selected client, stop and restart the server, then verify the same row and sync-log entry.

Reason: The SQLite JDBC jar and Maven are not present in this execution environment. Server Java sources compiled, but database operations and live sockets were not executed.

After execution record the actual date, executor, build hash, result and evidence. No pass is currently claimed.

## M02 Keyboard-only complete workflow

Requirements: AC1. Status: NOT RUN.

Steps: In the final GUI use Tab, Shift+Tab, arrows and Enter/Space to find an activity, complete a plan, correct validation errors, save and inspect sync.

Reason: The Swing GUI was not launched or operated in this run.

After execution record the actual date, executor, build hash, result and evidence. No pass is currently claimed.

## M03 Screen reader field names and status feedback

Requirements: AC1. Status: NOT RUN.

Steps: Use an actual screen reader to check form names, focus changes, validation and sync-status announcements.

Reason: Static label checks do not prove assistive-technology usability.

After execution record the actual date, executor, build hash, result and evidence. No pass is currently claimed.

## M04 Small display and text scaling

Requirements: AC1, QR2. Status: NOT RUN.

Steps: Inspect all final GUI screens on the agreed target screen size and increased OS text scaling. Record clipped text and inaccessible controls.

Reason: No target-device visual inspection or full GUI build was performed. FlatLaf is not available in this environment.

After execution record the actual date, executor, build hash, result and evidence. No pass is currently claimed.

## M05 Live interrupted network and retry

Requirements: FR5, QR1, QR2. Status: NOT RUN.

Steps: Use the agreed client/server pair, interrupt a real upload, reconnect and verify the server row, local state and absence of duplicates.

Reason: Remote and HTTP boundary doubles were used in automated client tests; live end-to-end networking was not executed.

After execution record the actual date, executor, build hash, result and evidence. No pass is currently claimed.

## M06 Unsynced work warning on exit

Requirements: QR2. Status: NOT RUN.

Steps: Close the final GUI with queued changes and verify a clear warning and safe retention of the pending records.

Reason: GUI-level exit behaviour was not exercised.

After execution record the actual date, executor, build hash, result and evidence. No pass is currently claimed.
