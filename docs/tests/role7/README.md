# Role 7 testing

Executed 06 November 2026 at 21:43 CAT. This replaces the earlier HTML-only evidence for this handover.

From the repository root run:

```text
java scripts/RunRole7Tests.java
```

Use JDK 21 or later. The runner has no Maven or external test-library dependency. It compiles 45 selected production-core and test files and runs 40 cases in isolated JVMs. The complete GUI is not compiled by this runner. It does not launch SQLite or a live server. `mvn test` does not automatically execute this custom runner: use the command above.

Recorded result: 30 PASS, 10 FAIL. A nonzero exit code is expected while the documented defects remain. This is not a claim that the final application passes acceptance. JUnit-format XML is an output format; the tests use the included Java assertion runner rather than the JUnit library.

Results are written to `docs/tests/role7/evidence`. To retain the original run, give the runner a different output directory:

```text
java scripts/RunRole7Tests.java . target/role7-my-rerun
```

The original execution evidence identifies every case, requirement, component, steps, expectation and actual output. Detailed cases are in TEST_CASES.md; exact FR/QR links are in TRACEABILITY_MATRIX.md; failures are in DEFECT_LOG.md. GUI and live server checks remain in REMAINING_CHECKS.md.

J09 and P03 are actual separate-process restart checks against the repository's real file stores. They are not SQLite restart tests. HTTP transport and route dispatch tests identify their doubles explicitly.
