package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;

import java.util.List;

public class Recipe implements Comparable<Recipe> {
    private String documentId;
    private String title;
    private String creator_docId;
    private String category;
    private String description;
    private List<String> keywords;
    private List<String> imageUrls_list;
    private String complexity;
    private Float duration;
    private String durationType;
    private Float avg_rating;
    private Boolean isFavorite;
    private String privacy;
    private Integer nrOfMissingIngredients;
    private List<String> missingIngredients;
    private Integer nrOfLikes;
    private Integer nrOfComments;


    public Recipe() {
        //public no-arg constructor needed
    }

    public Recipe(String title, String creator_docId, String category, String description, List<String> keywords
            , List<String> imageUrls_list, String complexity, Float duration, String durationType, Float avg_rating, Boolean isFavorite, String privacy) {

        if (title.trim().equals("")) {
            title = "No Title";
        }
        this.title = title;
        this.creator_docId = creator_docId;
        this.category = category;
        this.description = description;
        this.keywords = keywords;
        this.imageUrls_list = imageUrls_list;
        this.complexity = complexity;
        this.duration = duration;
        this.durationType = durationType;
        this.avg_rating = avg_rating;
        this.isFavorite = isFavorite;
        this.privacy = privacy;
    }

    @Exclude
    public Integer getNrOfLikes() {
        return nrOfLikes;
    }

    public void setNrOfLikes(Integer nrOfLikes) {
        this.nrOfLikes = nrOfLikes;
    }

    @Exclude
    public Integer getNrOfComments() {
        return nrOfComments;
    }

    public void setNrOfComments(Integer nrOfComments) {
        this.nrOfComments = nrOfComments;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    @Exclude
    public Integer getNrOfMissingIngredients() {
        return nrOfMissingIngredients;
    }

    public void setNrOfMissingIngredients(Integer nrOfMissingIngredients) {
        this.nrOfMissingIngredients = nrOfMissingIngredients;
    }

    @Exclude
    public List<String> getMissingIngredients() {
        return missingIngredients;
    }

    public void setMissingIngredients(List<String> missingIngredients) {
        this.missingIngredients = missingIngredients;
    }

    public String getComplexity() {
        return complexity;
    }

    public void setComplexity(String complexity) {
        this.complexity = complexity;
    }

    public Float getDuration() {
        return duration;
    }

    public void setDuration(Float duration) {
        this.duration = duration;
    }

    public String getDurationType() {
        return durationType;
    }

    public void setDurationType(String durationType) {
        this.durationType = durationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreator_docId() {
        return creator_docId;
    }

    public void setCreator_docId(String creator_docId) {
        this.creator_docId = creator_docId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getImageUrls_list() {
        return imageUrls_list;
    }

    public void setImageUrls_list(List<String> imageUrls_list) {
        this.imageUrls_list = imageUrls_list;
    }

    public Float getAvg_rating() {
        return avg_rating;
    }

    public void setAvg_rating(Float avg_rating) {
        this.avg_rating = avg_rating;
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
        return "Recipe{" +
                "documentId='" + documentId + '\'' +
                ", title='" + title + '\'' +
                ", creator_docId='" + creator_docId + '\'' +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", keywords=" + keywords +
                ", imageUrls_list=" + imageUrls_list +
                ", complexity='" + complexity + '\'' +
                ", duration=" + duration +
                ", durationType='" + durationType + '\'' +
                ", avg_rating=" + avg_rating +
                ", isFavorite=" + isFavorite +
                ", privacy='" + privacy + '\'' +
                ", nrOfMissingIngredients=" + nrOfMissingIngredients +
                ", missingIngredients=" + missingIngredients +
                ", nrOfLikes=" + nrOfLikes +
                ", nrOfComments=" + nrOfComments +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Recipe))
            return false;
        if (obj == this)
            return true;
        return this.documentId.equals(((Recipe) obj).documentId);
    }

    @Override
    public int compareTo(Recipe o) {
        return this.getNrOfMissingIngredients().compareTo(o.getNrOfMissingIngredients());
    }
}