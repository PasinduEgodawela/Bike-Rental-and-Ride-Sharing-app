package com.Webapp.BikeRentalSystem.Filehandling;

/**
 * Every record is stored as one line with fields separated by "|".
 * If a user types "|" or a line break into a form, it would break the line,
 * so this class removes them before anything is written to a file.
 */
public class TextClean {

    // Replaces "|" with "/" and line breaks with a space
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
