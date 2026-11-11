package com.Webapp.BikeRentalSystem.Model;

import com.Webapp.BikeRentalSystem.Filehandling.TextClean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class Ride {
    private String rideId, driverId, bikeId, startLocation, destination, status;
    private LocalDate rideDate;
    private LocalTime rideTime;
    private int availableSeats;

    public Ride() {}

    public Ride(String rideId, String driverId, String bikeId, String startLocation,
                String destination, LocalDate rideDate, LocalTime rideTime,
                int availableSeats, String status) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.bikeId = bikeId;
        this.startLocation = startLocation;
        this.destination = destination;
        this.rideDate = rideDate;
        this.rideTime = rideTime;
        this.availableSeats = availableSeats;
        this.status = status;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getBikeId() {
        return bikeId;
    }

    public void setBikeId(String bikeId) {
        this.bikeId = bikeId;
    }

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getRideDate() {
        return rideDate;
    }

    public void setRideDate(LocalDate rideDate) {
        this.rideDate = rideDate;
    }

    public LocalTime getRideTime() {
        return rideTime;
    }

    public void setRideTime(LocalTime rideTime) {
        this.rideTime = rideTime;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String toFileString() {
        return TextClean.clean(rideId) + "|" + TextClean.clean(driverId) + "|"
                + TextClean.clean(bikeId) + "|" + TextClean.clean(startLocation) + "|"
                + TextClean.clean(destination) + "|" + rideDate + "|" + rideTime + "|"
                + availableSeats + "|" + TextClean.clean(status);
    }

    // Throws IllegalArgumentException when the line is not a valid ride record.
    public static Ride fromFileString(String line) {
        // The pipe character must be escaped for split()
        String[] p = line.split("\\|", -1);
        if (p.length != 9) {
            throw new IllegalArgumentException("Ride record must contain nine fields");
        }
        try {
            return new Ride(
                    p[0], p[1], p[2], p[3], p[4],
                    LocalDate.parse(p[5]), LocalTime.parse(p[6]),
                    Integer.parseInt(p[7]), p[8]
            );
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException("Ride record has an invalid value", e);
        }
    }
}
