package com.rokudoz.irecipe.Models;


import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Post {
    private String documentId;
    private String referenced_recipe_docId;
    private String creatorId;
    private String text;
    private String imageUrl;
    private String privacy;

    private @ServerTimestamp
    Date creation_date;

    public Post(String referenced_recipe_docId, String creatorId, String text, String imageUrl, String privacy, Date creation_date) {
        this.referenced_recipe_docId = referenced_recipe_docId;
        this.creatorId = creatorId;
        this.text = text;
        this.imageUrl = imageUrl;
        this.privacy = privacy;
        this.creation_date = creation_date;
    }

    public Post() {
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getReferenced_recipe_docId() {
        return referenced_recipe_docId;
    }

    public void setReferenced_recipe_docId(String referenced_recipe_docId) {
        this.referenced_recipe_docId = referenced_recipe_docId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(Date creation_date) {
        this.creation_date = creation_date;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }


    @Override
    public String toString() {
        return "Post{" +
                "documentId='" + documentId + '\'' +
                ", referenced_recipe_docId='" + referenced_recipe_docId + '\'' +
                ", creatorId='" + creatorId + '\'' +
                ", text='" + text + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", privacy='" + privacy + '\'' +
                ", creation_date=" + creation_date +
                '}';
    }
}
