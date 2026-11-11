package com.Webapp.BikeRentalSystem.Service;


import com.Webapp.BikeRentalSystem.DTO.BookingDTO;
import com.Webapp.BikeRentalSystem.DTO.RideDTO;
import com.Webapp.BikeRentalSystem.Filehandling.RideFileHandler;
import com.Webapp.BikeRentalSystem.Model.Ride;
import com.Webapp.BikeRentalSystem.Model.User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class RideService {

    private final RideFileHandler fileHandler;
    private final BookingService bookingService;
    private final BikeService bikeService;
    private final UserService userService;

    public RideService(RideFileHandler fileHandler, BookingService bookingService,
                       BikeService bikeService, UserService userService) {
        this.fileHandler = fileHandler;
        this.bookingService = bookingService;
        this.bikeService = bikeService;
        this.userService = userService;
    }

    public List<Ride> readRides() {
        return fileHandler.loadRides();
    }

    public Ride getRideById(String rideId) {
        for (Ride ride : fileHandler.loadRides()) {
            if (ride.getRideId().equals(rideId)) {
                return ride;
            }
        }
        return null;
    }

    // ---------- create ----------

    // A bike carries the driver and one passenger, so every ride has exactly one passenger seat.
    // The seat count is never typed in a form.
    public static final int PASSENGER_SEATS = 1;

    public synchronized Ride createRide(RideDTO dto) {
        validateNewRide(dto);

        List<Ride> rides = fileHandler.loadRides();

        // The order must match the Ride constructor: rideId, driverId, bikeId, ...
        Ride ride = new Ride(
                fileHandler.nextId(rides),
                dto.getDriverId().trim(),
                dto.getBikeId().trim(),
                dto.getStartLocation().trim(),
                dto.getDestination().trim(),
                dto.getRideDate(),
                dto.getRideTime(),
                PASSENGER_SEATS,
                "Open"
        );

        rides.add(ride);
        fileHandler.saveRides(rides);
        return ride;
    }

    // Throws IllegalArgumentException with a message the page can show
    private void validateNewRide(RideDTO dto) {
        if (isBlank(dto.getBikeId())) {
            throw new IllegalArgumentException("Please enter a bike ID.");
        }
        if (isBlank(dto.getDriverId())) {
            throw new IllegalArgumentException("Please select a driver.");
        }
        if (isBlank(dto.getStartLocation()) || isBlank(dto.getDestination())) {
            throw new IllegalArgumentException("Start location and destination are required.");
        }
        if (dto.getStartLocation().trim().equalsIgnoreCase(dto.getDestination().trim())) {
            throw new IllegalArgumentException("Start location and destination must be different.");
        }
        if (dto.getRideDate() == null || dto.getRideTime() == null) {
            throw new IllegalArgumentException("Please choose the ride date and time.");
        }
        if (dto.getRideDate().isBefore(LocalDate.now())
                || (dto.getRideDate().equals(LocalDate.now()) && dto.getRideTime().isBefore(LocalTime.now()))) {
            throw new IllegalArgumentException("The ride cannot start in the past.");
        }

        try {
            if (bikeService.getBikeById(dto.getBikeId().trim()) == null) {
                throw new IllegalArgumentException("Bike " + dto.getBikeId().trim() + " does not exist.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("The bike list could not be read. Please try again.");
        }

        User driver = userService.getUserById(dto.getDriverId().trim());
        if (driver == null || !"Driver".equals(driver.getRole())) {
            throw new IllegalArgumentException("The selected driver does not exist.");
        }
    }

    // ---------- update ----------

    // Only the time and the status can change. The seat count only changes when a request is
    // approved (reserveSeat). Throws IllegalArgumentException for bad values.
    public synchronized Ride updateRide(String rideId, RideDTO dto) {
        if (dto.getRideTime() == null) {
            throw new IllegalArgumentException("Please choose the ride time.");
        }
        String wanted = dto.getStatus();
        if (!"Open".equals(wanted) && !"Full".equals(wanted)
                && !"Cancelled".equals(wanted) && !"Completed".equals(wanted)) {
            throw new IllegalArgumentException("Status must be Open, Full, Cancelled or Completed.");
        }

        List<Ride> rides = fileHandler.loadRides();

        for (Ride ride : rides) {
            if (ride.getRideId().equals(rideId)) {
                ride.setRideTime(dto.getRideTime());

                // Open / Full follow the number of free seats. Cancelled / Completed are kept as chosen.
                if ("Open".equals(wanted) || "Full".equals(wanted)) {
                    ride.setStatus(ride.getAvailableSeats() == 0 ? "Full" : "Open");
                } else {
                    ride.setStatus(wanted);
                }

                fileHandler.saveRides(rides);
                return ride;
            }
        }
        return null;
    }

    // One seat is taken when a ride request is approved.
    // Throws IllegalArgumentException if the ride is missing, not open or full.
    public synchronized void reserveSeat(String rideId) {
        List<Ride> rides = fileHandler.loadRides();

        for (Ride ride : rides) {
            if (ride.getRideId().equals(rideId)) {
                if (!"Open".equals(ride.getStatus()) || ride.getAvailableSeats() <= 0) {
                    throw new IllegalArgumentException("Ride " + rideId + " has no free seats.");
                }
                ride.setAvailableSeats(ride.getAvailableSeats() - 1);
                if (ride.getAvailableSeats() == 0) {
                    ride.setStatus("Full");
                }
                fileHandler.saveRides(rides);
                return;
            }
        }
        throw new IllegalArgumentException("Ride " + rideId + " does not exist.");
    }

    // ---------- delete ----------

    public synchronized void deleteRide(String rideId) {
        List<Ride> rides = fileHandler.loadRides();

        for (int i = 0; i < rides.size(); i++) {
            if (rides.get(i).getRideId().equals(rideId)) {
                rides.remove(i);
                fileHandler.saveRides(rides);
                return;
            }
        }
    }

    // When a Driver account is deleted, the rides assigned to that driver go too
    public synchronized void deleteRidesOfDriver(String driverId) {
        List<Ride> rides = fileHandler.loadRides();
        List<Ride> kept = new ArrayList<>();

        for (Ride ride : rides) {
            if (!driverId.equals(ride.getDriverId())) {
                kept.add(ride);
            }
        }

        if (kept.size() != rides.size()) {
            fileHandler.saveRides(kept);
        }
    }

    // ---------- lists ----------

    // DRIVER: rides assigned to this driver
    public List<Ride> getRidesForDriver(String userId) {
        List<Ride> result = new ArrayList<>();
        for (Ride ride : fileHandler.loadRides()) {
            if (userId.equals(ride.getDriverId())) {
                result.add(ride);
            }
        }
        return result;
    }

    // CUSTOMER: rides on the bikes this customer has booked
    public List<Ride> getRidesForCustomer(String userId) {
        List<Ride> result = new ArrayList<>();
        BookingDTO[] bookings = bookingService.getBookingByUserId(userId).getBookings();

        if (bookings == null) {
            return result;
        }

        for (Ride ride : fileHandler.loadRides()) {
            for (BookingDTO b : bookings) {
                if (ride.getBikeId().equals(b.getBikeId())) {
                    result.add(ride);
                    break;
                }
            }
        }
        return result;
    }

    // A Driver may only change their own rides
    public boolean driverOwns(String userId, String rideId) {
        Ride ride = getRideById(rideId);
        return ride != null && userId != null && userId.equals(ride.getDriverId());
    }

    // Rides on this bike that still have a free seat and have not started yet
    public List<Ride> getOpenRidesForBike(String bikeId) {
        List<Ride> result = new ArrayList<>();

        for (Ride ride : fileHandler.loadRides()) {
            if (ride.getBikeId().equals(bikeId)
                    && "Open".equals(ride.getStatus())
                    && ride.getAvailableSeats() > 0
                    && !ride.getRideDate().isBefore(LocalDate.now())) {
                result.add(ride);
            }
        }
        return result;
    }

    // rideId -> bookingId (customer's own booking for the ride's bike)
    public Map<String, String> getBookingIdsForRides(String userId, List<Ride> rides) {

        Map<String, String> map = new HashMap<>();
        BookingDTO[] bookings = bookingService.getBookingByUserId(userId).getBookings();

        if (bookings == null) {
            return map;
        }

        for (Ride ride : rides) {
            for (BookingDTO b : bookings) {
                if (ride.getBikeId().equals(b.getBikeId())
                        && !"Cancelled".equalsIgnoreCase(b.getStatus())) {
                    map.put(ride.getRideId(), b.getBookingId());
                    break;
                }
            }
        }
        return map;
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
