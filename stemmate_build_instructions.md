# STEMMate: Copilot prompt series (v2, writes files)

Use Copilot Chat in IntelliJ in **Agent mode** (or whichever mode lets it create and edit files in your project). In plain chat mode Copilot cannot create files, so use "insert into new file" on each answer instead.
Use the VSCode equivalent for this if you are using that IDE.

Requirement IDs are FR1 to FR8 plus QR1, QR2, SUS1 and AC1. Do not use any other numbering.

## Setup A: Put the design docs in the repo

Copy these into `docs/design/`:
- `STEMMate_A3_Technical_Resilience_and_Low-Resource_Design_v2.md`
- `Requirments.md`

## Setup B: Standing instructions (do this once)

Create `.github/copilot-instructions.md` with exactly this content. Copilot reads it on every request, so the rules do not need repeating in each prompt.

```markdown
# STEMMate project rules

- Stack: Java desktop client using the Repository pattern, Ballerina backend, PostgreSQL as the
  remote database, SQLite as the local database. Package root: org.stemmate.
- Source of truth: docs/design/STEMMate_A3_Technical_Resilience_and_Low-Resource_Design_v2.md
  (section 4.1 for components and contracts) and docs/design/Requirments.md.
- Requirement IDs are FR1-FR8, QR1, QR2, SUS1, AC1 only. Never invent or use other IDs.
- Prototype scope: workflow 1 Find an activity (FR1, FR2), workflow 2 Create and save a session
  plan (FR3, FR4, FR6), workflow 3 Offline sync and recovery (FR5, QR1, QR2, SUS1).
  FR7 and FR8 are deferred.
- No sign-in, AuthService, session parameters or pupil-identifying data anywhere. Synthetic data only.
- Services depend on repository interfaces, never on concrete stores. save() is local-first and
  idempotent by planId (UUID).
- Output rule: when I ask you to produce a plan, design or schema, write it to the file path I
  give, then reply with only the file path and a summary of at most five lines. Do not paste the
  whole file into chat. If a file exists, update it instead of creating a second copy.
- AI log: after each step, append one entry to docs/ai-use-log.md with the date, the step name,
  the files written, and a one-line note of what I should verify. Do not write anything about
  test results or participants.
```

## Step 0: Context check

```
Read docs/design/STEMMate_A3_Technical_Resilience_and_Low-Resource_Design_v2.md and
docs/design/Requirments.md. Write docs/plan/00-context.md with: the stack, the three prototype
workflows with their FR IDs, what is out of scope, and a list of any ambiguities you found.
Do not write code.
```

## Step 1: Repository layout

```
Propose the monorepo layout and write it to docs/plan/01-repo-layout.md:
- /client Java desktop app (recommend JavaFX or Swing for low-spec shared laptops, and Maven or
  Gradle, with reasons)
- /server Ballerina service
- /db schema scripts
- /docs design, plan, accessibility, tests, evaluation
Show the client package tree (ui, service, repository, store, model, sync, connectivity) using the
class names from section 4.1. Then create the empty directories and a .gitkeep in each.
```

## Step 2: Contracts, schemas and API (corrected version)

If you already ran the earlier Step 2, use Step 2b instead.

```
Create these files, based on section 4.1 of the design doc:

1. Java sources under client/src/main/java/org/stemmate/, one file per type, that COMPILE:
   - model: SyncStatus (enum: WAITING_TO_SYNC, SYNCING, SYNCED, FAILED_RETRY), Activity,
     SessionPlan (including createdAt and updatedAt as Instant), PlanStep, Material, SafetyNote,
     InclusionPrompt, SyncRecord. Use Java records.
   - repository: ActivityRepository, SessionPlanRepository (interfaces with Javadoc), and
     ActivityCriteria (a record with level, topic, maximumDurationMinutes and
     List<String> materialNames).
   - repository.exception: ValidationException, StorageFullException, NetworkUnavailableException.
   Interfaces have no implementations yet.
2. db/sqlite/schema.sql: client tables activities, session_plans, plan_steps, drafts, outbox,
   sync_log. drafts.plan_id must NOT have a foreign key to session_plans (a draft can exist before
   any plan is saved). Add a comment that session_plans.sync_status and outbox.status are updated
   in the same transaction.
3. db/postgres/schema.sql: server tables activities, session_plans (with received_at instead of
   sync_status) and plan_steps only. No drafts, outbox or sync_log on the server.
4. docs/api/endpoints.md: GET /activities, POST /plans (batch of at most 20 plans, idempotent by
   planId, upsert by updatedAt for last-write-wins), GET /sync/health.
5. Rules: no auth or session parameters, no pupil-identifying fields, synthetic data only.
Then tell me how saveForOffline relates to the cached catalogue list (decide and state it in
docs/plan/02-decisions.md).
```

## Step 2b: Apply fixes to what you already generated

```
Revise the Step 2 output you already produced and write the result to files:
- Java sources under client/src/main/java/org/stemmate/ as compiling records and interfaces
  (model, repository, repository.exception), with updatedAt added to SessionPlan and
  ActivityCriteria.materialName changed to List<String> materialNames. Add
  NetworkUnavailableException.
- db/sqlite/schema.sql with the drafts.plan_id foreign key removed and a comment that
  session_plans.sync_status and outbox.status are updated in one transaction.
- db/postgres/schema.sql with only activities, session_plans (received_at instead of
  sync_status) and plan_steps.
- docs/api/endpoints.md with a batch limit of 20 plans and updatedAt in the POST /plans payload.
- docs/plan/02-decisions.md recording how saveForOffline relates to the cached catalogue list.
```

## Step 3: Implementation plan

```
Create docs/plan/03-implementation-plan.md with phased tasks, each small enough for one commit:
1 Skeleton and database setup
2 Workflow 1: Find an activity (FR1, FR2), filtering the cached list offline
3 Workflow 2: Create and save a session plan (FR3, FR4), with autosave and draft restore (FR6)
4 Workflow 3: Outbox, ConnectivityMonitor, SyncManager, retry, status bar (FR5, QR1, QR2)
5 Accessibility fixes (AC1)
6 Sync-failure log and help screen (SUS1)
Number tasks like 2.1, 2.2. Each task has: goal, files touched, requirement IDs, done check, and
whether it is essential for 2 October. Use a checkbox for each task so I can tick them off.
```

## Step 4: Simulation strategy

```
Create docs/plan/04-simulation.md covering the offline/online toggle that simulates
ConnectivityMonitor, a fake RemoteSessionPlanStore that can succeed or fail on demand (for the
"Failed, retry" path), and about 15 synthetic activities. Include a table of what is real and
what is simulated that matches section 7 of the design doc. Also create
client/src/main/resources/seed/activities.json with the synthetic activities.
```

## Step 5: Accessibility plan

```
Create docs/plan/05-accessibility.md: a WCAG 2.2 AA checklist for the Java client (keyboard
operability, focus order, labels, contrast, non-colour status cues, text resizing, accessible
names, error messages). For each, say how to implement it in the chosen UI toolkit and how to
test it. Include an empty "issues found and fixes made" table with columns: ID, date, issue,
WCAG criterion, fix, status.
```

## Step 6: Test plan

```
Create docs/plan/06-test-plan.md covering JUnit tests with fake repositories, idempotent save,
local-first write, the offline evaluation checks from the design doc's traceability table, Ballerina
tests for the idempotent POST, and a manual script for 3-5 classmate evaluation tasks using
anonymous labels (P1, P2). Map each test to a requirement ID. Leave all results columns empty.
```

## Step 7: Traceability

```
Create docs/traceability.md: a table linking evidence, requirement, prototype element and
evaluation check for FR1, FR2, FR3, FR4, FR5, FR6, QR1, QR2, SUS1 and AC1. Add a "Commit
convention" section where every commit message starts with the requirement ID, for example
"FR5: queue plan in outbox".
```

## Step 8: Review

```
Review docs/plan/*.md and the schemas against: scope too large for the time left, anything needing
real infrastructure, tasks with no requirement ID, UI code bypassing services or repositories,
and any mention of sign-in or AuthService. Write the findings and a prioritised cut list to
docs/plan/08-plan-review.md.
```

## Then build

```
Implement task 2.3 from docs/plan/03-implementation-plan.md. Follow the repository pattern. Only
touch the files listed for that task. Tick the checkbox in the plan when finished.
```

## What to check yourself

- Compile the Java skeleton after Step 2 (`mvn compile` or `gradle build`), and run both schema files against SQLite and PostgreSQL to check they load.
- Read `docs/ai-use-log.md` and fill in what you verified and changed. The brief requires an AI-use declaration.
- Never let Copilot fill in test results, participant feedback or contribution history.