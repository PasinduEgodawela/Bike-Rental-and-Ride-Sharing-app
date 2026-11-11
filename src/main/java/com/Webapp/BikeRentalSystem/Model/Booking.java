package com.Webapp.BikeRentalSystem.Model;

import java.time.LocalDate;

public class Booking {

    private String bookingId;
    private String userId;
    private String bikeId;
    private LocalDate bookingDate;
    private String status;

    public Booking() {
    }

    public Booking(String bookingId, String userId, String bikeId, LocalDate bookingDate,  String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.bikeId = bikeId;
        this.bookingDate = bookingDate;
        this.status = status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBikeId() {
        return bikeId;
    }

    public void setBikeId(String bikeId) {
        this.bikeId = bikeId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }



}
