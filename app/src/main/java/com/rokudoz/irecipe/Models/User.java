package com.rokudoz.irecipe.Models;

import java.util.Date;
import java.util.List;

public class User {
    private String user_id;
    private String name;
    private String username;
    private Date birth_date;
    private String sex;
    private String nationality;
    private String userProfilePicUrl;
    private List<Ingredient> ingredient_list;
    private List<String> favoriteRecipes;

    public User() {
        //Empty constructor
    }

    public User(String user_id, String name, String username, Date birth_date, String sex
            , String nationality, String userProfilePicUrl, List<Ingredient> ingredient_list
            , List<String> favoriteRecipes) {
        this.user_id = user_id;
        this.name = name;
        this.username = username;
        this.birth_date = birth_date;
        this.sex = sex;
        this.nationality = nationality;
        this.userProfilePicUrl = userProfilePicUrl;
        this.ingredient_list = ingredient_list;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getBirth_date() {
        return birth_date;
    }

    public void setBirth_date(Date birth_date) {
        this.birth_date = birth_date;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getUserProfilePicUrl() {
        return userProfilePicUrl;
    }

    public void setUserProfilePicUrl(String userProfilePicUrl) {
        this.userProfilePicUrl = userProfilePicUrl;
    }

    public List<Ingredient> getIngredient_list() {
        return ingredient_list;
    }

    public void setIngredient_list(List<Ingredient> ingredient_list) {
        this.ingredient_list = ingredient_list;
    }

    public List<String> getFavoriteRecipes() {
        return favoriteRecipes;
    }

    public void setFavoriteRecipes(List<String> favoriteRecipes) {
        this.favoriteRecipes = favoriteRecipes;
    }

    @Override
    public String toString() {
        return "User{" +
                "user_id='" + user_id + '\'' +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", birth_date=" + birth_date +
                ", sex='" + sex + '\'' +
                ", nationality='" + nationality + '\'' +
                ", userProfilePicUrl='" + userProfilePicUrl + '\'' +
                ", ingredient_list=" + ingredient_list +
                ", favoriteRecipes=" + favoriteRecipes +
                '}';
    }
}