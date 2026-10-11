package com.herak.bouldershare.classes;

import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Herak on 27.4.2017..
 */

public class BoulderProblemInfo {
    private Long id;
    private String author;
    private String name;
    private String comment;
    private String grade;
    private Uri inputBitmapUri;
    private Uri finalBitmapUri;
    private List<Hold> holds;
    private int referenceWidth;
    private int referenceHeight;

    public int getReferenceWidth() { return referenceWidth; }
    public int getReferenceHeight() { return referenceHeight; }
    public void setReferenceSize(int width, int height) {
        referenceWidth = width;
        referenceHeight = height;
    }

    /** A small, bitmap-free snapshot used across activity recreation and IO. */
    public Bundle toBundle() {
        Bundle state = new Bundle();
        if (id != null) state.putLong("id", id);
        state.putString("author", author);
        state.putString("name", name);
        state.putString("comment", comment);
        state.putString("grade", grade);
        state.putString("input", inputBitmapUri == null ? null : inputBitmapUri.toString());
        state.putString("output", finalBitmapUri == null ? null : finalBitmapUri.toString());
        state.putInt("width", referenceWidth);
        state.putInt("height", referenceHeight);
        ArrayList<Bundle> markers = new ArrayList<>();
        if (holds != null) for (Hold hold : holds) {
            Bundle marker = new Bundle();
            marker.putFloat("x", hold.getX());
            marker.putFloat("y", hold.getY());
            marker.putInt("radius", hold.getCircleRadius());
            marker.putString("type", hold.getType().name());
            marker.putLong("id", hold.getId());
            marker.putLong("boulderId", hold.getBoulderId());
            markers.add(marker);
        }
        state.putParcelableArrayList("holds", markers);
        return state;
    }

    public static BoulderProblemInfo fromBundle(Bundle state) {
        if (state == null) return null;
        BoulderProblemInfo info = new BoulderProblemInfo(state.getString("author"),
                state.getString("name"), state.getString("comment"), state.getString("grade"));
        if (state.containsKey("id")) info.setId(state.getLong("id"));
        if (state.getString("input") != null) info.setInputBitmapUri(Uri.parse(state.getString("input")));
        if (state.getString("output") != null) info.setFinalBitmapUri(Uri.parse(state.getString("output")));
        info.setReferenceSize(state.getInt("width"), state.getInt("height"));
        List<Hold> holds = new ArrayList<>();
        ArrayList<Bundle> markers = state.getParcelableArrayList("holds");
        if (markers != null) for (Bundle marker : markers) {
            Hold hold = new Hold(marker.getFloat("x"), marker.getFloat("y"), marker.getInt("radius"));
            hold.setType(com.herak.bouldershare.enums.HoldType.valueOf(marker.getString("type")));
            hold.setId(marker.getLong("id"));
            hold.setBoulderId(marker.getLong("boulderId"));
            holds.add(hold);
        }
        info.setHolds(holds);
        return info;
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Uri getFinalBitmapUri() {
        return finalBitmapUri;
    }

    public void setFinalBitmapUri(Uri finalBitmapUri) {
        this.finalBitmapUri = finalBitmapUri;
    }




}
