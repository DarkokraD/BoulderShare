package com.herak.bouldershare.classes;

import android.net.Uri;

import java.util.List;

/**
 * Created by Herak on 27.4.2017..
 */

public class BoulderProblemInfo {
    private String author;
    private String name;
    private String comment;
    private String grade;
    private Uri inputBitmapUri;
    private List<Hold> holds;

    public BoulderProblemInfo(){

    }

    public BoulderProblemInfo(String author, String name, String comment, String grade) {
        this.author = author;
        this.name = name;
        this.comment = comment;
        this.grade = grade;
    }

    public List<Hold> getHolds() {
        return holds;
    }

    public void setHolds(List<Hold> holds) {
        this.holds = holds;
    }

    public Uri getInputBitmapUri() {
        return inputBitmapUri;
    }

    public void setInputBitmapUri(Uri inputBitmapUri) {
        this.inputBitmapUri = inputBitmapUri;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }
}
