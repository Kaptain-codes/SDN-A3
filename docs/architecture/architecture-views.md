# Architecture views (A4)

These diagrams render on GitHub. The editable sources are the .mmd files in this folder. Green solid = built; yellow dashed = designed only; blue = person. See README.md for purposes and requirement links.

## View 1: System context

- View 1: System context. Purpose: show who uses STEMMate and what sits outside it.
- Requirements: FR1-FR8 (facilitator), FR7 (custodian), SUS1 (administrator), PR rule: no learner data.
- Legend: green solid = built; yellow dashed = designed only; blue = person; dashed arrow = not built or optional.
- Constraints: intermittent internet, costly data, shared low-spec laptops, limited support. No learner data is stored.

```mermaid
flowchart LR
  fac["Facilitator<br/>plans STEM sessions<br/>(FR1-FR6, FR8)"]
  cus["Equipment custodian<br/>records kit loans (FR7)"]
  adm["Local administrator<br/>reads the sync-failure log (SUS1)"]
  sys["STEMMate (STEMCraft desktop client)<br/>shared Windows laptop,<br/>offline first"]
  srv["Remote STEMMate server<br/>Ballerina API + PostgreSQL<br/>DESIGNED, NOT BUILT"]
  fac -->|"filters activities,<br/>builds and finalises plans"| sys
  cus -->|"logs kit checkouts"| sys
  adm -.->|"reads local log"| sys
  sys <-.->|"small batches over HTTPS,<br/>only when online"| srv
  classDef built fill:#dff0d8,stroke:#3c763d,color:#000
  classDef designed fill:#fcf8e3,stroke:#8a6d3b,stroke-dasharray:5 5,color:#000
  classDef person fill:#e8eefc,stroke:#31528c,color:#000
  class sys built
  class srv designed
  class fac,cus,adm person
```

## View 2: Containers

- View 2: Containers. Purpose: show the main runnable parts and what each stores or calls.
- Requirements: FR2, FR5, FR6, QR1, QR2, SUS1. ADR-001 (laptop first), ADR-002 (Java + Swing), ADR-003 (storage, Role 4 package, section 4).
- Legend: green solid = built; yellow dashed = designed only.
- Note: the stand-in store always confirms, so the failure path cannot be shown yet (A3 finding F2).

```mermaid
flowchart TB
  subgraph laptop["Shared Windows laptop"]
    client["STEMCraft client<br/>Java 17, Swing + FlatLaf<br/>BUILT"]
    files[("Local store<br/>Java-serialised files in .stemmate<br/>BUILT")]
    sqlite[("Local SQLite database<br/>schema_Patched.sql<br/>DESIGNED, not connected")]
    standin["Stand-in remote store<br/>in memory, always confirms<br/>BUILT (simulated)"]
  end
  subgraph cloud["Server side (not built)"]
    api["Ballerina API<br/>DESIGNED"]
    pg[("PostgreSQL<br/>DESIGNED")]
  end
  client -->|"reads and writes first"| files
  client -.->|"target replacement"| sqlite
  client -->|"sync batches (max 20)"| standin
  client -.->|"HTTPS, when online"| api
  api -.-> pg
  classDef built fill:#dff0d8,stroke:#3c763d,color:#000
  classDef designed fill:#fcf8e3,stroke:#8a6d3b,stroke-dasharray:5 5,color:#000
  class client,files,standin built
  class sqlite,api,pg designed
```

## View 3: Components (inside the client)

- View 3: Components inside the STEMCraft client. Purpose: show layers and dependencies.
- Requirements: FR1-FR8, QR1 (local first), SUS1 (sync log). Arrows checked against the imports and App.java at commit 28fef52.
- Legend: solid arrows point from a class to the class it uses; dashed arrows mean "implements".
- Note: SessionPlanService depends only on SessionPlanRepository; the outbox record is created inside LocalSessionPlanStore, which uses the concrete LocalOutboxRepository.
- Gap: few accessible names are set in the UI (AC1); there is no sign-in or close warning (QR2).

```mermaid
flowchart TB
  app["App<br/>composition root: creates the stores,<br/>services, controller and window"]
  subgraph ui["UI layer"]
    shell["DesktopShellFrame<br/>screens, role switch"]
    bar["SyncStatusBar<br/>Online/Offline, pending count"]
    theme["AppTheme, ThemeComponents, RoundedBorder"]
  end
  ctrl["DesktopController<br/>wires the UI to the services"]
  subgraph svc["Service layer (business logic and validation)"]
    cat["ActivityCatalogueService<br/>FR1, FR2"]
    plan["SessionPlanService<br/>FR3, FR4, FR6, FR8"]
    kit["EquipmentKitService<br/>FR7"]
  end
  sync["SyncManager<br/>batches of 20, retry<br/>FR5, QR1, QR2"]
  conn["ConnectivityMonitor<br/>Online/Offline switch"]
  subgraph repo["Repository interfaces"]
    r1["ActivityRepository"]
    r2["SessionPlanRepository"]
    r3["OutboxRepository"]
    r4["SyncLogRepository"]
    r5["EquipmentKitRepository"]
    r6["RemoteSessionPlanStore"]
  end
  subgraph store["Implementations (files today, SQLite is the target)"]
    stAct["LocalActivityStore"]
    stPlan["LocalSessionPlanStore"]
    stOut["LocalOutboxRepository"]
    stLog["LocalSyncLogStore"]
    stKit["LocalEquipmentKitStore"]
    stRem["LocalRemoteSessionPlanStore<br/>simulated server"]
    fss["FileStoreSupport<br/>reads and writes the .stemmate files"]
    mem["InMemoryDraftStore<br/>unused alternative"]
  end
  app --> shell
  app --> ctrl
  shell --> ctrl
  shell --> bar
  shell --> theme
  ctrl --> cat
  ctrl --> plan
  ctrl --> kit
  ctrl --> sync
  ctrl --> conn
  sync --> conn
  cat --> r1
  plan --> r2
  kit --> r5
  sync --> r3
  sync --> r2
  sync --> r4
  sync --> r6
  stAct -.->|implements| r1
  stPlan -.->|implements| r2
  stOut -.->|implements| r3
  stLog -.->|implements| r4
  stKit -.->|implements| r5
  stRem -.->|implements| r6
  mem -.->|implements| r2
  stPlan -->|"queues a record when a plan is finalised"| stOut
  stAct --> fss
  stPlan --> fss
  stOut --> fss
  stLog --> fss
  stKit --> fss
```

## View 4: Deployment

- View 4: Deployment. Purpose: show where each part runs and over what link.
- Requirements: QR2 (low-spec shared devices, costly data), SUS1 (local support), QR1 (data stays local until confirmed).
- Legend: green solid = built; yellow dashed = designed only; hexagon = network link.
- Delete the .stemmate folder to reset the demo. No sign-in, so anyone on the laptop account can read the plans (A3 pain point 4).

```mermaid
flowchart LR
  subgraph node1["Shared Windows laptop (primary device, ADR-001)"]
    rt["STEMCraft.exe<br/>jpackage bundle, about 47 MB,<br/>own Java runtime"]
    home[".stemmate folder in the user's home<br/>plans, drafts, outbox, sync log, kits"]
  end
  net{{"Intermittent school internet<br/>costly data, small batches only"}}
  subgraph node2["Remote host (not built)"]
    api["Ballerina API"]
    pg[("PostgreSQL")]
  end
  subgraph node3["Developer and test machines"]
    src["GitHub repo Kaptain-codes/SDN-A3<br/>source, schemas, docs"]
  end
  rt --- home
  rt -.-> net
  net -.-> api
  api -.-> pg
  src -.->|"build and release (zip)"| rt
  classDef built fill:#dff0d8,stroke:#3c763d,color:#000
  classDef designed fill:#fcf8e3,stroke:#8a6d3b,stroke-dasharray:5 5,color:#000
  class rt,home,src built
  class api,pg designed
```
