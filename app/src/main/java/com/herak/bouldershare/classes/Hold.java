package com.herak.bouldershare.classes;

import com.herak.bouldershare.enums.HoldType;

/**
 * Created by darko on 23.4.2017..
 */

public class Hold {
    long id, boulderId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBoulderId() {
        return boulderId;
    }

    public void setBoulderId(long boulderId) {
        this.boulderId = boulderId;
    }

    float x, y;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public HoldType getType() {
        return type;
    }

    public int getCircleRadius() {
        return circleRadius;
    }

    public void setCircleRadius(int circleRadius) {
        this.circleRadius = circleRadius;
    }

    HoldType type;
    int circleRadius;

    public Hold(float x, float y){
        this.x = x;
        this.y = y;
    }

    public Hold(float x, float y, int circleRadius){
        this.x = x;
        this.y = y;
        this.circleRadius = circleRadius;
    }

    public void setType(HoldType type){
        this.type = type;
    }

    public double distanceFrom(Hold hold){
        return Math.sqrt(Math.pow(this.x - hold.x, 2) + Math.pow(this.y - hold.y, 2));
    }


}
