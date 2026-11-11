
package com.Webapp.BikeRentalSystem.DTO;

public class RideRequestResponse {

    private int status;
    private String message;
    private RideRequestDTO rideRequest;
    private RideRequestDTO[] rideRequests;

    public RideRequestResponse() {
    }

    public RideRequestResponse(int status, String message,
                               RideRequestDTO rideRequest,
                               RideRequestDTO[] rideRequests) {
        this.status = status;
        this.message = message;
        this.rideRequest = rideRequest;
        this.rideRequests = rideRequests;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RideRequestDTO getRideRequest() {
        return rideRequest;
    }

    public void setRideRequest(RideRequestDTO rideRequest) {
        this.rideRequest = rideRequest;
    }

    public RideRequestDTO[] getRideRequests() {
        return rideRequests;
    }

    public void setRideRequests(RideRequestDTO[] rideRequests) {
        this.rideRequests = rideRequests;
    }

    public static RideRequestResponse message(int status, String message) {
        return new RideRequestResponse(status, message, null, null);
    }
}
