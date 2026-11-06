# A4 repository layout

```text
/
├── client/
│   ├── src/main/java/org/stemmate/  selected rich Java Swing client
│   └── pom.xml                      client build and SQLite dependency
├── db/sqlite/schema.sql             shared tables + client-only tables
├── server/
│   ├── src/main/java/stemmate/server/ thin HTTP server and migrations
│   └── src/main/resources/           shared server schema and seed data
├── prototype/src/                   legacy Role 5 client and file outbox
├── docs/
│   ├── requirements.md              canonical A4 baseline
│   ├── traceability.md               current evidence and gaps
│   ├── architecture/
│   ├── api/endpoints.md
│   └── supportability.md
├── db/postgres/                     legacy Ballerina/PostgreSQL schema
└── server/service.bal                legacy Ballerina service
```

The selected A4 path is the rich `client/` application and its local SQLite database connected to the `server/` thin Java SQLite service. `prototype/src` and the Ballerina/PostgreSQL path are legacy/alternate implementations.
