package com.rokudoz.irecipe.Models;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class User {

    private String user_id;
    private String name;
    Map<String, Boolean> tags;
    private ArrayList<String> favoriteRecipes;
    private String userProfilePicUrl;

    public User(String user_id, String name, Map<String, Boolean> tags, ArrayList<String> favoriteRecipes,String userProfilePicUrl) {
        this.user_id = user_id;
        this.name = name;
        this.tags = tags;
        this.favoriteRecipes = favoriteRecipes;
        this.userProfilePicUrl = userProfilePicUrl;
    }


    public User() {
        //Empty constructor
    }

    public ArrayList<String> getFavoriteRecipes() {
        return favoriteRecipes;
    }

    public void setFavoriteRecipes(ArrayList<String> favoriteRecipes) {
        this.favoriteRecipes = favoriteRecipes;
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


    public String getUserProfilePicUrl() {
        return userProfilePicUrl;
    }

    public void setUserProfilePicUrl(String userProfilePicUrl) {
        this.userProfilePicUrl = userProfilePicUrl;
    }

    @Override
    public String toString() {
        return "User{" +
                "user_id='" + user_id + '\'' +
                ", name='" + name + '\'' +
                ", tags=" + tags +
                ", favoriteRecipes=" + favoriteRecipes +
                ", userProfilePicUrl='" + userProfilePicUrl + '\'' +
                '}';
    }
}