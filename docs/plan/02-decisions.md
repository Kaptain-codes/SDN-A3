# Decisions

`saveForOffline` belongs to the activity catalogue repository and persists a selected activity in the local SQLite catalogue/cache. It does not create a session plan, draft, or outbox item. The cached catalogue list is the source used by `findByCriteria` when offline; saving an activity marks that activity as locally available without changing the catalogue's filtering semantics. A plan is written to the plan tables and outbox only by `SessionPlanRepository.save()`.
