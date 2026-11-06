# ADR-002: Selected A4 UI and server implementation

**Status:** Current A4 demonstrator description; does not resolve mobile delivery
**Scope:** Selected rich-client + thin-server A4 slice

## Context

The repo contains a rich Java Swing client with a local SQLite store and a separate Role 5 prototype client with a thin Java server and SQLite. The team selected the thin SQLite server with the rich client so the demonstrator can use the richer planner, draft store, and shared data model. Earlier prototype test evidence is not automatically evidence for the rich client.

## A4 implementation

- Client: Java Swing in `client/src/main/java/org/stemmate`.
- Local queued work: SQLite plan, draft, and outbox tables.
- Server: Java HTTP service in `server/src/main/java/stemmate/server`.
- Server persistence: SQLite, configured by `STEMMATE_DB_FILE`, default `stemmate.db`.
- Protocol: URL-encoded records, one per line; rich client sends batches of 10 to `POST /sync`; server accepts at most 50.

The `prototype/src` client and Ballerina/PostgreSQL path are legacy/alternate. This selection does not assert that the aligned rich client/server is production-ready or fully verified.

## QR3 consequence

Swing is a laptop/desktop interface. Phone delivery remains unfinished. QR3 stays a Must requirement for both device categories and is not met until the same core workflow is implemented and tested on a phone. This ADR does not approve a mobile framework or weaken QR3; the architect must record that technical decision separately.

## Verification evidence

`server/README.md` reports restart-persistence and bad-connection checks for the prototype. Rerun them against the selected rich client before using them as evidence. The selected-client accessibility checks, FR4 end-to-end persistence, and durable full-draft restore still need verification.
