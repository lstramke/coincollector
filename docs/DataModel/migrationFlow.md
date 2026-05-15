
## Flyway Migration Overview

Flyway is a lightweight database migration tool that applies versioned SQL scripts to a database in a controlled, repeatable way. Place migration files on the classpath (default: `db/migration`) using the naming conventions `V{version}__{description}.sql` for versioned migrations. On application startup Flyway checks the `flyway_schema_history` table and executes any pending migrations in order, recording each applied migration.

The `baseline-on-migrate` option lets Flyway adopt an existing schema by creating a baseline entry instead of re-applying older migrations. In Spring Boot, configure Flyway via `spring.flyway.*` properties (for example `spring.flyway.locations` and `spring.flyway.baseline-on-migrate`).

## Why we chose Flyway

- because it integrates seamlessly with Spring Boot, 
- works well with plain SQL (which suits SQLite), 
- and provides simple, reliable versioned and repeatable migrations that ensure the same schema changes are applied consistently across environments. 

Flyway's approach reduces drift between environments and makes upgrades and rollouts safer and auditable.

##  Usage notes:
- Put migration scripts in `src/main/resources/db/migration` so they are included on the application classpath.
- Flyway runs automatically at startup when the Flyway starter is present; for tests use a dedicated test datasource (e.g. `application-test.yml`) so Flyway initializes a test DB.
