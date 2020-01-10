package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;

import java.util.List;

public class TestRecipe {
    private String documentId;
    private String title;
    private String creator_docId;
    private String category;
    private String description;
    private List<String> keywords;
    private List<String> imageUrls_list;
    private List<Ingredient> ingredients_list;
    private List<Instruction> instructions_list;
    private List<String> ratings_docId_list;
    private Float avg_rating;
    private Boolean isFavorite;


    public TestRecipe() {
        //public no-arg constructor needed
    }

    public TestRecipe(String title, String creator_docId, String category, String description, List<String> keywords
            , List<String> imageUrls_list, List<Ingredient> ingredients_list, List<Instruction> instructions_list
            , List<String> ratings_docId_list, Float avg_rating, Boolean isFavorite) {

        if (title.trim().equals("")) {
            title = "No Title";
        }
        this.title = title;
        this.creator_docId = creator_docId;
        this.category = category;
        this.description = description;
        this.keywords = keywords;
        this.imageUrls_list = imageUrls_list;
        this.ingredients_list = ingredients_list;
        this.instructions_list = instructions_list;
        this.ratings_docId_list = ratings_docId_list;
        this.avg_rating = avg_rating;
        this.isFavorite = isFavorite;
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

    public List<Ingredient> getIngredients_list() {
        return ingredients_list;
    }

    public void setIngredients_list(List<Ingredient> ingredients_list) {
        this.ingredients_list = ingredients_list;
    }

    public List<Instruction> getInstructions_list() {
        return instructions_list;
    }

    public void setInstructions_list(List<Instruction> instructions_list) {
        this.instructions_list = instructions_list;
    }

    public List<String> getRatings_docId_list() {
        return ratings_docId_list;
    }

    public void setRatings_docId_list(List<String> ratings_docId_list) {
        this.ratings_docId_list = ratings_docId_list;
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

    @Override
    public String toString() {
        return "TestRecipe{" +
                "documentId='" + documentId + '\'' +
                ", title='" + title + '\'' +
                ", creator_docId='" + creator_docId + '\'' +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", keywords=" + keywords +
                ", imageUrls_list=" + imageUrls_list +
                ", ingredients_list=" + ingredients_list +
                ", instructions_list=" + instructions_list +
                ", ratings_docId_list=" + ratings_docId_list +
                ", avg_rating=" + avg_rating +
                ", isFavorite=" + isFavorite +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestRecipe))
            return false;
        if (obj == this)
            return true;
        return this.title.equals(((TestRecipe) obj).title);
    }
}