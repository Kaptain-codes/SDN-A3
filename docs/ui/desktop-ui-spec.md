# Desktop UI Specification: STEMCraft Facilitator Studio & Kit Manager

**Target Framework:** Java Swing
**Architecture:** MVC / MVP (Strict separation of UI from Business Logic)
**Context:** This application operates in an offline-first environment for STEM facilitators and equipment custodians.

---

## 1. Design Principles
* **Source of Truth:** This document supersedes any assumed visual or structural conventions. Do not invent alternative UI behaviors.
* **No Dead Controls:** Every visible interactive control must trigger a real backend system operation or be explicitly disabled. No UI-only placeholder buttons or fake dialogs.
* **Offline-First:** All read/write operations execute against local storage (SQLite/JSON) first. Network operations happen asynchronously via the Outbox/SyncManager.
* **Logic-Free UI:** Swing `ActionListener`s and UI components must NOT contain business logic, validation rules, or database queries. They must delegate to interface-driven Services.

---

## 2. Visual System

Do not use default Swing Look & Feel colors. Implement custom renderers or use FlatLaf with the following strict token overrides.

### 2.1 Colors
* **Canvas / App Background:** `#F7F7F5`
* **Surface / Cards:** `#FFFFFF`
* **Borders / Dividers:** `#E5E5DF`
* **Text Primary:** `#171716`
* **Text Secondary:** `#73736C`
* **Brand (STEMCraft Green):** Primary `#2D6A4F`, Hover `#1F4E3D`, Light/Badge `#F2F7F4`
* **Status - Success/Online (Emerald):** `#2F855A`
* **Status - Pending/Offline (Amber):** `#B7791F`
* **Status - Error/Safety (Red):** `#C94B45`
* **Status - Inclusion (Indigo):** `#5B5FC7`

### 2.2 Typography
* **Font Family:** `Plus Jakarta Sans` (Primary), `JetBrains Mono` (Data/Badges). *Swing Fallback:* System Sans-Serif / Monospaced.
* **Scale:**
  * H1 (Screen Titles): 20px, Bold
  * H2 (Card Titles): 14px, Bold
  * Body text: 12px, Regular
  * Badges/Metadata: 10px, Monospaced, Uppercase tracking

### 2.3 Components & Spacing
* **Cards:** White background, 1px `#E5E5DF` line border, 16px corner radius (requires custom `Border` implementation), 16px-24px internal padding. Subtle drop shadow.
* **Buttons:** 12px radius, 8px/16px padding.
  * *Primary:* Brand Green background, White text.
  * *Secondary:* Canvas background, Border `#E5E5DF`, Primary text.
* **Inputs/Selects:** 12px radius, `#FAFAF8` background, 1px `#E5E5DF` border. Focus state must draw a 3px Brand Green outline.

---

## 3. Application Shell

The main window utilizes a `BorderLayout`.

### 3.1 Header (North)
* **Left:** App Logo and Title.
* **Right:** 
  * Network Status Toggle (Simulates connectivity for testing: Online=Emerald, Offline=Amber).
  * Sync Indicator (Only visible when outbox > 0). Displays count and "Sync" button.
  * Role Switcher (Facilitator vs. Custodian toggle).

### 3.2 Sidebar Navigation (West)
* Renders as a vertical list of buttons. 
* **Items:** Activity Catalog, Saved Offline (with dynamic count badge), Session Planner, Equipment Custody.
* **Footer:** Offline-ready info card.

### 3.3 Main Content Area (Center)
* Managed by a `CardLayout` to seamlessly swap between the four primary views without spawning new `JFrame` instances.

---

## 4. Screen-by-Screen Specification

### 4.1 Activity Catalog (FR1, FR2)
* **Header:** Title + Search Bar.
* **Filters Area:** Four dropdowns (Level, Topic, Duration, Materials) + "Reset all" button.
* **Content:** Responsive grid (FlowLayout or WrapLayout) of Activity Cards.
* **Card Elements:** Tags, Title, Description, Materials list. "Save Offline" / "Remove" button, "Create Plan" button.

### 4.2 Saved Offline (FR2)
* **Header:** Title + Storage Ready badge.
* **Content:** Grid of saved activities. Same card layout as Catalog, but missing the "Create Plan" action if viewed strictly in library mode (or dynamically routed to builder).
* **Empty State:** Icon + "No offline activities saved" if count is 0.

### 4.3 Session Planner (FR3, FR4, FR6, FR8)
* **Header:** Title, Autosave status badge ("Saved" vs "Saving..."), "Duplicate draft" button, "Save & finalize" button.
* **Main Form:** 
  * Title input.
  * Associated Activity dropdown & Target group input.
  * Dynamic Steps List: Rows containing Step Name, Timing, Description, and a "Remove" button. Requires an "Add step" button.
  * Materials `JTextArea`.
  * Safety Notes `JTextArea` (Red tinted background).
  * Inclusion Prompts `JTextArea` (Indigo tinted background).
* **Sidebar (Right):** List of finalized saved session plans with Edit and Duplicate actions.

### 4.4 Equipment Custody (FR7)
* **Header:** Title + "Log kit checkout" button.
* **Content:** `JTable` containing Kit ID, Responsible Person, Intended Date, Status, Actions (Edit/Delete).
* **Modal Dialog:** `JDialog` for logging/editing a checkout. Fields: Kit Name (required), Person (required), Date (required), Status (dropdown).

---

## 5. Interaction Specification & FR Mapping

Every interactive control must map strictly through this pipeline. **Do not bypass the service layer.**

| UI Feature / Control | UI Event | Service / System Operation | Persistence / State | FR Mapping |
| :--- | :--- | :--- | :--- | :--- |
| **Catalog Filters** (Dropdowns, Search) | `ItemListener` / `DocumentListener` | `ActivityService.getFiltered(...)` | Local SQLite Read -> Update UI Grid | FR1 |
| **Save Offline** (Button on Card) | `ActionListener` | `ActivityService.toggleSaveOffline(id)` | `LocalActivityStore` -> Update counter/badge | FR2 |
| **Create Plan** (Button on Card) | `ActionListener` | `PlanService.initFromActivity(id)` | In-Memory Draft -> Switch to Builder Tab | FR3 |
| **Session Plan Inputs** (Any keystroke/change) | `DocumentListener` (debounced) | `DraftService.autoSave(planDTO)` | `LocalDraftStore` -> Update Autosave Badge | FR6 |
| **Add/Remove Step** (Buttons in Builder) | `ActionListener` | Update UI Model -> trigger autoSave | `LocalDraftStore` | FR4 |
| **Save & Finalize** (Button in Builder) | `ActionListener` | `PlanService.finalizePlan(planDTO)` | `LocalPlanStore` + `Outbox` -> Refresh List | FR3, FR5 |
| **Duplicate Plan** (Button in Builder/List) | `ActionListener` | `PlanService.duplicatePlan(id)` | `LocalDraftStore` -> Populate form | FR8 |
| **Log Kit Checkout** (Modal Save) | `ActionListener` | `KitService.saveRecord(kitDTO)` | `LocalKitStore` + `Outbox` -> Refresh Table | FR7 |
| **Delete Kit Record** (Table Action) | `ActionListener` | `KitService.deleteRecord(id)` | `LocalKitStore` + `Outbox` -> Refresh Table | FR7 |
| **Network Toggle** (Header) | `ActionListener` | `SyncManager.setNetworkState(bool)` | State update -> Trigger Outbox processing | FR5 |
| **Manual Sync** (Header) | `ActionListener` | `SyncManager.processQueue()` | Clear `Outbox` -> Sync to remote | FR5 |
| **Role Switcher** (Header) | `ActionListener` | `AppController.setRole(role)` | Switch `CardLayout` view | N/A |

---

## 6. State Behavior

* **Offline-First Resilience:** UI must never block or show a loading spinner waiting for a remote HTTP response. Saves return immediately upon local SQLite insertion, appending to the Sync Outbox.
* **Syncing Feedback:** While processing the outbox, display a non-blocking UI Toast or update the header sync indicator to "Syncing...".
* **Empty States:** Empty tables, empty grids, or empty saved lists must render a visual empty state (Center aligned icon + title + subtitle), not just a blank white panel.
* **Form Validation:** The Kit Checkout modal must validate required fields before delegating to `KitService`. Invalid fields should display a red error label beneath the input and draw a red border.
* **Debounced Autosave:** The Session Planner must wait 700ms after the user stops typing before triggering `DraftService.autoSave()` to prevent DB locking.

---

## 7. Accessibility & Keyboard Navigation
* **Focus Order:** Ensure predictable `TAB` traversal top-to-bottom, left-to-right.
* **Visible Focus:** Rely on the defined 3px Brand Green focus ring for all active inputs and buttons. Do not disable default Swing focus painting without replacing it.
* **Accessible Labels:** Any icon-only buttons (like the Trash icon for deleting a step/kit) must have a `setToolTipText()` describing the action.

---

## 8. Implementation Constraints for AI/Copilot
1. **No Logic in UI:** Do not write SQL, file I/O, or domain logic inside `JFrame` or `JPanel` classes. Route all actions through injected Services (e.g., `controller.savePlan()`).
2. **Components:** Break the UI down into reusable Swing components (e.g., `ActivityCardPanel`, `StepRowPanel`) rather than one massive class.
3. **No Mocks (Except Network):** Aside from simulating the network connection toggle, do not create "fake" success dialogs. Actually persist the data to the configured local storage mechanism.
4. **Data Sync:** Honor the existing `SyncManager` outbox queue pattern.