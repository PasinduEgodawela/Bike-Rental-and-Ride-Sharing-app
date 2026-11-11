package com.Webapp.BikeRentalSystem.Model;

import com.Webapp.BikeRentalSystem.Filehandling.TextClean;

public class Bike {
    private String bikeId;
    private String model;
    private String type;
    private String location;
    private double pricePerDay;
    private boolean available;
    private String status;
    private String ownerId = "";     // userId of the Owner who listed the bike ("" = no owner, Admin only)

    public Bike(String bikeId, String model, String type, String location, double pricePerDay, boolean available, String status, String ownerId) {
        this(bikeId, model, type, location, pricePerDay, available, status);
        this.ownerId = ownerId == null ? "" : ownerId;
    }

    public Bike(String bikeId, String model, String type, String location, double pricePerDay, boolean available, String status) {
        this.bikeId = bikeId;
        this.model = model;
        this.type = type;
        this.location = location;
        this.pricePerDay = pricePerDay;
        this.available = available;
        this.status = status;
    }

    public Bike() {

    }

    public String getBikeId() {
        return bikeId;
    }

    public void setBikeId(String bikeId) {
        this.bikeId = bikeId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId == null ? "" : ownerId;
    }

    public String toFileString() {
        return TextClean.clean(bikeId) + "|" + TextClean.clean(model) + "|"
                + TextClean.clean(type) + "|" + TextClean.clean(location) + "|"
                + pricePerDay + "|" + available + "|" + TextClean.clean(status)
                + "|" + TextClean.clean(ownerId);
    }

    // Seven fields (older files, no owner) or eight fields (the last one is the owner's userId).
    // Throws IllegalArgumentException when the line is not a valid bike record.
    // The file handler catches it, so one bad line does not stop the bikes page.
    public static Bike fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length != 7 && p.length != 8) {
            throw new IllegalArgumentException("Bike record must contain seven or eight fields");
        }
        try {
            return new Bike(p[0], p[1], p[2], p[3],
                    Double.parseDouble(p[4]), Boolean.parseBoolean(p[5]), p[6],
                    p.length == 8 ? p[7] : "");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bike record has an invalid price", e);
        }
    }
}
