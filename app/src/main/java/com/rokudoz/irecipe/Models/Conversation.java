package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Conversation {
    private String userId;
    private String text;
    private String type;
    private Boolean read;

    @ServerTimestamp
    private
    Date date;

    public Conversation(String userId, String text, String type, Date date, Boolean read) {
        this.userId = userId;
        this.text = text;
        this.type = type;
        this.date = date;
        this.read = read;
    }

    public Conversation() {
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
