package io.github.lstramke.coincollector.model;

import java.util.UUID;

public class User {
    private String id;
    private String name;

    public User(String name){
        this(UUID.randomUUID().toString(), name);
    }

    User(String id, String name) throws IllegalArgumentException{
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("userId is null or blank");
        }
        this.id = id;
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
}
