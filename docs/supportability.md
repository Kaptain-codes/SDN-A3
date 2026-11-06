# A4 selected demonstrator: setup and recovery

This guide covers the selected rich Java Swing client in `client/` and the thin Java server in `server/`. The Role 5 application in `prototype/src` and the Ballerina/PostgreSQL service are legacy prototypes; their test evidence is not evidence for the selected client/server pair.

## Requirements and start

Use JDK 21 or later and Maven. Build the server:

```powershell
cd server
mvn package
java -jar target/stemmate-server.jar
```

Build and launch the rich client from `client/` using its Maven project and Java entry point `org.stemmate.App`. The client defaults to `http://localhost:8080`; configure `STEMMATE_SERVER_URL` or `-Dstemmate.server.url` to change it. The server defaults to port 8080 and `stemmate.db` in its working directory; configure `STEMMATE_DB_FILE` and `STEMMATE_PORT` as needed. The client SQLite path can be set with `STEMMATE_SQLITE_PATH` or `-Dstemmate.sqlite.path`; otherwise it uses `~/.stemmate/stemmate.db`.

Check `GET /health` for `status=OK`. The server creates its schema and synthetic seed data at startup. Back up the stopped server database and client SQLite file together. Do not delete client SQLite while the outbox has unconfirmed work.

## Storage and sync

Client-only SQLite tables contain drafts, outbox records, and local sync status. Shared activity and plan tables use matching definitions on client and server. The server also keeps device associations and idempotent request results. The rich client sends at most 10 plan records per request; the server accepts at most 50 records and a 256 KiB request body.

The client presents waiting, syncing, synced, and retry states. If a request fails, unconfirmed work remains in the client database for retry. Check the client status, retain the plan/item ID and error message, confirm `GET /health`, and retry without deleting the local database.

## Known operational limits

This is a local demonstrator, not a secured multi-user deployment. There is no login, encryption/access-control layer, administrator log interface, or warning on logout with unsynced items. Low-spec performance has no agreed measure yet. Use only synthetic data. Phone implementation and acceptance remain unfinished under QR3.

## Legacy notes

`prototype/src`, `server/service.bal`, `db/postgres/schema.sql`, and the JSON `POST /plans` contract are alternative/legacy artifacts. Do not use their test runs as evidence for the selected rich client without rerunning them against this path.
