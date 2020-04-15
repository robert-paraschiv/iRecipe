package com.rokudoz.irecipe.Models;

import java.util.List;
import java.util.Objects;

public class IngredientCategory {
    String categoryName;
    List<Ingredient> ingredientList;

    public IngredientCategory(String categoryName, List<Ingredient> ingredientList) {
        this.categoryName = categoryName;
        this.ingredientList = ingredientList;
    }

    public IngredientCategory() {
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<Ingredient> getIngredientList() {
        return ingredientList;
    }

    public void setIngredientList(List<Ingredient> ingredientList) {
        this.ingredientList = ingredientList;
    }

    @Override
    public String toString() {
        return "IngredientCategory{" +
                "categoryName='" + categoryName + '\'' +
                ", ingredientList=" + ingredientList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngredientCategory that = (IngredientCategory) o;
        return Objects.equals(categoryName, that.categoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryName);
    }
}
