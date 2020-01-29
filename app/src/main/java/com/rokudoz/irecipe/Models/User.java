package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;

import java.util.Calendar;
import java.util.List;

public class User {
    private String user_id;
    private String name;
    private String username;
    private String email;
    private String description;
    private String gender;
    private String nationality;
    private String userProfilePicUrl;
    private String user_tokenID;

    public User() {
        //Empty constructor
    }

    public User(String user_id, String name, String username, String email, String description, String gender
            , String nationality, String userProfilePicUrl) {
        this.user_id = user_id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.description = description;
        this.gender = gender;
        this.nationality = nationality;
        this.userProfilePicUrl = userProfilePicUrl;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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


    public String getUser_tokenID() {
        return user_tokenID;
    }

    public void setUser_tokenID(String user_tokenID) {
        this.user_tokenID = user_tokenID;
    }

    @Override
    public String toString() {
        return "User{" +
                "user_id='" + user_id + '\'' +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", description='" + description + '\'' +
                ", gender='" + gender + '\'' +
                ", nationality='" + nationality + '\'' +
                ", userProfilePicUrl='" + userProfilePicUrl + '\'' +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof User))
            return false;
        if (obj == this)
            return true;
        return this.getUser_id().equals(((User) obj).getUser_id());
    }
}