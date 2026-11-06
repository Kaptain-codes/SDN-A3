# STEMMate Namibia – server (SDN621S A4, Role 5)

A small **thin server** for the vertical slice: **save offline → "Waiting to Sync" → sync → "Confirmed Result"**.
The laptop does the work (forms, drafts, the outbox). The server only checks the data, stores it in **SQLite**, and sends back a short answer.

| Decision | Choice | Evidence / reason |
|---|---|---|
| Primary device | Laptop (Java Swing client) | Device-use brief: laptops 92.2% (83/90 teachers), smartphones 73.3%, projectors 70.0% |
| Language | Java 21, client and server | One language for the team |
| Storage | SQLite (one file, `stemmate.db`) | What the A4 brief asks for. No install, no password, works offline |
| Network | Built for weak or unstable connections | See section 4 |

## Files
```
server/src/main/java/stemmate/server/
  Server.java      HTTP endpoints + the line format
  Database.java    all SQLite work (save, read, sync log)
  Validator.java   input checks
server/src/main/resources/
  schema.sql       tables
  seed.sql         synthetic demo activities
src/ (client)
  ServerClient.java  sends the outbox in small batches
  Outbox.java        keeps the outbox in a file on the laptop
```

## 1. Run it
Needs **JDK 21+** and **Maven**. Nothing else; SQLite is inside the jar.
```
cd server
mvn package
java -jar target/stemmate-server.jar
```
Check it: open http://localhost:8080/health. It should show `status=OK`.
The database file `stemmate.db` is created next to where you run it, along with the tables and demo activities.

Optional settings: `STEMMATE_DB_FILE` (default `stemmate.db`), `STEMMATE_PORT` (default `8080`).
Client settings: `STEMMATE_SERVER_URL` (default `http://localhost:8080`), `STEMMATE_DEVICE_ID`, `STEMMATE_OUTBOX_FILE`.

Then run `STEMCraftVerticalSlice` in IntelliJ as usual. To open the server in IntelliJ too, right-click `server/pom.xml` → **Add as Maven Project**.

## 2. Endpoints
Every message is plain text, **one record per line**, written like a URL query: `id=...&action=SAVE_PLAN&title=Wind%20Turbine`.

| Request | Answer |
|---|---|
| `GET /health` | `status=OK` |
| `GET /activities` | one activity per line (FR1) |
| `GET /plans?deviceId=x` | one plan per line (`deviceId` is optional) |
| `POST /sync?deviceId=x` | body: up to 50 outbox items, one per line. Answer: `id=...&status=OK\|REJECTED&message=...` per item (FR5) |

Item actions:
- `SAVE_PLAN`: fields `planId, title, activityId, targetGroup, safety, inclusion, updatedAt`
- `SAVE_OFFLINE`: field `activityId` (FR2)
- `CHECKOUT_KIT`: always REJECTED, because FR7 is deferred

## 3. Checks (Validator.java)
- The title is required, max 120 characters
- Safety controls are required (FR4). *This is my reading of FR4. Check it against the FR4 wording in the requirements.*
- `activityId` is optional, but if it is given it must exist (`ACT-101` style)
- `updatedAt` must be a number (milliseconds)

## 4. Built for a bad connection (put this in the offline/sync strategy and ADR 3)
| Problem | What happens |
|---|---|
| No internet at all | Items stay "Waiting to Sync" in `~/.stemmate/outbox.txt`. Nothing is lost, even if the app closes or the laptop shuts down. |
| Connection drops mid-sync | Items are sent 10 at a time. Batches that got through are confirmed. The rest are retried. |
| Answer lost, client sends again | Each item has an id. The server remembers it in `sync_log` and sends back the same answer without saving twice. |
| Connection keeps failing | The client retries by itself: 15s, then 30s, 60s… up to 5 minutes, then back to 15s once it works. |
| Slow or dead line | Short timeouts (5s to connect, 15s per batch), so the app never freezes. The network runs off the Swing thread. |
| Two edits of the same plan | Last write wins on `updatedAt`. An older edit never overwrites a newer one. |
| Server crash or power cut | SQLite runs in WAL mode with `synchronous = FULL`, so each item is on disk before the answer is sent. |
| Data size | Plain text lines, much smaller than JSON. |

## 5. Tests run (6 Nov 2026)
- **Restart test** (`scripts/restart-test.ps1` on Windows, `restart-test.sh` on Mac/Linux): sync a plan, `kill -9` the server, start it again, and the plan is still there. **PASS**
- **Bad-connection test**: 25 plans sent through a proxy that dropped every second connection and delayed the rest by 1 second. Every one was confirmed after 3 attempts, and each was stored exactly once (25 plans, 25 log rows). **PASS**
- **Outbox file**: items saved to the file and read back unchanged, including titles with `&`, `=` and quotes. **PASS**
- **Validation**: missing title, missing safety, unknown activity and the kit checkout were all REJECTED with a reason. A resent item got the same answer. An older edit was ignored. **PASS**

## 6. Known limits (for the defect log)
- No login yet. Use the server on a trusted local network only.
- One shared database connection, one request at a time. That is fine for a school, not for thousands of users.
- Confirmed items are removed from the outbox file on the next save. They stay visible until the app restarts.
