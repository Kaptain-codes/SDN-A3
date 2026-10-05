# Repository layout

```text
/
├── client/                         Java desktop client
│   ├── pom.xml                     Maven build (JavaFX is not required for the skeleton)
│   └── src/main/
│       ├── java/org/stemmate/
│       │   ├── ui/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── store/
│       │   ├── model/
│       │   ├── sync/
│       │   └── connectivity/
│       └── resources/seed/
├── server/                         Ballerina service
├── db/
│   ├── sqlite/schema.sql            local schema
│   └── postgres/schema.sql          remote schema
└── docs/
    ├── design/
    ├── plan/
    ├── accessibility/
    ├── tests/
    └── evaluation/
```

Java uses Maven because it provides a conventional low-friction build and dependency layout for shared low-specification laptops. Swing is recommended for the first client because it is included in the JDK and avoids a separate JavaFX runtime; the UI remains separated from services so JavaFX can be introduced later without changing repository contracts.

## Client package tree

```text
org.stemmate
├── ui/
│   ├── ActivityListScreen
│   ├── ActivityDetailScreen
│   ├── PlanBuilderScreen
│   ├── SyncStatusBar
│   └── HelpScreen
├── service/
│   ├── ActivityCatalogueService
│   └── SessionPlanService
├── repository/
│   ├── ActivityRepository
│   ├── ActivityCriteria
│   └── SessionPlanRepository
├── store/
│   ├── LocalActivityStore
│   ├── LocalSessionPlanStore
│   └── RemoteSessionPlanStore
├── model/
│   ├── SyncStatus
│   ├── Activity
│   ├── SessionPlan
│   ├── PlanStep
│   ├── Material
│   ├── SafetyNote
│   ├── InclusionPrompt
│   └── SyncRecord
├── sync/
│   └── SyncManager
└── connectivity/
    └── ConnectivityMonitor
```

Each package directory has a `.gitkeep` until its implementation is added.
