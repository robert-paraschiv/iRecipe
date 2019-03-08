package com.rokudoz.irecipe;

import com.google.firebase.firestore.Exclude;

public class Note {
    private String documentId;
    private String title;
    private String description;
    private int priority;
    private int contPotatos;

    public Note() {
        //public no-arg constructor needed
    }

    public Note(String title, String description, int priority, int contPotatos) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.contPotatos = contPotatos;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public int getContPotatos() {
        return contPotatos;
    }

    public void setContPotatos(int contPotatos) {
        this.contPotatos = contPotatos;
    }
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}