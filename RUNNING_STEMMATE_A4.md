# Running the STEMMate A4 demonstrator

This guide runs the selected rich Swing client in `client/` with the thin Java SQLite server in `server/`. `prototype/src` is the older Role 5 demo and is not the selected client.

## Prerequisites

- JDK 21 or later. The client compiles for Java 17; the server requires Java 21.
- Maven installed and available as `mvn` in PowerShell.
- The first build needs network access to download Maven dependencies. Later builds can use Maven's local cache.

Run these commands from the repository root: `C:\Users\wykva\IdeaProjects\SDN-A3`.

## 1. Build both applications

Open PowerShell in the repository root:

```powershell
mvn -f server\pom.xml -DskipTests package
mvn -f client\pom.xml -DskipTests package
```

The server executable is `server\target\stemmate-server.jar`. The client can be started through its configured Maven entry point in step 3. `-DskipTests` builds the artifacts without running tests.

If Maven reports an access error for its local repository, close this terminal and reopen PowerShell, then check that `$env:USERPROFILE\.m2\repository` exists and is writable. Avoid setting `MAVEN_OPTS` to a cache under `C:\.m2`.

## 2. Start the server

Open a second PowerShell window at the repository root:

```powershell
$env:STEMMATE_DB_FILE = Join-Path (Get-Location) 'server\stemmate.db'
$env:STEMMATE_PORT = '8080'
java -jar server\target\stemmate-server.jar
```

The server creates its SQLite database and synthetic seed activities at the configured path. Leave this window open while using the client.

In another PowerShell window, check that it is responding:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

Expected response: `status=OK`.

To stop the server, press **Ctrl+C** in its window. Back up `server\stemmate.db` while the server is stopped.

## 3. Start the rich desktop client

Open a third PowerShell window at the repository root:

```powershell
$env:STEMMATE_SERVER_URL = 'http://localhost:8080'
$env:STEMMATE_SQLITE_PATH = Join-Path (Get-Location) 'client\stemmate.db'
mvn -f client\pom.xml exec:java
```

The client database path is optional; without it, the database is stored at `$env:USERPROFILE\.stemmate\stemmate.db`. The client stores plans, drafts, and its sync outbox locally.

## 4. What to try

1. Browse the activity catalogue and try the level, topic, duration, and materials filters.
2. Open the planner, edit a draft, wait for the autosave indicator, then close and reopen the client to check draft restoration.
3. Save a plan locally and inspect its status/outbox. Keep the server window open to observe server activity.
4. Check `GET /health` again after the client is running.

The client’s Online/Offline toggle simulates connectivity for the UI. It does not disable the computer's actual network connection.

## Current sync limitation

The rich client currently sends activity UUIDs, while the thin server validator expects IDs in the `ACT-###` form. As a result, rich-client plan sync can be rejected even though local saving works. Do not treat a local save as a successful server sync. This mismatch must be fixed before using the rich client for an end-to-end sync demonstration.

Phone support remains unfinished. Accessibility, full offline activity reopening, migration against a copy of an existing database, and the selected-client acceptance checks still need verification. The historical restart and dropped-connection results for `prototype/src` are not evidence for the rich client.

## Optional: check an existing database migration safely

Never use your only database copy for a migration check. Stop the server, make a copy, and point the server at that copy:

```powershell
Copy-Item server\stemmate.db "$env:TEMP\stemmate-migration-check.db"
$env:STEMMATE_DB_FILE = "$env:TEMP\stemmate-migration-check.db"
$env:STEMMATE_PORT = '8081'
java -jar server\target\stemmate-server.jar
```

In another window, request `http://localhost:8081/health`. If startup and the health response succeed, stop the server with **Ctrl+C**. Inspect the copied database in a SQLite viewer to confirm legacy plans and their safety/inclusion rows were retained. Remove the temporary copy only after inspection.

## Useful files

- Requirements and implementation status: `docs\requirements.md`, `docs\traceability.md`
- Endpoint contract: `docs\api\endpoints.md`
- Setup/recovery notes: `docs\supportability.md`
- Selected server code and schema: `server\src\main\java\stemmate\server`, `server\src\main\resources\schema.sql`
- Client SQLite schema: `db\sqlite\schema.sql`
