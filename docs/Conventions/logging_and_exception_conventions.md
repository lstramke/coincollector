# Logging & Exception Conventions

Unified guidelines for consistent logging and exception handling across the project.

---
## 1. Core Principles
- Consistency over cleverness: use the same patterns everywhere.
- Be concise, contextual, and unambiguous.
- No sensitive data (passwords, tokens, secrets, personal raw data) in logs or exception messages.
- Use English everywhere.
- Parameterize logs with `{}` (SLF4J) – never `+` concatenation.
- One event per log entry; no multi-purpose blobs.
- Exceptions carry context (who/what/why) but stay minimal.

---
## 2. Logging
### 2.1 Levels
| Level | Use Case |
|-------|----------|
| TRACE | Extremely fine-grained diagnostics (temporary). |
| DEBUG | Technical flow detail (entity read, branch decisions). |
| INFO  | Successful domain state changes (create/update/delete). |
| WARN  | Recoverable irregularities (validation failed, not found when notable, unexpected row count). |
| ERROR | Unexpected technical failure (propagating exception path). |

Rules:
- Do not use ERROR for handled business conditions.
- Keep INFO sparse: only meaningful state transitions.

### 2.2 Message Structure
Pattern:
```
<Entity> <action|state>: id={}[, key={}]
```
Examples:
- `User created: id={}`
- `User updated: id={}`
- `User deleted: id={}`
- `User read: id={}` (DEBUG)
- `User not found: id={}` (DEBUG/WARN)
- `User validation failed: userId null/blank`
- `User create failed: id={}`
- `User not updated (rowsAffected={}): id={}`

Past tense for success; negative cases use: `failed`, `aborted`, `not found`, `not <past tense>`.

### 2.3 Placeholders
- Always: `logger.info("User created: id={}", userId);`
- Order: primary identifiers first (`id`, `groupId`, `collectionId`), then metrics (`rowsAffected`, `count`).
    - If two identifiers provide better context, include both in the log message.
- No manual concatenation.

### 2.4 Validation & Technical Distinction
| Scenario | Level | Example |
|----------|-------|---------|
| Validation aborted | WARN | `User update aborted: validation failed` |
| Optional absence | DEBUG | `User not found: id={}` |
| Required absence | WARN | `User not found: id={}` |
| Mapping invalid | WARN | `User read produced invalid data: id={}` |
| DB/IO failure | ERROR | `User update failed: id={}` |
| Unexpected rows | WARN (then throw) | `User not updated (rowsAffected={}): id={}` |

### 2.5 Interaction with Exceptions
- Log at the boundary where you have the richest context.
- Avoid double-logging the same failure unless adding context.
- Validation failures: only WARN once.

### 2.6 Performance
- Guard expensive log assembly with level checks.
- Avoid hot-loop logging.

### 2.7 Quick Logging Checklist
- Correct level?
- Contains identity key(s)?
- Uses `{}` placeholders?
- No sensitive data?
- Matches phrasing patterns?

---
## 3. Exceptions
### 3.1 Categories
| Category | Typical Cause | Action / Type |
|----------|---------------|---------------|
| Validation / Input | Null/blank IDs, invalid values | Throw `IllegalArgumentException` |
| Not Found (optional) | Missing but acceptable | Return `Optional.empty()` |
| Technical (DB/IO) | SQL driver, IO issues | Propagate `SQLException` (wrap only with context) |
| Illegal State | Contradictory internal condition | `IllegalStateException` |

### 3.2 Optional vs Exception
| Scenario | Return / Throw |
|----------|----------------|
| Non-mandatory lookup | `Optional.empty()` |
| Corrupt row mapping | WARN + `Optional.empty()` |

### 3.3 Message Style
Format: `Context: detail`.
Examples:
- `User validation failed (create)`
- `User update affected unexpected number of rows: 0`
- `userId must not be null or blank (delete)`
Guidelines:
- No trailing period (unless multi-sentence).
- Avoid redundant words (`Exception while ...`).

### 3.4 Throwing Strategy
- Use unchecked for programmer or validation errors.
- Use checked only for external recoverable failures (e.g., SQL).
- Wrap only when crossing abstraction boundary AND adding value.

### 3.5 Logging & Exceptions Alignment
| Scenario | Log? | Level | Notes |
|----------|------|-------|-------|
| Validation failure | Yes | WARN | Do not log again higher up. |
| Optional empty (normal) | Maybe | DEBUG | Often skip. |
| Technical failure | Yes | ERROR | Include id. |
| Unexpected row count | Yes then throw | WARN | Add row count. |
| Re-throw unchanged | No | - | Unless adding context. |

### 3.6 Exception Checklist
- Correct type chosen?
- Message contextual & minimal?
- Includes key identifier(s)?
- No sensitive data?
- Original cause preserved?
- Not double-logged?

---
## 4. Alignment Rules (Cross-Cutting)
- Logging level usage: INFO (success), WARN (recoverable irregular), ERROR (unexpected failure).
- Always `{}` placeholders in logs.
- Clear, contextual exception messages.
- No sensitive data logged or embedded in exception messages.
- Consistent wording: success = past tense; failure = `failed/aborted/not ...`.

---
## 5. Quick Reference Table
| Aspect | Rule |
|--------|------|
| Success log | `Entity action: id={}` at INFO |
| Validation fail | WARN with reason (no ERROR) |
| Not found (optional) | DEBUG (or no log) + `Optional.empty()` |
| Unexpected rows | WARN + throw |
| Tech failure | ERROR + context |
| Sensitive data | Never log |
| Placeholders | Always `{}` |

---
## 6. Summary
Log concise, contextual events with correct levels using placeholders; throw purposeful, contextual exceptions; prefer Optional for normal absence; never expose sensitive data; remain consistent.
