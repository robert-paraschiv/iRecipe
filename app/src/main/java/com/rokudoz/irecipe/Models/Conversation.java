package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Conversation {
    private String userId;
    private String user_name;
    private String user_profilePic;
    private String text;
    private String type;
    private Boolean read;

    @ServerTimestamp
    private
    Date date;

    public Conversation(String userId, String user_name,String user_profilePic,String text, String type, Date date, Boolean read) {
        this.userId = userId;
        this.user_name = user_name;
        this.user_profilePic = user_profilePic;
        this.text = text;
        this.type = type;
        this.date = date;
        this.read = read;
    }

    public Conversation() {
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_profilePic() {
        return user_profilePic;
    }

    public void setUser_profilePic(String user_profilePic) {
        this.user_profilePic = user_profilePic;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "userId='" + userId + '\'' +
                ", user_name='" + user_name + '\'' +
                ", user_profilePic='" + user_profilePic + '\'' +
                ", text='" + text + '\'' +
                ", type='" + type + '\'' +
                ", read=" + read +
                ", date=" + date +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Conversation))
            return false;
        if (obj == this)
            return true;
        return this.userId.equals(((Conversation) obj).userId);
    }
}
