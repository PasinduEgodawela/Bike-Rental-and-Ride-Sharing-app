package com.Webapp.BikeRentalSystem.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BookingDTO {

    private String bookingId;
    private String userId;
    @NotBlank(message = "Please enter a bike ID.")
    private String bikeId;
    private String userName;
    private String bikeName;

    @NotNull(message = "Please choose a booking date.")
    private LocalDate bookingDate = LocalDate.now();

    private String status = "Pending";

    public BookingDTO() {
    }

    public BookingDTO(String bookingId, String userId, String bikeId, LocalDate bookingDate, String status) {
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBikeName() {
        return bikeName;
    }

    public void setBikeName(String bikeName) {
        this.bikeName = bikeName;
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
