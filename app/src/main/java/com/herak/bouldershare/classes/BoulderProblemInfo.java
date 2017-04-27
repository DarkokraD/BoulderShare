package com.herak.bouldershare.classes;

/**
 * Created by Herak on 27.4.2017..
 */

public class BoulderProblemInfo {
    private String author;
    private String name;
    private String comment;
    private String grade;

    public BoulderProblemInfo(){

    }

    public BoulderProblemInfo(String author, String name, String comment, String grade) {
        this.author = author;
        this.name = name;
        this.comment = comment;
        this.grade = grade;
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
