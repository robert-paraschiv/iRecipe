package com.rokudoz.irecipe.Models;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class ScheduleEvent {
    private String documentID;
    private String recipeID;
    private Date date;
    private String mealType;

    public ScheduleEvent(String recipeID, Date date, String mealType) {
        this.recipeID = recipeID;
        this.date = date;
        this.mealType = mealType;
    }

    public ScheduleEvent() {
    }

    @Exclude
    public String getDocumentID() {
        return documentID;
    }

    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }

    public String getRecipeID() {
        return recipeID;
    }

    public void setRecipeID(String recipeID) {
        this.recipeID = recipeID;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    @Override
    public String toString() {
        return "ScheduleEvent{" +
                "documentID='" + documentID + '\'' +
                ", recipeID='" + recipeID + '\'' +
                ", date=" + date +
                ", mealType='" + mealType + '\'' +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ScheduleEvent))
            return false;
        if (obj == this)
            return true;
        return this.getDocumentID().equals(((ScheduleEvent) obj).getDocumentID());

    }
}
