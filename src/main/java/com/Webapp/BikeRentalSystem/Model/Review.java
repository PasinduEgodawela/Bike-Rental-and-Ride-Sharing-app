package com.Webapp.BikeRentalSystem.Model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class Review {
    private String reviewId;
    private String userId;
    private String bikeId;
    private int rating;
    private String comment;
    private LocalDate reviewDate;

    public Review() { }

    public Review(String reviewId, String userId, String bikeId, int rating, String comment, LocalDate reviewDate) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.bikeId = bikeId;
        this.rating = rating;
        this.comment = comment;
        this.reviewDate = reviewDate;
    }

    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getBikeId() { return bikeId; }
    public void setBikeId(String bikeId) { this.bikeId = bikeId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDate getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDate reviewDate) { this.reviewDate = reviewDate; }

    public String toFileString() {
        return String.join("|", safe(reviewId), safe(userId), safe(bikeId), Integer.toString(rating),
                safe(comment), reviewDate == null ? "" : reviewDate.toString());
    }

    public static Review fromFileString(String line) {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 6) throw new IllegalArgumentException("Review record must contain six fields");
        try {
            return new Review(fields[0], fields[1], fields[2], Integer.parseInt(fields[3]), fields[4],
                    LocalDate.parse(fields[5]));
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new IllegalArgumentException("Review record contains invalid values", ex);
        }
    }

    private static String safe(String value) {
        return Objects.toString(value, "").replace('|', '/').replace('\r', ' ').replace('\n', ' ');
    }
}
