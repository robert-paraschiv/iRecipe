package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Comment {
    private String documentID;
    private String recipe_documentID;
    private String user_id;
    private String comment_text;

    private @ServerTimestamp
    Date comment_timeStamp;


    public Comment(String recipe_documentID, String user_id, String comment_text, Date comment_timeStamp) {
        this.recipe_documentID = recipe_documentID;
        this.user_id = user_id;
        this.comment_text = comment_text;
        this.comment_timeStamp = comment_timeStamp;
    }

    public Comment() {
    }

    @Exclude
    public String getDocumentID() {
        return documentID;
    }

    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }

    public String getRecipe_documentID() {
        return recipe_documentID;
    }

    public void setRecipe_documentID(String recipe_documentID) {
        this.recipe_documentID = recipe_documentID;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getComment_text() {
        return comment_text;
    }

    public void setComment_text(String comment_text) {
        this.comment_text = comment_text;
    }

    public Date getComment_timeStamp() {
        return comment_timeStamp;
    }

    public void setComment_timeStamp(Date comment_timeStamp) {
        this.comment_timeStamp = comment_timeStamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "documentID='" + documentID + '\'' +
                ", recipe_documentID='" + recipe_documentID + '\'' +
                ", user_id='" + user_id + '\'' +
                ", comment_text='" + comment_text + '\'' +
                ", comment_timeStamp=" + comment_timeStamp +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Comment))
            return false;
        if (obj == this)
            return true;
        return this.documentID.equals(((Comment) obj).documentID);
    }
}
