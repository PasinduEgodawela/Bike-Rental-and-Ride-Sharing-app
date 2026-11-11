package com.Webapp.BikeRentalSystem.Service;

import com.Webapp.BikeRentalSystem.DSA.BikeSort;
import com.Webapp.BikeRentalSystem.Filehandling.BikeFileHandler;
import com.Webapp.BikeRentalSystem.Model.Bike;
import com.Webapp.BikeRentalSystem.Model.User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class BikeService {
    private final BikeFileHandler handler;

    // The file handler is created by Spring and given to us
    public BikeService(BikeFileHandler handler) {
        this.handler = handler;
    }

    public List<Bike> getAllBikes() throws IOException {
        return handler.loadBikes();
    }

    // sort = "availability" puts the available bikes first (quick sort), anything else keeps file order
    public List<Bike> getAllBikes(String sort) throws IOException {
        List<Bike> bikes = handler.loadBikes();

        if ("availability".equals(sort)) {
            BikeSort.quickSort(bikes, 0, bikes.size() - 1);
        }
        return bikes;
    }

    public Bike getBikeById(String bikeId) throws IOException {
        List<Bike> bikes = handler.loadBikes();

        for (int i = 0; i < bikes.size(); i++) {
            if (bikes.get(i).getBikeId().equals(bikeId)) {
                return bikes.get(i);
            }
        }
        return null;
    }

    // B001, B002 ... the highest number + 1
    public String getNextBikeId() throws IOException {
        List<Bike> bikes = handler.loadBikes();
        int highest = 0;

        for (int i = 0; i < bikes.size(); i++) {
            String id = bikes.get(i).getBikeId();
            try {
                int number = Integer.parseInt(id.substring(1));
                if (number > highest) {
                    highest = number;
                }
            } catch (RuntimeException e) {
                // an ID in some other form (or an empty one) is not counted
            }
        }
        int next = highest + 1;
        return String.format("B%03d", next);
    }

    // The bike ID is made here, never taken from the form,
    // so two people adding bikes at the same time cannot get the same ID.
    public synchronized String addBike(Bike bike) throws IOException {
        bike.setBikeId(getNextBikeId());

        String error = validate(bike);
        if (!error.equals("")) {
            return error;
        }

        List<Bike> bikes = handler.loadBikes();
        bikes.add(bike);
        handler.saveBikes(bikes);
        return "OK";
    }


    public synchronized String updateBike(Bike updatedBike) throws IOException {
        String error = validate(updatedBike);
        if (!error.equals("")) {
            return error;
        }

        List<Bike> bikes = handler.loadBikes();

        for (int i = 0; i < bikes.size(); i++) {
            if (bikes.get(i).getBikeId().equals(updatedBike.getBikeId())) {
                // the owner never changes when a bike is edited
                updatedBike.setOwnerId(bikes.get(i).getOwnerId());
                bikes.set(i, updatedBike);
                handler.saveBikes(bikes);
                return "OK";
            }
        }
        return "Bike not found.";
    }

    // An Admin manages every bike. An Owner manages only the bikes that carry their own user ID.
    public boolean canManage(User user, String bikeId) throws IOException {
        if (user == null) {
            return false;
        }
        if ("Admin".equals(user.getRole())) {
            return true;
        }
        Bike bike = getBikeById(bikeId);
        return "Owner".equals(user.getRole()) && bike != null
                && user.getUserId().equals(bike.getOwnerId());
    }

    // Only changes the available flag. Used when a booking is confirmed, cancelled or completed.
    public synchronized void setAvailability(String bikeId, boolean available) throws IOException {
        List<Bike> bikes = handler.loadBikes();

        for (int i = 0; i < bikes.size(); i++) {
            if (bikes.get(i).getBikeId().equals(bikeId)) {
                bikes.get(i).setAvailable(available);
                handler.saveBikes(bikes);
                return;
            }
        }
    }

    // The controller checks for active bookings before calling this
    public synchronized String deleteBike(String bikeId) throws IOException {
        List<Bike> bikes = handler.loadBikes();

        for (int i = 0; i < bikes.size(); i++) {
            if (bikes.get(i).getBikeId().equals(bikeId)) {

                bikes.remove(i);
                handler.saveBikes(bikes);
                return "OK";
            }
        }
        return "Bike not found.";
    }


    private String validate(Bike bike) {
        if (bike.getBikeId() == null || bike.getBikeId().isBlank()) {
            return "Bike ID cannot be empty.";
        }
        if (bike.getModel() == null || bike.getModel().isBlank()) {
            return "Model cannot be empty.";
        }
        if (!"Motorbike".equals(bike.getType())
                && !"Scooter".equals(bike.getType())
                && !"Bicycle".equals(bike.getType())) {
            return "Type must be Motorbike, Scooter or Bicycle.";
        }
        if (bike.getLocation() == null || bike.getLocation().isBlank()) {
            return "Location cannot be empty.";
        }
        if (bike.getPricePerDay() <= 0) {
            return "Price per day must be greater than 0.";
        }
        if (!"Active".equals(bike.getStatus())
                && !"Maintenance".equals(bike.getStatus())) {
            return "Status must be Active or Maintenance.";
        }
        if ("Maintenance".equals(bike.getStatus()) && bike.isAvailable()) {
            return "A bike in Maintenance cannot be available.";
        }
        return "";
    }
}
