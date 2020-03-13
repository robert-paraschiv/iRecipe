package com.rokudoz.irecipe.Models;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class ScheduledMeal {
    private String documentID;
    private String recipeID;
    private Date date;
    private String dateString;
    private String mealType;
    private String mealPicture;
    private String mealTitle;

    public ScheduledMeal(String recipeID, Date date, String dateString, String mealType) {
        this.recipeID = recipeID;
        this.date = date;
        this.dateString = dateString;
        this.mealType = mealType;
    }

    public ScheduledMeal() {
    }

    @Exclude
    public String getDocumentID() {
        return documentID;
    }

    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }

    @Exclude
    public String getMealPicture() {
        return mealPicture;
    }

    public void setMealPicture(String mealPicture) {
        this.mealPicture = mealPicture;
    }

    @Exclude
    public String getMealTitle() {
        return mealTitle;
    }

    public void setMealTitle(String mealTitle) {
        this.mealTitle = mealTitle;
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

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
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
                ", dateString='" + dateString + '\'' +
                ", mealType='" + mealType + '\'' +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ScheduledMeal))
            return false;
        if (obj == this)
            return true;
        return this.getDocumentID().equals(((ScheduledMeal) obj).getDocumentID());

    }
}
