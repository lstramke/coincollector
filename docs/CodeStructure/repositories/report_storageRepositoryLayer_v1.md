# Class Diagram Report: Storage Repository Layer

## Overview

This document describes the design and structure of the Storage Repository Layer for the CoinCollector application. The layer abstracts the persistence of core domain entities related to Euro coins and their collections, providing a clean interface for data operations while hiding the underlying database implementation details.

## Class Diagram

![Storage Repository Layer v1](storageRepositoryLayer_v1.svg)

The model consists of three main repository interfaces and their corresponding concrete SQLite implementations:

- **Interfaces:**
  - `EuroCoinStorageRepository`
  - `EuroCoinCollectionStorageRepository`
  - `EuroCoinCollectionGroupStorageRepository`

- **Concrete Implementations:**
  - `EuroCoinSqliteRepository`
  - `EuroCoinCollectionSqliteRepository`
  - `EuroCoinCollectionGroupSqliteRepository`

### Interfaces

Each repository interface defines the core CRUD operations expected for the respective entity, along with additional helpful methods:

- `create(entity) : boolean` — Adds a new entity.
- `read(id) : Entity` — Retrieves an entity by its ID.
- `update(entity) : boolean` — Updates an existing entity.
- `delete(id) : boolean` — Removes an entity by its ID.
- `getAll() : List<Entity>` — Retrieves all entities (or `getAllByUser(userId)` for groups).
- `exists(id) : boolean` — Checks if an entity with the given ID already exists.

The `exists` method primarily supports service-layer logic to verify the presence of an entity before performing insert or update operations, improving error prevention and data consistency.
The boolean return type indicates success or failure, enabling straightforward error handling without modeling exceptions directly in the interface.

### Concrete Implementations

The concrete classes implement these interfaces using SQLite as the persistence technology. Each implementation holds:

- A `Connection` attribute, injected via the constructor to ensure all repositories share the same database connection instance. This design choice supports consistent transaction management and resource handling.
- A `tableName` attribute representing the specific database table associated with the repository.
- A `static final logger` attribute in each concrete repository implementation to provide consistent, class-level logging across all instances, facilitating centralized tracking of database operations and errors.

## Design Considerations

- **Separation of Interface and Implementation:** By defining repository interfaces independent of the storage technology, the design allows for future flexibility. For example, switching to a different database or storage method would only require new implementations without affecting the service layer.

- **Shared Database Connection:** Passing the same `Connection` object to each repository implementation promotes resource efficiency and consistent transactional boundaries.

- **Return Types and Error Handling:**  boolean return types on CRUD operations to signal success or failure, avoiding exception modeling in the UML. Exceptions can be handled at the implementation or service layer.

- **User-Specific Queries:** The `EuroCoinCollectionGroupStorageRepository` interface includes a user-specific retrieval method (`getAllByUser(User user)`), reflecting the domain requirement that collection groups belong to individual users.

