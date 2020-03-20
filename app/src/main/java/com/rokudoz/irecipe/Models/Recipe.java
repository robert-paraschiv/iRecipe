package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class Recipe implements Comparable<Recipe> {
    private String documentId;
    private String title;
    private String creator_docId;
    private String creator_name;
    private String creator_imageUrl;
    private String category;
    private String description;
    private List<Ingredient> ingredient_list;
    private List<Instruction> instruction_list;
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
    private Integer number_of_likes;
    private Integer number_of_comments;

    private @ServerTimestamp
    Date creation_date;

    public Recipe() {
        //public no-arg constructor needed
    }

    public Recipe(String title, String creator_docId, String creator_name, String creator_imageUrl, String category, String description
            , List<Ingredient> ingredient_list, List<Instruction> instruction_list, List<String> keywords, List<String> imageUrls_list, String complexity
            , Float duration, String durationType, Float avg_rating, Boolean isFavorite, String privacy, Integer number_of_likes, Integer number_of_comments,Date creation_date) {

        if (title.trim().equals("")) {
            title = "No Title";
        }
        this.title = title;
        this.creator_docId = creator_docId;
        this.creator_name = creator_name;
        this.creator_imageUrl = creator_imageUrl;
        this.category = category;
        this.description = description;
        this.ingredient_list = ingredient_list;
        this.instruction_list = instruction_list;
        this.keywords = keywords;
        this.imageUrls_list = imageUrls_list;
        this.complexity = complexity;
        this.duration = duration;
        this.durationType = durationType;
        this.avg_rating = avg_rating;
        this.isFavorite = isFavorite;
        this.privacy = privacy;
        this.number_of_likes = number_of_likes;
        this.number_of_comments = number_of_comments;
        this.creation_date = creation_date;
    }

    public Integer getNumber_of_likes() {
        return number_of_likes;
    }

    public void setNumber_of_likes(Integer number_of_likes) {
        this.number_of_likes = number_of_likes;
    }

    public Integer getNumber_of_comments() {
        return number_of_comments;
    }

    public void setNumber_of_comments(Integer number_of_comments) {
        this.number_of_comments = number_of_comments;
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

    public String getCreator_name() {
        return creator_name;
    }

    public void setCreator_name(String creator_name) {
        this.creator_name = creator_name;
    }

    public String getCreator_imageUrl() {
        return creator_imageUrl;
    }

    public void setCreator_imageUrl(String creator_imageUrl) {
        this.creator_imageUrl = creator_imageUrl;
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

    public List<Ingredient> getIngredient_list() {
        return ingredient_list;
    }

    public void setIngredient_list(List<Ingredient> ingredient_list) {
        this.ingredient_list = ingredient_list;
    }

    public List<Instruction> getInstruction_list() {
        return instruction_list;
    }

    public void setInstruction_list(List<Instruction> instruction_list) {
        this.instruction_list = instruction_list;
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

    public Date getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(Date creation_date) {
        this.creation_date = creation_date;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "documentId='" + documentId + '\'' +
                ", title='" + title + '\'' +
                ", creator_docId='" + creator_docId + '\'' +
                ", creator_name='" + creator_name + '\'' +
                ", creator_imageUrl='" + creator_imageUrl + '\'' +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", ingredient_list=" + ingredient_list +
                ", instruction_list=" + instruction_list +
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
                ", number_of_likes=" + number_of_likes +
                ", number_of_comments=" + number_of_comments +
                ", creation_date=" + creation_date +
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