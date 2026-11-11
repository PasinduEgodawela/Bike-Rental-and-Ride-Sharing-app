package com.Webapp.BikeRentalSystem.DTO;

public class ReviewResponse {

    private int status;
    private String message;
    private ReviewDTO review;
    private ReviewDTO[] reviews;
    private double averageRating;

    public ReviewResponse() {
    }

    public ReviewResponse(int status, String message, ReviewDTO review, ReviewDTO[] reviews, double averageRating) {
        this.status = status;
        this.message = message;
        this.review = review;
        this.reviews = reviews;
        this.averageRating = averageRating;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ReviewDTO getReview() { return review; }
    public void setReview(ReviewDTO review) { this.review = review; }

    public ReviewDTO[] getReviews() { return reviews; }
    public void setReviews(ReviewDTO[] reviews) { this.reviews = reviews; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public static ReviewResponse message(int status, String message) {
        return new ReviewResponse(status, message, null, null, 0);
    }
}