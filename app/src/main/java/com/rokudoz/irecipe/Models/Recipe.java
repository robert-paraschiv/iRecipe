package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;

import java.util.Map;

public class Recipe {
    private String documentId;
    private String title;
    private String description;
    private Map<String, Boolean> tags;

    public Recipe() {
        //public no-arg constructor needed
    }

    public Recipe(String title, String description, Map<String, Boolean> tags) {
        this.title = title;
        this.description = description;
        this.tags = tags;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Boolean> getTags() {
        return tags;
    }
}