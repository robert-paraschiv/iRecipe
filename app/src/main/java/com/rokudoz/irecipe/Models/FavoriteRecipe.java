package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class FavoriteRecipe {
    private String documentId;

    private @ServerTimestamp
    Date timeStamp;

    public FavoriteRecipe(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}
