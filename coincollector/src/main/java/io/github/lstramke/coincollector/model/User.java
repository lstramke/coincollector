package io.github.lstramke.coincollector.model;

import java.util.List;
import java.util.UUID;

public class User {
    private String id;
    private String name;
    private List<CollectionGroup> ownedCollections;

    public User(String name){
        this.id = UUID.randomUUID().toString();
        this.name = name;
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

    public List<CollectionGroup> getOwnedCollections() {
        return ownedCollections;
    }
}
