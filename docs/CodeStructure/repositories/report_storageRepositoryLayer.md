# Storage Repository Layer – Current State

## 1. Purpose
The storage repository layer provides JDBC/SQL based persistence (currently SQLite) for the domain objects: `EuroCoin`, `EuroCoinCollection`, `EuroCoinCollectionGroup`, and `User`. It exposes a small, explicit set of CRUD-style operations while isolating higher layers from SQL, JDBC boilerplate, and row-to-object mapping.

## 2. Responsibilities Split
| Layer | Responsibility |
|-------|----------------|
| Service Layer | Opens/controls `Connection`, sets transactional scope, orchestrates multiple repository calls, decides commit/rollback. |
| Repository Layer | Validates inputs, executes prepared SQL using a provided `Connection`, maps `ResultSet` rows to domain objects, returns results (no transaction side-effects). |

Design constraints:
* A repository method never closes, commits, rolls back, or mutates the auto-commit state of the provided `Connection`.
* Repositories are stateless and reusable (safely shareable singletons).

## 3. Diagram

![Storage Repository Layer v3](storageRepositoryLayer_v3.svg)

## 4. Repository Interfaces (Canonical Shape)
All public methods accept a `java.sql.Connection` as first parameter. Return contracts avoid throwing checked SQL exceptions outward; instead they translate into `false` or `Optional.empty()` plus logging.

Generic pattern (illustrative):
```
boolean create(Connection connection, Entity entity);
Optional<Entity> read(Connection connection, String id);
boolean update(Connection connection, Entity entity);
boolean delete(Connection connection, String id);
Optional<Boolean> exists(Connection connection, String id);
List<Entity> getAll(Connection connection);               // where applicable
```
Specialized:
```
List<EuroCoinCollectionGroup> getAllByUser(Connection connection, String userId);
```

Implemented interfaces:
* `EuroCoinStorageRepository`
* `EuroCoinCollectionStorageRepository`
* `EuroCoinCollectionGroupStorageRepository`
* `UserStorageRepository` (no `getAll()` requirement)

## 5. Implementations
Concrete classes (suffix `SqliteRepository`) encapsulate table-specific SQL. Consistent internal elements:
| Aspect | Notes |
|--------|-------|
| Table name | Held as constructor parameter; used in formatted SQL (controlled source). |
| Logger | One static logger per class for structured messages. |
| Factories | Domain mappers (`EuroCoinFactory`, etc.) convert `ResultSet` rows to rich domain objects. |
| Validation | Early guard clauses prevent invalid writes/updates/deletes (null/blank IDs, required fields). |
| Error handling | Catches `SQLException`, logs `error`, returns failure indicator. |
| Resource scope | Uses try-with-resources for `PreparedStatement` & `ResultSet`; does not close the passed `Connection`. |

## 6. SQL & Parameter Handling
* Always use `PreparedStatement` for dynamic values.
* Table name is the only concatenated element; current assumption: internal controlled enumeration (safe from injection). Should be white‑listed if external configuration is added later.
* No implicit ordering unless functionally required; consumers should not rely on row order.

## 7. Transaction Usage Pattern (from Service Layer)
Typical orchestration (example only):
```
try (Connection c = dataSource.getConnection()) {
  c.setAutoCommit(false);
  userRepository.create(c, user);
  groupRepository.create(c, group);
  collectionRepository.create(c, collection);
  c.commit();
} catch (Exception e) {
  // rollback best-effort
  try { c.rollback(); } catch (SQLException ignored) { }
  // propagate or translate
}
```
Guidelines:
1. Roll back on any repository failure (`false` / `Optional.empty()` where empty signals data or SQL issue).
2. Optionally use savepoints for partial optional operations.
3. Adjust isolation level only in service layer if stricter consistency required.

## 8. Validation Rules (Current Set)
| Entity | Core Checks |
|--------|-------------|
| EuroCoin | id not blank; year >= configured start; value / mintCountry / mint non-null; collectionId not blank |
| EuroCoinCollection | id not blank; coins list non-null (may be empty) |
| EuroCoinCollectionGroup | id not blank; ownerId not blank; collections list non-null (may be empty) |
| User | userId not blank |

Validation returns a boolean; failures log at `warn` and short-circuit before SQL execution.

## 9. Logging Policy
| Level | When |
|-------|------|
| debug | SQL execution context, row counts, cache/miss style info. |
| info  | Successful single-row create/update/delete. |
| warn  | Validation failure; 0 rows affected when 1 expected; suspicious input. |
| error | SQLException or mapping failure (include SQL state & vendor code). |

Optional enhancement: pass a correlation/transaction id from the service layer for multi-call traceability.

## 10. Summary
The repository layer is a stateless, connection-accepting data mapping component. It centralizes SQL and mapping, delegates all transaction control upward, and enforces lightweight validation and consistent logging without throwing checked SQL exceptions into domain logic.
