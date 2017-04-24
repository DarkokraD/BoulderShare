package com.herak.bouldershare.classes;

import com.herak.bouldershare.enums.HoldType;

/**
 * Created by darko on 23.4.2017..
 */

class Hold {
    float x, y;
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
