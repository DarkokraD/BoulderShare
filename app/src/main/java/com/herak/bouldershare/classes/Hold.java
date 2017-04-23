package com.herak.bouldershare.classes;

import com.herak.bouldershare.enums.HoldType;

/**
 * Created by darko on 23.4.2017..
 */

class Hold {
    float x, y;
    HoldType type;

    public void setType(HoldType type){
        this.type = type;
    }

    public double distanceFrom(Hold hold){
        return Math.sqrt(Math.pow(this.x - hold.x, 2) + Math.pow(this.y - hold.y, 2));
    }
}
