# General STEMMate debugging guide

Use this guide for the selected rich Swing client (`client/`) and thin Java server (`server/`). Keep the server and client PowerShell windows open so their messages remain available. Use synthetic data only.

## First checks

1. Confirm the server window is still running.
2. From another PowerShell window, run:

   ```powershell
   Invoke-RestMethod http://localhost:8080/health
   ```

   Expected response: `status=OK`.

3. Confirm the client was started with the expected server URL and database path:

   ```powershell
   $env:STEMMATE_SERVER_URL
   $env:STEMMATE_SQLITE_PATH
   ```

4. Copy the first complete exception and its `Caused by` section. Record the time, action, test ID, and whether the client or server produced the message.

## Build or Maven errors

Run from the repository root. In PowerShell, pass the local repository option with `--%` so it is not split while parsing the Windows path:

```powershell
mvn --% -f client\pom.xml -Dmaven.repo.local=C:\Users\wykva\.m2\repository -DskipTests package
mvn --% -f server\pom.xml -Dmaven.repo.local=C:\Users\wykva\.m2\repository -DskipTests package
```

- `Access is denied` for `C:\.m2\repository`: Maven is using an unwritable cache. Use the explicit user-profile repository shown above.
- `No plugin found for prefix '.repo.local=C'`: PowerShell split the `-D` path argument. Use the exact `--%` command form above.
- Dependency download errors: check network/proxy access to Maven Central separately from source compilation.
- A successful `package` command builds the app; it does not prove acceptance criteria or run tests when `-DskipTests` is used.

## Server will not start

- Check that port 8080 is not already in use. To use another port, set `$env:STEMMATE_PORT = '8081'` and use that port in the client URL.
- Check the server database path in `$env:STEMMATE_DB_FILE` and verify the folder is writable.
- If a schema migration fails, stop the server and preserve the database. Copy it to a separate file and point the server at the copy before investigating migration behavior.
- Do not delete or overwrite `server\stemmate.db` as a first fix. It may contain useful demo data.

## Client will not start

- Confirm the server URL, but note that client UI startup should not require the server to respond.
- Check the SQLite path in `$env:STEMMATE_SQLITE_PATH` and verify its parent directory is writable.
- For `no such table: main.outbox`, verify the client jar was rebuilt after the schema-order fix so the outbox table is created before its index. Rebuild, then rerun the client. The schema uses `IF NOT EXISTS`; a partially initialized database should complete on the next launch.
- `SLF4J: Failed to load class StaticLoggerBinder` is a logging-provider warning. It is not itself a startup failure; check for a later exception.
- Do not remove the client database without first making a copy. It contains drafts, saved plans, and queued sync items.

## Client and server disagree during sync

- Check the client status and outbox before retrying. Preserve the item/plan ID and server response.
- Check the server console for validation details.
- Current known blocker: the rich client sends UUID activity IDs, while the server validator accepts `ACT-###`. A local save can succeed while remote sync is rejected. Do not mark FR5 or QR1 as passed until this is corrected and retested.
- The client Online/Offline switch simulates connectivity in the UI; it does not disconnect the actual network.
- Results from `prototype/src` belong to the legacy demo and do not establish rich-client behavior.

## Safe database and migration troubleshooting

1. Stop the server and client cleanly.
2. Copy the database before inspecting or migrating it:

   ```powershell
   Copy-Item server\stemmate.db "$env:TEMP\stemmate-debug-copy.db"
   ```

3. Set `STEMMATE_DB_FILE` to the copy and use a different server port for the experiment.
4. Start the server and check `/health`; inspect the copied database with a SQLite viewer.
5. Keep the original file untouched until the issue is understood.

The client database is separate. Copy `client\stemmate.db` separately when investigating local drafts or outbox state.

## Evidence to collect

- Exact command used and working directory.
- JDK and Maven versions (`java -version`, `mvn -version`).
- Client/server build revision and app version.
- Complete console error, including nested `Caused by` messages.
- HTTP endpoint/response and whether `/health` succeeded.
- Database file path and whether the issue reproduces on a copy.
- Acceptance test ID, steps taken, expected result, actual result, and evidence path.

Do not include passwords, personal data, or learner-identifying information in logs or evidence. Redact any unrelated private paths or tokens before sharing output.
