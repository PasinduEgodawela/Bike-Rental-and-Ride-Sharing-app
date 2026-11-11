package com.Webapp.BikeRentalSystem.Filehandling;

import java.util.List;

/**
 * Makes the next ID for a file such as bookings.txt.
 * The next ID is the highest number found + 1. It looks at every line, even a
 * line that is damaged, so a new record never reuses an ID that is already in the file.
 */
public class RecordIds {

    // prefix "BID" and lines "BID001|...", "BID002|..." gives "BID003"
    public static String next(String prefix, List<String> rawLines) {
        int highest = 0;

        for (String line : rawLines) {
            String id = line.split("\\|", -1)[0].trim();
            if (id.startsWith(prefix)) {
                try {
                    int number = Integer.parseInt(id.substring(prefix.length()));
                    if (number > highest) {
                        highest = number;
                    }
                } catch (NumberFormatException e) {
                    // not an ID of this kind, so it is not counted
                }
            }
        }

        return String.format("%s%03d", prefix, highest + 1);
    }
}
