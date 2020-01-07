package com.rokudoz.irecipe.Models;

import com.google.firebase.firestore.Exclude;

import java.util.Map;

public class Ingredient {
    private String documentId;
    private String name;
    private Float quantity;
    private String quantity_type;
    private Boolean owned;

    public Ingredient() {
        //public no-arg constructor needed
    }

    public Ingredient(String name, Float quantity, String quantity_type,Boolean owned) {
        this.name = name;
        this.quantity = quantity;
        this.quantity_type = quantity_type;
        this.owned = owned;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getQuantity() {
        return quantity;
    }

    public void setQuantity(Float quantity) {
        this.quantity = quantity;
    }

    public String getQuantity_type() {
        return quantity_type;
    }

    public void setQuantity_type(String quantity_type) {
        this.quantity_type = quantity_type;
    }

    public Boolean getOwned() {
        return owned;
    }

    public void setOwned(Boolean owned) {
        this.owned = owned;
    }

    @Override
    public String toString() {
        return "Ingredient{" +
                "name='" + name + '\'' +
                ", quantity=" + quantity +
                ", quantity_type='" + quantity_type + '\'' +
                ", owned=" + owned +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Ingredient))
            return false;
        if (obj == this)
            return true;
        return this.name.equals(((Ingredient) obj).name);
    }
}