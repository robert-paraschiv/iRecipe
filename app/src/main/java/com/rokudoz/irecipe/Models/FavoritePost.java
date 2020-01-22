package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class FavoritePost {
    private String documentId;

    private @ServerTimestamp
    Date mCommentTimeStamp;

    public FavoritePost(Date mCommentTimeStamp) {
        this.mCommentTimeStamp = mCommentTimeStamp;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Date getmCommentTimeStamp() {
        return mCommentTimeStamp;
    }

    public void setmCommentTimeStamp(Date mCommentTimeStamp) {
        this.mCommentTimeStamp = mCommentTimeStamp;
    }
}
