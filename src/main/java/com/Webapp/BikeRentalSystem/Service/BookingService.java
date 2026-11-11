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

    public boolean hasActiveBookingForBike(String bikeId) throws IOException {
        for (Booking booking : bookingFileHandler.findAll()) {
            if (booking.getBikeId().equals(bikeId) && isActive(booking.getStatus())) {
                return true;
            }
        }
        return false;
    }


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

    public synchronized int deleteBookingsOfUser(String userId) throws IOException {
        Booking[] bookings = bookingFileHandler.getBookingsByUserId(userId);

        for (Booking booking : bookings) {
            bookingFileHandler.delete(booking.getBookingId());
            refreshBikeAvailability(booking.getBikeId());
        }
        return bookings.length;
    }


    private boolean isActive(String status) {
        return "Pending".equals(status) || "Confirmed".equals(status);
    }

    private boolean isValidStatus(String status) {
        return "Pending".equals(status) || "Confirmed".equals(status)
                || "Cancelled".equals(status) || "Completed".equals(status);
    }


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
