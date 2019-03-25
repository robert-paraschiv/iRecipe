package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class UserWhoFaved {

    private String userID;

    private @ServerTimestamp
    Date mFaveTimestamp;

    public UserWhoFaved(String userID, Date mFaveTimestamp) {
        this.userID = userID;
        this.mFaveTimestamp = mFaveTimestamp;
    }

    public UserWhoFaved() {}

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public Date getmFaveTimestamp() {
        return mFaveTimestamp;
    }

    public void setmFaveTimestamp(Date mFaveTimestamp) {
        this.mFaveTimestamp = mFaveTimestamp;
    }
}
