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
    private Boolean isFavorite;
    private String privacy;
    private String creator_name;
    private String creator_imageUrl;
    private Integer number_of_comments;
    private Integer number_of_likes;

    private @ServerTimestamp
    Date creation_date;

    public Post(String referenced_recipe_docId, String creatorId, String text, String imageUrl, Boolean isFavorite, String privacy, Date creation_date) {
        this.referenced_recipe_docId = referenced_recipe_docId;
        this.creatorId = creatorId;
        this.text = text;
        this.imageUrl = imageUrl;
        this.isFavorite = isFavorite;
        this.privacy = privacy;
        this.creation_date = creation_date;
    }

    public Post() {
    }
    @Exclude
    public String getCreator_name() {
        return creator_name;
    }

    public void setCreator_name(String creator_name) {
        this.creator_name = creator_name;
    }
    @Exclude
    public String getCreator_imageUrl() {
        return creator_imageUrl;
    }

    public void setCreator_imageUrl(String creator_imageUrl) {
        this.creator_imageUrl = creator_imageUrl;
    }
    @Exclude
    public Integer getNumber_of_comments() {
        return number_of_comments;
    }
    @Exclude
    public void setNumber_of_comments(Integer number_of_comments) {
        this.number_of_comments = number_of_comments;
    }
    @Exclude
    public Integer getNumber_of_likes() {
        return number_of_likes;
    }

    public void setNumber_of_likes(Integer number_of_likes) {
        this.number_of_likes = number_of_likes;
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

    public Boolean getFavorite() {
        return isFavorite;
    }

    public void setFavorite(Boolean favorite) {
        isFavorite = favorite;
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
                ", isFavorite=" + isFavorite +
                ", privacy='" + privacy + '\'' +
                ", creation_date=" + creation_date +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Post))
            return false;
        if (obj == this)
            return true;
        return this.documentId.equals(((Post) obj).documentId);
    }
}
