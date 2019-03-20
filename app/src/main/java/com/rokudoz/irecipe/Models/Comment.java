package com.rokudoz.irecipe.Models;

public class Comment {
    private String mRecipeDocumentId;
    private String mUserId;
    private String mImageUrl;
    private String mName;
    private String mCommentText;
    private String mCommentTimeStamp;

    public Comment(String mRecipeDocumentId,String mUserId, String mImageUrl, String mName, String mCommentText,String mCommentTimeStamp) {
        this.mRecipeDocumentId = mRecipeDocumentId;
        this.mUserId = mUserId;
        this.mImageUrl = mImageUrl;
        this.mName = mName;
        this.mCommentText = mCommentText;
        this.mCommentTimeStamp = mCommentTimeStamp;
    }

    public Comment() {
    }

    public String getmImageUrl() {
        return mImageUrl;
    }

    public void setmImageUrl(String mImageUrl) {
        this.mImageUrl = mImageUrl;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmCommentText() {
        return mCommentText;
    }

    public void setmCommentText(String mCommentText) {
        this.mCommentText = mCommentText;
    }

    public String getmUserId() {
        return mUserId;
    }

    public void setmUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public String getmRecipeDocumentId() {
        return mRecipeDocumentId;
    }

    public void setmRecipeDocumentId(String mRecipeDocumentId) {
        this.mRecipeDocumentId = mRecipeDocumentId;
    }

    public String getmCommentTimeStamp() {
        return mCommentTimeStamp;
    }

    public void setmCommentTimeStamp(String mCommentTimeStamp) {
        this.mCommentTimeStamp = mCommentTimeStamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "mRecipeDocumentId='" + mRecipeDocumentId + '\'' +
                ", mUserId='" + mUserId + '\'' +
                ", mImageUrl='" + mImageUrl + '\'' +
                ", mName='" + mName + '\'' +
                ", mCommentText='" + mCommentText + '\'' +
                ", mCommentTimeStamp='" + mCommentTimeStamp + '\'' +
                '}';
    }
}
