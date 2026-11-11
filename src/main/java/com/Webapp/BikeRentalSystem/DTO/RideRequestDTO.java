package com.Webapp.BikeRentalSystem.DTO;

import java.time.LocalDate;

public class RideRequestDTO {

    private String requestId;
    private String bookingId;
    private String passengerId;     // real user ID, e.g. U002
    private String bikeId;          // real bike ID, e.g. B001
    private String rideId;          // the ride the passenger wants a seat on
    private LocalDate requestDate;
    private String status;
    private String passengerName;   // only for showing on pages, never stored
    private String bikeName;        // only for showing on pages, never stored

    public RideRequestDTO() {
    }

    public RideRequestDTO(String requestId, String bookingId, String passengerId, String bikeId,
                          LocalDate requestDate, String status) {
        this.requestId = requestId;
        this.bookingId = bookingId;
        this.passengerId = passengerId;
        this.bikeId = bikeId;
        this.requestDate = requestDate;
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getBikeId() {
        return bikeId;
    }

    public void setBikeId(String bikeId) {
        this.bikeId = bikeId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getBikeName() {
        return bikeName;
    }

    public void setBikeName(String bikeName) {
        this.bikeName = bikeName;
    }
}
