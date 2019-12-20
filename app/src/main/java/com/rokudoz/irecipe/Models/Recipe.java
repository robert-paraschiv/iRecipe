package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;

import java.util.List;
import java.util.Map;

public class Recipe {
    private String documentId;
    private String title;
    private String category;
    private String description;
    private Map<String, Boolean> tags;
    private List<String> imageUrl;
    private Boolean isFavorite;
    private List<String> ingredient_array;
    private String instructions;
    private Integer numberOfFaves;

    public Recipe() {
        //public no-arg constructor needed
    }

    public Recipe(String title, String category, String description
            , Map<String, Boolean> tags, List<String> imageUrl
            , Boolean isFavorite, List<String> ingredient_array
            , String instructions, Integer numberOfFaves) {

        if (title.trim().equals("")) {
            title = "No Title";
        }
        this.title = title;
        this.category = category;
        this.description = description;
        this.tags = tags;
        this.imageUrl = imageUrl;
        this.isFavorite = isFavorite;
        this.ingredient_array = ingredient_array;
        this.instructions = instructions;
        this.numberOfFaves = numberOfFaves;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public List<String> getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(List<String> imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getFavorite() {
        return isFavorite;
    }

    public List<String> getIngredient_array() {
        return ingredient_array;
    }

    public void setIngredient_array(List<String> ingredient_array) {
        this.ingredient_array = ingredient_array;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setFavorite(Boolean favorite) {
        isFavorite = favorite;
    }

    public Integer getNumberOfFaves() {
        return numberOfFaves;
    }

    public void setNumberOfFaves(Integer numberOfFaves) {
        this.numberOfFaves = numberOfFaves;
    }
}