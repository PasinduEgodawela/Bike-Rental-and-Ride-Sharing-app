package com.Webapp.BikeRentalSystem.Service;

import com.Webapp.BikeRentalSystem.DTO.BookingDTO;
import com.Webapp.BikeRentalSystem.DTO.BookingResponse;
import com.Webapp.BikeRentalSystem.DTO.RideRequestDTO;
import com.Webapp.BikeRentalSystem.DTO.RideRequestResponse;
import com.Webapp.BikeRentalSystem.Filehandling.RideRequestFileHandler;
import com.Webapp.BikeRentalSystem.Model.Bike;
import com.Webapp.BikeRentalSystem.Model.Ride;
import com.Webapp.BikeRentalSystem.Model.RideRequest;
import com.Webapp.BikeRentalSystem.Model.User;
import com.Webapp.BikeRentalSystem.Service.DSA.RequestQueue;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Ride requests wait in a FIFO queue (first in, first out).
 * Only the request at the front of the queue can be approved or rejected.
 * Approving a request takes one seat on its ride and confirms its booking.
 */
@Service
public class RideRequestService {

    private final RideRequestFileHandler rideRequestFileHandler;
    private final BookingService bookingService;
    private final RideService rideService;
    private final UserService userService;
    private final BikeService bikeService;

    // The queue holds only the Pending requests, oldest first
    private RequestQueue queue = new RequestQueue();


    public RideRequestService(RideRequestFileHandler rideRequestFileHandler,
                              BookingService bookingService,
                              RideService rideService,
                              UserService userService,
                              BikeService bikeService) {
        this.rideRequestFileHandler = rideRequestFileHandler;
        this.bookingService = bookingService;
        this.rideService = rideService;
        this.userService = userService;
        this.bikeService = bikeService;
    }

    // ---------- the queue ----------

    @PostConstruct
    public void loadQueue() throws IOException {
        rebuildQueue();
    }

    // Fills a fresh queue with every Pending request, in file order (oldest first).
    // Called after anything that could make the queue out of date, so it never holds stale entries.
    private synchronized void rebuildQueue() throws IOException {
        RequestQueue fresh = new RequestQueue();

        for (RideRequest request : rideRequestFileHandler.findAll()) {
            if ("Pending".equals(request.getStatus())) {
                fresh.enqueue(request);
            }
        }
        queue = fresh;
    }

    // The ID of the request that is next in line, or null when nobody is waiting
    public synchronized String getFrontRequestId() {
        try {
            rebuildQueue();
        } catch (IOException e) {
            return null;
        }

        RideRequest front = queue.peek();
        return front == null ? null : front.getRequestId();
    }

    // ---------- reading ----------

    private RideRequestDTO toDTO(RideRequest request) throws IOException {
        RideRequestDTO dto = new RideRequestDTO(
                request.getRequestId(),
                request.getBookingId(),
                request.getPassengerId(),
                request.getBikeId(),
                request.getRequestDate(),
                request.getStatus());
        dto.setRideId(request.getRideId());

        User passenger = userService.getUserById(request.getPassengerId());
        Bike bike = bikeService.getBikeById(request.getBikeId());
        dto.setPassengerName(passenger != null ? passenger.getName() : "Unknown User");
        dto.setBikeName(bike != null ? bike.getModel() : "Unknown Bike");
        return dto;
    }

    public RideRequestResponse getAllRideRequests() {

        try {
            RideRequest[] rideRequests = rideRequestFileHandler.findAll();
            RideRequestDTO[] rideRequestDTOS = new RideRequestDTO[rideRequests.length];

            for (int i = 0; i < rideRequests.length; i++) {
                rideRequestDTOS[i] = toDTO(rideRequests[i]);
            }
            return new RideRequestResponse(200, "success", null, rideRequestDTOS);

        } catch (IOException e) {
            return RideRequestResponse.message(500, e.getMessage());
        }
    }

    public RideRequestResponse getRideRequestById(String requestId) {
        try {
            RideRequest request = rideRequestFileHandler.findById(requestId);

            if (request == null) {
                return RideRequestResponse.message(404, "Ride Request not found");
            }

            return new RideRequestResponse(200, "success", toDTO(request), null);

        } catch (IOException e) {
            return RideRequestResponse.message(500, e.getMessage());
        }
    }

    // ---------- create ----------

    /**
     * Saves a new request. The passenger and the bike come from the real booking, not from the
     * form, and the request is tied to one open ride on that bike.
     */
    public synchronized RideRequestResponse saveRideRequest(RideRequestDTO requestDTO) {

        try {
            String bookingId = requestDTO.getBookingId() == null ? "" : requestDTO.getBookingId().trim();

            BookingResponse bookingResponse = bookingService.getBookingById(bookingId);
            BookingDTO booking = bookingResponse.getBooking();
            if (booking == null) {
                return RideRequestResponse.message(404, "Booking " + bookingId + " was not found");
            }
            if (!"Pending".equals(booking.getStatus()) && !"Confirmed".equals(booking.getStatus())) {
                return RideRequestResponse.message(400, "This booking is " + booking.getStatus()
                        + ", so it cannot share a ride");
            }

            // one request per booking
            for (RideRequest existing : rideRequestFileHandler.findAll()) {
                if (existing.getBookingId().equals(bookingId)
                        && ("Pending".equals(existing.getStatus()) || "Approved".equals(existing.getStatus()))) {
                    return RideRequestResponse.message(409, "This booking already has a ride request ("
                            + existing.getRequestId() + ")");
                }
            }

            // the ride: the one chosen on the form, or the first open ride on the bike
            String rideId = requestDTO.getRideId() == null ? "" : requestDTO.getRideId().trim();
            List<Ride> openRides = rideService.getOpenRidesForBike(booking.getBikeId());

            if (rideId.isEmpty()) {
                if (openRides.isEmpty()) {
                    return RideRequestResponse.message(400, "There is no open ride with a free seat for this bike");
                }
                rideId = openRides.get(0).getRideId();
            } else {
                boolean found = false;
                for (Ride ride : openRides) {
                    if (ride.getRideId().equals(rideId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return RideRequestResponse.message(400, "Ride " + rideId
                            + " is not an open ride on this bike");
                }
            }

            RideRequest requestSave = new RideRequest(
                    null,
                    booking.getBookingId(),
                    booking.getUserId(),
                    booking.getBikeId(),
                    LocalDate.now(),
                    "Pending",
                    rideId
            );

            rideRequestFileHandler.create(requestSave);
            rebuildQueue();

            return RideRequestResponse.message(200, "Ride Request saved successfully");

        } catch (IOException e) {
            return RideRequestResponse.message(500, e.getMessage());
        }
    }

    // ---------- approve / reject (front of the queue only) ----------

    public synchronized RideRequestResponse approveRideRequest(String requestId) {
        return process(requestId, true);
    }

    public synchronized RideRequestResponse rejectRideRequest(String requestId) {
        return process(requestId, false);
    }

    private RideRequestResponse process(String requestId, boolean approve) {
        try {
            RideRequest request = rideRequestFileHandler.findById(requestId);

            if (request == null) {
                return RideRequestResponse.message(404, "Ride Request not found");
            }

            if (!"Pending".equals(request.getStatus())) {
                return RideRequestResponse.message(400, "Request is already " + request.getStatus());
            }

            // first in, first out: only the request at the front may be handled
            rebuildQueue();
            RideRequest front = queue.peek();
            if (front == null || !front.getRequestId().equals(requestId)) {
                String next = front == null ? "none" : front.getRequestId();
                return RideRequestResponse.message(409,
                        "Requests must be handled in the order they arrived. Next in line: " + next);
            }

            if (approve) {
                // the booking must still exist
                if (bookingService.getBookingById(request.getBookingId()).getBooking() == null) {
                    return RideRequestResponse.message(404, "The booking for this request no longer exists."
                            + " Reject this request or delete it.");
                }

                // take the seat, so a full ride stops the approval
                if (!request.getRideId().isEmpty()) {
                    try {
                        rideService.reserveSeat(request.getRideId());
                    } catch (IllegalArgumentException e) {
                        return RideRequestResponse.message(400, e.getMessage()
                                + " Reject this request or delete it.");
                    }
                }

                bookingService.updateBookingStatus(request.getBookingId(), "Confirmed");
                request.setStatus("Approved");

            } else {
                bookingService.updateBookingStatus(request.getBookingId(), "Cancelled");
                request.setStatus("Rejected");
            }

            rideRequestFileHandler.update(request);
            queue.dequeue();          // the request has been handled, so it leaves the queue

            return RideRequestResponse.message(200, "Ride Request " + request.getStatus());

        } catch (IOException e) {
            return RideRequestResponse.message(500, e.getMessage());
        }
    }

    // ---------- delete ----------

    public synchronized RideRequestResponse deleteRideRequest(String requestId) {
        try {
            if (!rideRequestFileHandler.delete(requestId)) {
                return RideRequestResponse.message(404, "Ride Request not found");
            }

            rebuildQueue();          // a deleted request must not stay in the queue

            return RideRequestResponse.message(200, "Ride Request deleted successfully");

        } catch (IOException e) {
            return RideRequestResponse.message(500, e.getMessage());
        }
    }

    // When an account is deleted, its ride requests go too
    public synchronized void deleteRequestsOfUser(String userId) throws IOException {
        for (RideRequest request : rideRequestFileHandler.findAll()) {
            if (userId.equals(request.getPassengerId())) {
                rideRequestFileHandler.delete(request.getRequestId());
            }
        }
        rebuildQueue();
    }
}
