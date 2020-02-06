package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class UserWhoFaved {

    private String userID;
    private String user_name;
    private String user_imageUrl;

    private @ServerTimestamp
    Date mFaveTimestamp;

    public UserWhoFaved(String userID, String user_name, String user_imageUrl, Date mFaveTimestamp) {
        this.userID = userID;
        this.user_name = user_name;
        this.user_imageUrl = user_imageUrl;
        this.mFaveTimestamp = mFaveTimestamp;
    }

    public UserWhoFaved() {
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_imageUrl() {
        return user_imageUrl;
    }

    public void setUser_imageUrl(String user_imageUrl) {
        this.user_imageUrl = user_imageUrl;
    }

    public Date getmFaveTimestamp() {
        return mFaveTimestamp;
    }

    public void setmFaveTimestamp(Date mFaveTimestamp) {
        this.mFaveTimestamp = mFaveTimestamp;
    }
}
