package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Message {
    private String documentId;
    private String text;
    private String sender_id;
    private String type;

    private @ServerTimestamp
    Date timestamp;

    public Message(String sender_id, String text, String type, Date timestamp) {
        this.sender_id = sender_id;
        this.text = text;
        this.type = type;
        this.timestamp = timestamp;
    }

    public Message() {
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "documentId='" + documentId + '\'' +
                ", text='" + text + '\'' +
                ", sender_id='" + sender_id + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
