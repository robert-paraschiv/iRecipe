package com.rokudoz.irecipe.Models;

import java.util.List;

public class Instruction {
    private Integer stepNumber;
    private String text;
    private String imgUrl;

    public Instruction(Integer stepNumber, String text, String imgUrl) {
        this.stepNumber = stepNumber;
        this.text = text;
        this.imgUrl = imgUrl;
    }

    public Instruction() {
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    @Override
    public String toString() {
        return "Instruction{" +
                "stepNumber=" + stepNumber +
                ", text='" + text + '\'' +
                ", imgUrl='" + imgUrl + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Instruction))
            return false;
        if (obj == this)
            return true;
        return this.text.equals(((Instruction) obj).text);
    }
}
