# ADR-001: Device support requirement

**Status:** Superseded for A4 by the finalized QR3 requirement
**Original decision date/author:** 6 Nov 2026 / Role 3 architect (see original repository history)

## Original decision

The original ADR selected shared laptops/desktops as the primary target and deferred phones. That decision was based on the available teacher device-use evidence and the existing Swing implementation.

## Current A4 requirement

The finalized A4 baseline adds QR3: the FR1–FR6 workflows must have equal Must priority on laptop/desktop and mobile. The laptop-only decision above does not satisfy QR3 and must not be used to downgrade or remove it.

## Current implementation status

The selected A4 demonstrator is laptop-only Java Swing. Mobile delivery and phone acceptance testing are unfinished. QR3 is therefore not met by the current slice. The project must record a new technical decision when the architect selects a mobile delivery approach; until then, this ADR documents the requirement conflict and implementation gap.
