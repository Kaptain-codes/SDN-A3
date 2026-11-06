# ADR-002: Keep Java with Swing for the A4 slice, defer a mobile interface

* **Date:** 06 Nov 2026
* **Author:** Benito Iyambo, 225005794, Role 3 (Architect, views)
* **Requirements affected:** QR2, AC1 (no change to FR1 to FR6)

## Context

The A3 client is written in Java 17 with Swing and FlatLaf, packaged with a bundled runtime (about 47 MB). Swing is a desktop toolkit and does not run on phones. ADR-001 chose shared laptops and desktops as the primary device, so the question is whether the A4 interface technology should change, for example to reach phones. The repository pattern already separates services from storage, which lowers the cost of changing the interface later.

## Options considered

|Option|Short description|
|-|-|
|A|Keep Java with Swing, desktop only (chosen)|
|B|Keep the Java services, replace the UI with JavaFX built for phones|
|C|Keep the Java services, use a browser-based UI that runs on laptops and phones|
|D|Rewrite as a native Android app or in another language|

## Assessment against the drivers

|Driver|A|B|C|D|
|-|-|-|-|-|
|Fits laptop-first (ADR-001)|High|Medium|Medium|Low|
|Reuses A3 code|High|Medium|Medium|Low|
|Reaches phones|None|High|High|High|
|Works offline on a shared laptop|High|Medium|Low until an offline cache is built|Medium|
|Risk and effort before the deadline|Low|High|Medium|High|

## Evidence

* The A3 report: STEMCraft implements FR1 to FR8, but FR5 sync is simulated; the repository's web prototype failed five accessibility checks, cannot start without internet, and lacks the v1.1 fixes.
* Swing's accessibility is weaker than the A3 plan claimed: the final report found that the main screens set no accessible names. This is fixable in Swing.
* The A4 brief values one reliable end-to-end slice over breadth.

## Decision

Choose Option A for A4. Options B and C are recorded as future paths, not rejected on merit. The services and repositories stay independent of the UI so a second interface can be added later.

## Consequences

* **Benefits:** lowest risk; keeps the slice on the working A3 code; leaves time for tests and evaluation.
* **Costs and trade-offs:** no phone interface in A4; the 47 MB download remains.
* **Residual risks:** Swing accessibility needs explicit labels, focus order and non-colour cues (Role 6); phone-only facilitators remain unserved.

## Requirement and traceability changes

No requirement text changes. AC1 stays "partly met" until the Swing fixes are tested. No change-log entry is needed unless the team decides to log every ADR.

## Verification

Accessibility checks on the Swing screens; the slice runs from a clean setup on a Windows laptop.

