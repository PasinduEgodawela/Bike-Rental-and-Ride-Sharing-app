package com.Webapp.BikeRentalSystem.Service;

import com.Webapp.BikeRentalSystem.DTO.BookingDTO;
import com.Webapp.BikeRentalSystem.DTO.BookingResponse;
import com.Webapp.BikeRentalSystem.Filehandling.BookingFileHandler;
import com.Webapp.BikeRentalSystem.Model.Bike;
import com.Webapp.BikeRentalSystem.Model.Booking;
import com.Webapp.BikeRentalSystem.Model.User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Bookings are stored with real IDs (user ID and bike ID).
 * The BookingDTO carries those IDs plus the user's name and the bike's model,
 * which are only there to be shown on the pages.
 *
 * Booking statuses: Pending, Confirmed, Cancelled, Completed.
 */
@Service
public class BookingService {

    private final BookingFileHandler bookingFileHandler;
    private final UserService userService;
    private final BikeService bikeService;

    public BookingService(BookingFileHandler bookingFileHandler, UserService userService, BikeService bikeService) {
        this.bookingFileHandler = bookingFileHandler;
        this.userService = userService;
        this.bikeService = bikeService;
    }

    // ---------- reading ----------

    // Booking -> BookingDTO. The ID fields keep the real IDs; the names go in their own fields.
    private BookingDTO toDTO(Booking booking) throws IOException {
        User user = userService.getUserById(booking.getUserId());
        Bike bike = bikeService.getBikeById(booking.getBikeId());

        BookingDTO dto = new BookingDTO(
                booking.getBookingId(),
                booking.getUserId(),
                booking.getBikeId(),
                booking.getBookingDate(),
                booking.getStatus());

        dto.setUserName(user != null ? user.getName() : "Unknown User");
        dto.setBikeName(bike != null ? bike.getModel() : "Unknown Bike");
        return dto;
    }

    private BookingDTO[] toDTOs(Booking[] bookings) throws IOException {
        BookingDTO[] bookingDTOS = new BookingDTO[bookings.length];

        for (int i = 0; i < bookings.length; i++) {
            bookingDTOS[i] = toDTO(bookings[i]);
        }
        return bookingDTOS;
    }

    public BookingResponse getAllBookings() {
        try {
            return new BookingResponse(200, "success", null, toDTOs(bookingFileHandler.findAll()));
        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    public BookingResponse getBookingById(String bookingId) {
        try {
            Booking booking = bookingFileHandler.findById(bookingId);

            if (booking == null) {
                return BookingResponse.message(404, "Booking not found");
            }
            return new BookingResponse(200, "success", toDTO(booking), null);

        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    public BookingResponse getBookingByUserId(String userId) {
        try {
            return new BookingResponse(200, "success", null,
                    toDTOs(bookingFileHandler.getBookingsByUserId(userId)));
        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    // True when the bike has a Pending or Confirmed booking (used before deleting a bike)
    public boolean hasActiveBookingForBike(String bikeId) throws IOException {
        for (Booking booking : bookingFileHandler.findAll()) {
            if (booking.getBikeId().equals(bikeId) && isActive(booking.getStatus())) {
                return true;
            }
        }
        return false;
    }

    // ---------- create ----------

    // The status is always Pending. It is never taken from the form,
    // otherwise a customer could confirm their own booking.
    public synchronized BookingResponse saveBooking(String userId, BookingDTO bookingDTO) {

        try {
            if (userId == null || userId.isBlank()) {
                return BookingResponse.message(401, "Please sign in to make a booking");
            }

            String bikeId = bookingDTO.getBikeId() == null ? "" : bookingDTO.getBikeId().trim();
            LocalDate date = bookingDTO.getBookingDate();

            if (bikeId.isEmpty()) {
                return BookingResponse.message(400, "Please enter a bike ID");
            }
            if (date == null) {
                return BookingResponse.message(400, "Please choose a booking date");
            }
            if (date.isBefore(LocalDate.now())) {
                return BookingResponse.message(400, "The booking date cannot be in the past");
            }

            Bike bike = bikeService.getBikeById(bikeId);
            if (bike == null) {
                return BookingResponse.message(404, "Bike " + bikeId + " does not exist");
            }
            if (!"Active".equals(bike.getStatus())) {
                return BookingResponse.message(400, "This bike is not in service right now");
            }
            if (hasBookingOnDate(bikeId, date, null)) {
                return BookingResponse.message(409, "This bike is already booked for " + date);
            }

            Booking bookingSave = new Booking(null, userId, bikeId, date, "Pending");
            bookingFileHandler.create(bookingSave);

            return new BookingResponse(200, "success", null, null);

        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    // ---------- update ----------

    /**
     * Edits a booking. Only the date and the status can change; the owner and the bike never do.
     * An Admin can set any status. A customer can only cancel their own booking.
     */
    public synchronized BookingResponse updateBooking(String bookingId, BookingDTO bookingDTO,
                                                      String userId, String role) {

        try {
            Booking booking = bookingFileHandler.findById(bookingId);
            if (booking == null) {
                return BookingResponse.message(404, "Booking Id not found");
            }

            boolean isAdmin = "Admin".equals(role);
            if (!isAdmin && !booking.getUserId().equals(userId)) {
                return BookingResponse.message(403, "You can only change your own bookings");
            }

            // ----- date -----
            LocalDate newDate = bookingDTO.getBookingDate();
            if (newDate == null) {
                return BookingResponse.message(400, "Please choose a booking date");
            }
            if (!newDate.equals(booking.getBookingDate())) {
                if (newDate.isBefore(LocalDate.now())) {
                    return BookingResponse.message(400, "The booking date cannot be in the past");
                }
                if (hasBookingOnDate(booking.getBikeId(), newDate, bookingId)) {
                    return BookingResponse.message(409, "This bike is already booked for " + newDate);
                }
            }

            // ----- status -----
            String newStatus = booking.getStatus();
            String wanted = bookingDTO.getStatus();

            if (wanted != null && !wanted.equals(booking.getStatus())) {
                if (isAdmin) {
                    if (!isValidStatus(wanted)) {
                        return BookingResponse.message(400, "Status must be Pending, Confirmed, Cancelled or Completed");
                    }
                    if ("Completed".equals(wanted) && !"Confirmed".equals(booking.getStatus())) {
                        return BookingResponse.message(400, "Only a Confirmed booking can be completed");
                    }
                    newStatus = wanted;
                } else if ("Cancelled".equals(wanted)
                        && isActive(booking.getStatus())) {
                    newStatus = "Cancelled";
                }
            }

            Booking bookingUpdate = new Booking(bookingId,
                    booking.getUserId(),
                    booking.getBikeId(),
                    newDate,
                    newStatus);

            bookingFileHandler.update(bookingUpdate);
            refreshBikeAvailability(booking.getBikeId());

            return new BookingResponse(200, "success", null, null);

        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    // Used by the ride request queue (approve = Confirmed, reject = Cancelled).
    // This is a system action, so it does no permission check.
    public synchronized BookingResponse updateBookingStatus(String bookingId, String status) {
        try {
            Booking booking = bookingFileHandler.findById(bookingId);

            if (booking == null) {
                return BookingResponse.message(404, "Booking Id not found");
            }
            if (!isValidStatus(status)) {
                return BookingResponse.message(400, "Invalid booking status");
            }

            Booking bookingUpdate = new Booking(
                    booking.getBookingId(),
                    booking.getUserId(),
                    booking.getBikeId(),
                    booking.getBookingDate(),
                    status
            );

            bookingFileHandler.update(bookingUpdate);
            refreshBikeAvailability(booking.getBikeId());

            return new BookingResponse(200, "success", null, null);

        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    // Admin: Confirmed -> Completed. Only a Completed booking lets the customer review the bike.
    public synchronized BookingResponse completeBooking(String bookingId) {
        try {
            Booking booking = bookingFileHandler.findById(bookingId);

            if (booking == null) {
                return BookingResponse.message(404, "Booking Id not found");
            }
            if (!"Confirmed".equals(booking.getStatus())) {
                return BookingResponse.message(400, "Only a Confirmed booking can be completed");
            }
            return updateBookingStatus(bookingId, "Completed");

        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    // ---------- delete ----------

    public synchronized BookingResponse deleteBooking(String bookingId, String userId, String role) {

        try {
            Booking booking = bookingFileHandler.findById(bookingId);

            if (booking == null) {
                return BookingResponse.message(404, "Booking not found");
            }
            if (!"Admin".equals(role) && !booking.getUserId().equals(userId)) {
                return BookingResponse.message(403, "You can only delete your own bookings");
            }

            bookingFileHandler.delete(bookingId);
            refreshBikeAvailability(booking.getBikeId());

            return BookingResponse.message(200, "Booking deleted successfully");

        } catch (IOException e) {
            return BookingResponse.message(500, e.getMessage());
        }
    }

    // When an account is deleted, its bookings go too. Returns how many were removed.
    public synchronized int deleteBookingsOfUser(String userId) throws IOException {
        Booking[] bookings = bookingFileHandler.getBookingsByUserId(userId);

        for (Booking booking : bookings) {
            bookingFileHandler.delete(booking.getBookingId());
            refreshBikeAvailability(booking.getBikeId());
        }
        return bookings.length;
    }

    // ---------- rules and helpers ----------

    private boolean isActive(String status) {
        return "Pending".equals(status) || "Confirmed".equals(status);
    }

    private boolean isValidStatus(String status) {
        return "Pending".equals(status) || "Confirmed".equals(status)
                || "Cancelled".equals(status) || "Completed".equals(status);
    }

    // Is there already a Pending or Confirmed booking for this bike on this date?
    // ignoreBookingId lets an edit skip the booking being edited.
    private boolean hasBookingOnDate(String bikeId, LocalDate date, String ignoreBookingId) throws IOException {
        for (Booking other : bookingFileHandler.findAll()) {
            if (other.getBikeId().equals(bikeId)
                    && date.equals(other.getBookingDate())
                    && isActive(other.getStatus())
                    && !other.getBookingId().equals(ignoreBookingId)) {
                return true;
            }
        }
        return false;
    }

    // A bike shows as Not Available while it has a Confirmed booking (it is out on rent),
    // and again as Available once that booking is Completed, Cancelled or deleted.
    // A bike in Maintenance is never available.
    private void refreshBikeAvailability(String bikeId) throws IOException {
        Bike bike = bikeService.getBikeById(bikeId);
        if (bike == null) {
            return;
        }

        boolean hasConfirmed = false;
        for (Booking other : bookingFileHandler.findAll()) {
            if (other.getBikeId().equals(bikeId) && "Confirmed".equals(other.getStatus())) {
                hasConfirmed = true;
                break;
            }
        }

        boolean shouldBeAvailable = "Active".equals(bike.getStatus()) && !hasConfirmed;

        if (bike.isAvailable() != shouldBeAvailable) {
            bikeService.setAvailability(bikeId, shouldBeAvailable);
        }
    }
}
