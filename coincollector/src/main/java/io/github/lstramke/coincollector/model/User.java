package io.github.lstramke.coincollector.model;

import java.util.UUID;

/**
 * Domain user owning collection groups.
 */
public class User {
    private final String id;
    private final String name;
    private String passwordHash;

    /**
     * Create a new user with a freshly generated random id.
     * Prefer this for normal application code when no id exists yet.
     */
    public User(String name) {
        this(createUserId(), name, null);
    }

    public User(String name, String passwordHash) {
        this(createUserId(), name, passwordHash);
    }

    /**
     * Package-private constructor intended for {@link UserFactory} and
     * persistence/import layers that must preserve an existing id (no new id
     * generation). Not public to discourage arbitrary id injection in regular
     * domain workflows.
     */
    User(String id, String name, String passwordHash) throws IllegalArgumentException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("userId is null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("username is null or blank");
        }
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
    }

    private static String createUserId() {
        return UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
