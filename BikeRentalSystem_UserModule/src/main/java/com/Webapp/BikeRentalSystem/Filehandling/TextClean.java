package com.Webapp.BikeRentalSystem.Filehandling;


public class TextClean {


    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('|', '/')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }
}
