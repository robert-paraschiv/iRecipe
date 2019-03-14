package com.rokudoz.irecipe.Models;

import java.util.Map;

public class User {

    private String user_id;
    private String name;
    Map<String, Boolean> tags;

    public User(String user_id, String name, Map<String, Boolean> tags) {
        this.user_id = user_id;
        this.name = name;
        this.tags = tags;
    }

    public User() {

    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Boolean> getTags() {
        return tags;
    }

    public void setTags(Map<String, Boolean> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "User{" +
                "user_id='" + user_id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}