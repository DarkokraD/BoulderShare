package com.herak.bouldershare.enums;

/**
 * Created by Herak on 27.4.2017..
 */

public enum Grade {
    UNKNOWN("Unknown"),
    F4("4"),
    F5("5"),
    F6A("6A"), F6AP("6A+"), F6B("6B"), F6BP("6B+"), F6C("6C"), F6CP("6C+"),
    F7A("7A"), F7AP("7A+"), F7B("7B"), F7BP("7B+"), F7C("7C"), F7CP("7C+"),
    F8A("8A"), F8AP("8A+"), F8B("8B"), F8BP("8B+"), F8C("8C"), F8CP("8C+");

    private String val;

    Grade(String val){
        this.val = val;
    }

    public String getVal(){
        return val;
    }

}





