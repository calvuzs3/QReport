# QReport — 4 Client Group Overview

**Version:** 1.0
**Date:** June 2026

---

## 1. Data Hierarchy

```
Client
├── Facilities          (physical plants/sites)
│   └── Islands         (robotic islands, POLY family)
│       └── MechanicalUnits  (robots, axes, panels, ...)
├── Contacts            (people at the client company)
└── Contracts           (service agreements)
```

Each level depends on its parent via Room `ForeignKey(onDelete = CASCADE)`.

---

## 2. Feature Documents

| # | Feature | Scope | Parent |
|---|---------|-------|--------|
| 4_1 | Client | `client/client` | — |
| 4_2 | Facility | `client/facility` | Client |
| 4_3 | Island | `client/island` | Facility |
| 4_4 | MechanicalUnit | `client/unit` | Island |
| 4_5 | Contact | `client/contact` | Client |
| 4_6 | Contract | `client/contract` | Client |

---

## 3. Architecture Patterns (common to all features)

**Layer stack:**
```
UI (Composable)
    ↓ collects StateFlow
ViewModel
    ↓ calls
UseCase          ← returns QrResult<D, QrError.FeatureError>
    ↓ calls
Repository       ← returns kotlin.Result<T>
    ↓ calls
DAO / Room
```

**String resources:** all user-facing strings via `stringResource()` or `labelResId: Int` on enums.
No hardcoded strings in domain or data layers.

**Error flow:** `QrError.FeatureError` → `result.error.asUiText()` → `UiText` → `error: UiText?` in UiState.

---

## 4. Delete Lifecycle (all entities)

Every entity follows the same two-stage soft-delete:

```
isActive=true,  isDeleted=false   →  [normal]
        ↓ first invoke of DeleteUseCase
isActive=false, isDeleted=false   →  [deactivated]  — hidden in UI
        ↓ second invoke of DeleteUseCase
isActive=false, isDeleted=true    →  [marked deleted] — queued for server sync
```

The `DeleteXxxUseCase` reads the current state and picks the stage automatically,
returning a `DeleteXxxResult` enum (`DEACTIVATED` or `MARKED_DELETED`) so the
ViewModel can show the appropriate snackbar message.

**Cascade on delete:** when a parent is deleted, all children follow in the same
`@Transaction`, via bulk `UPDATE` queries in the parent's DAO:

```
Client deactivated   → Facilities, Islands, MechanicalUnits, Contacts, Contracts
Facility deactivated → Islands, MechanicalUnits          (via FacilityDao @Transaction)
Island deactivated   → MechanicalUnits                   (via IslandDao @Transaction)
MechanicalUnit       → no children
Contact              → no children
Contract             → no children
```

All DAOs also include `is_deleted = 0` in every normal read query.

---

## 5. UI Patterns (common to all list screens)

```
TopAppBar  (icon, title, cycleCardVariant, sort, filter)
QReportSearchBar
QReportFiltersChipRow       ← visible only when non-default filter/sort active
QReportSelectorRow(s)       ← one per ancestor level (see below)
Content area (PullToRefresh)
FAB → create
```

**Selector rows by feature:**

| Feature | Selectors |
|---------|-----------|
| Facility | Client |
| Island | Client → Facility |
| MechanicalUnit | Client → Facility → Island |
| Contact | Client |
| Contract | Client |

`null` selection at any level shows all records for that level.
Each selector is `enabled` only if its parent selector has a value.

**Card variants:** every entity card implements FULL / COMPACT / MINIMAL.
Button sizes: 48 dp (FULL), 36 dp (COMPACT). Both glove-friendly.

---

## 6. Screens by Feature

| Feature | List | Form | Detail |
|---------|------|------|--------|
| Client | ✅ | ✅ | ✅ (4 tabs) |
| Facility | ✅ | ✅ | ✅ (3 tabs) |
| Island | ✅ | ✅ | ✅ (3 tabs) |
| MechanicalUnit | ✅ | ✅ | — |
| Contact | ✅ | ✅ | — |
| Contract | ✅ | ✅ | — |

Leaf-level entities (MechanicalUnit, Contact, Contract) have no detail screen —
all data fits on the FULL card variant.

---

## 7. Checkup Integration

> **Status: not yet implemented** across all features.

Planned integration points:
- `CheckUp.clientId` → per-client history
- `CheckUp.facilityId` → per-facility history
- `CheckUp.islandId` → per-island inspection records
- `CheckUp.unitId` → per-unit line items within a checkup

Implementation deferred to the CheckUp feature phase.