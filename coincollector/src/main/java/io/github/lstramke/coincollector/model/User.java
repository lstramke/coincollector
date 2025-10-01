package io.github.lstramke.coincollector.model;

import java.util.UUID;

/**
 * Domain user owning collection groups.
 */
public class User {
    private String id;
    private String name;

    /**
     * Create a new user with a freshly generated random id.
     * Prefer this for normal application code when no id exists yet.
     */
    public User(String name){
        this(createUserId(), name);
    }

    /**
     * Package-private constructor intended for {@link UserFactory} and
     * persistence/import layers that must preserve an existing id (no new id
     * generation). Not public to discourage arbitrary id injection in regular
     * domain workflows.
     */
    User(String id, String name) throws IllegalArgumentException{
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("userId is null or blank");
        }
        this.id = id;
        if(name == null || name.isBlank()){
             throw new IllegalArgumentException("username is null or blank");
        }
        this.name = name;
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
    
    public void setName(String username) {
        this.name = username;
    }
}
