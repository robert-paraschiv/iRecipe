package com.rokudoz.irecipe.Models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class User {

    private String user_id;
    private String name;
    Map<String, Boolean> tags;
    private ArrayList<String> favoriteRecipes;
    private String userProfilePicUrl;
    private List<String> ingredient_array;

    public User(String user_id, String name, Map<String, Boolean> tags, ArrayList<String> favoriteRecipes, String userProfilePicUrl, List<String> ingredient_array) {
        this.user_id = user_id;
        this.name = name;
        this.tags = tags;
        this.favoriteRecipes = favoriteRecipes;
        this.userProfilePicUrl = userProfilePicUrl;
        this.ingredient_array = ingredient_array;
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

    public List<String> getIngredient_array() {
        return ingredient_array;
    }

    public void setIngredient_array(List<String> ingredient_array) {
        this.ingredient_array = ingredient_array;
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