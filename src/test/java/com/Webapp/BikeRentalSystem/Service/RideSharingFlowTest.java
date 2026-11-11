package com.Webapp.BikeRentalSystem.Service;

import com.Webapp.BikeRentalSystem.DTO.BookingDTO;
import com.Webapp.BikeRentalSystem.DTO.RideDTO;
import com.Webapp.BikeRentalSystem.DTO.RideRequestDTO;
import com.Webapp.BikeRentalSystem.DTO.RideRequestResponse;
import com.Webapp.BikeRentalSystem.Filehandling.BikeFileHandler;
import com.Webapp.BikeRentalSystem.Filehandling.BookingFileHandler;
import com.Webapp.BikeRentalSystem.Filehandling.FileHandler;
import com.Webapp.BikeRentalSystem.Filehandling.FilePaymentRepository;
import com.Webapp.BikeRentalSystem.Filehandling.RideFileHandler;
import com.Webapp.BikeRentalSystem.Filehandling.RideRequestFileHandler;
import com.Webapp.BikeRentalSystem.Filehandling.UserFileHandler;
import com.Webapp.BikeRentalSystem.Model.Billing;
import com.Webapp.BikeRentalSystem.Model.PaymentException;
import com.Webapp.BikeRentalSystem.Model.Ride;
import com.Webapp.BikeRentalSystem.Model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Booking -> ride request -> approve, and payments, using temporary data files. */
class RideSharingFlowTest {

    @TempDir
    Path dir;

    private BikeService bikeService;
    private UserService userService;
    private BookingService bookingService;
    private RideService rideService;
    private RideRequestService requestService;
    private FilePaymentRepository payments;
    private PaymentService paymentService;

    private final User customer = new User("U002", "Cust", "c@x.com", "secret1", "0711111111", "addr", "Customer");
    private final User admin = new User("U001", "Admin", "a@x.com", "secret1", "0711111111", "addr", "Admin");

    private void setUp() throws Exception {
        Files.write(dir.resolve("users.txt"), List.of(
                admin.toFileString(),
                customer.toFileString(),
                new User("U003", "Drv", "d@x.com", "secret1", "0712222222", "addr", "Driver").toFileString()));
        Files.write(dir.resolve("bikes.txt"), List.of("B001|Honda|Motorbike|Kandy|800.0|true|Active"));
        Files.write(dir.resolve("bookings.txt"), List.<String>of());
        Files.write(dir.resolve("rides.txt"), List.<String>of());
        Files.write(dir.resolve("rideRequests.txt"), List.<String>of());
        Files.write(dir.resolve("payments.txt"), List.<String>of());

        userService = new UserService(new UserFileHandler(new FileHandler(), dir.resolve("users.txt").toString()));
        bikeService = new BikeService(new BikeFileHandler(dir.resolve("bikes.txt").toString()));
        bookingService = new BookingService(new BookingFileHandler(dir.resolve("bookings.txt").toString()),
                userService, bikeService);
        rideService = new RideService(new RideFileHandler(new FileHandler(), dir.resolve("rides.txt").toString()),
                bookingService, bikeService, userService);
        requestService = new RideRequestService(new RideRequestFileHandler(dir.resolve("rideRequests.txt").toString()),
                bookingService, rideService, userService, bikeService);
        payments = new FilePaymentRepository(dir.resolve("payments.txt").toString());
        paymentService = new PaymentService(payments, bookingService, bikeService, userService);
    }

    // A save returns only a status; the new booking is found again by its date.
    private String newBooking() {
        return newBooking(1);
    }

    private String newBooking(int daysFromNow) {
        BookingDTO dto = new BookingDTO();
        dto.setBikeId("B001");
        dto.setBookingDate(LocalDate.now().plusDays(daysFromNow));
        assertEquals(200, bookingService.saveBooking("U002", dto).getStatus());
        return findBooking(daysFromNow).getBookingId();
    }

    private BookingDTO findBooking(int daysFromNow) {
        for (BookingDTO b : bookingService.getAllBookings().getBookings()) {
            if (b.getBookingDate().equals(LocalDate.now().plusDays(daysFromNow))) {
                return b;
            }
        }
        throw new AssertionError("booking not found");
    }

    private RideRequestDTO findRequest(String bookingId) {
        for (RideRequestDTO r : requestService.getAllRideRequests().getRideRequests()) {
            if (r.getBookingId().equals(bookingId)) {
                return r;
            }
        }
        throw new AssertionError("ride request not found");
    }

    private String requestIdFor(String bookingId, String rideId) {
        assertEquals(200, requestService.saveRideRequest(requestFor(bookingId, rideId)).getStatus());
        return findRequest(bookingId).getRequestId();
    }

    // The seats value typed in the DTO is ignored: every ride has one passenger seat
    private String newRide() {
        Ride ride = rideService.createRide(new RideDTO(null, "B001", "U003", "Kandy", "Colombo",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5, "Open"));
        return ride.getRideId();
    }

    private RideRequestDTO requestFor(String bookingId, String rideId) {
        RideRequestDTO dto = new RideRequestDTO();
        dto.setBookingId(bookingId);
        dto.setRideId(rideId);
        return dto;
    }

    @Test
    void bookingIsStoredWithRealIdsAndForcedPending() throws Exception {
        setUp();
        BookingDTO dto = new BookingDTO();
        dto.setBikeId("B001");
        dto.setBookingDate(LocalDate.now().plusDays(1));
        dto.setStatus("Confirmed");                       // a customer must not choose this

        assertEquals(200, bookingService.saveBooking("U002", dto).getStatus());

        BookingDTO saved = findBooking(1);
        assertEquals("U002", saved.getUserId());
        assertEquals("B001", saved.getBikeId());
        assertEquals("Pending", saved.getStatus());
        assertEquals("Cust", saved.getUserName());
        assertEquals("Honda", saved.getBikeName());
    }

    @Test
    void pastDateUnknownBikeAndDoubleBookingAreRejected() throws Exception {
        setUp();
        BookingDTO past = new BookingDTO();
        past.setBikeId("B001");
        past.setBookingDate(LocalDate.now().minusDays(1));
        assertEquals(400, bookingService.saveBooking("U002", past).getStatus());

        BookingDTO unknown = new BookingDTO();
        unknown.setBikeId("B999");
        unknown.setBookingDate(LocalDate.now().plusDays(1));
        assertEquals(404, bookingService.saveBooking("U002", unknown).getStatus());

        newBooking();
        BookingDTO again = new BookingDTO();
        again.setBikeId("B001");
        again.setBookingDate(LocalDate.now().plusDays(1));
        assertEquals(409, bookingService.saveBooking("U002", again).getStatus());
    }

    @Test
    void requestsAreApprovedInArrivalOrderOnly() throws Exception {
        setUp();
        String first = newBooking();

        String second = newBooking(2);

        String rideId = newRide();

        String r1 = requestIdFor(first, rideId);
        String r2 = requestIdFor(second, rideId);

        assertEquals(r1, requestService.getFrontRequestId());

        // the second request cannot jump the queue
        RideRequestResponse jump = requestService.approveRideRequest(r2);
        assertEquals(409, jump.getStatus());

        // the first one can, and the booking becomes Confirmed
        assertEquals(200, requestService.approveRideRequest(r1).getStatus());
        assertEquals("Confirmed", bookingService.getBookingById(first).getBooking().getStatus());
        assertEquals("Approved", findRequest(first).getStatus());
        assertEquals(r2, requestService.getFrontRequestId());
    }

    @Test
    void approvingTakesOneSeatAndAFullRideIsRefused() throws Exception {
        setUp();
        String booking = newBooking();
        String rideId = newRide();

        String requestId = requestIdFor(booking, rideId);
        assertEquals(200, requestService.approveRideRequest(requestId).getStatus());

        assertEquals(0, rideService.getRideById(rideId).getAvailableSeats());
        assertThrows(RuntimeException.class, () -> rideService.reserveSeat(rideId));
    }

    @Test
    void rejectingCancelsTheBookingAndFreesTheQueue() throws Exception {
        setUp();
        String booking = newBooking();
        String rideId = newRide();
        String requestId = requestIdFor(booking, rideId);

        assertEquals(200, requestService.rejectRideRequest(requestId).getStatus());

        assertEquals("Cancelled", bookingService.getBookingById(booking).getBooking().getStatus());
        assertEquals(null, requestService.getFrontRequestId());
    }

    @Test
    void secondActiveRequestForTheSameBookingIsRefused() throws Exception {
        setUp();
        String booking = newBooking();
        String rideId = newRide();

        assertEquals(200, requestService.saveRideRequest(requestFor(booking, rideId)).getStatus());
        assertEquals(409, requestService.saveRideRequest(requestFor(booking, rideId)).getStatus());
    }

    @Test
    void everyRideHasExactlyOnePassengerSeat() throws Exception {
        setUp();
        String rideId = newRide();

        assertEquals(1, rideService.getRideById(rideId).getAvailableSeats());
        assertEquals("Open", rideService.getRideById(rideId).getStatus());

        // editing the ride cannot add seats
        RideDTO edit = new RideDTO(rideId, "B001", "U003", "Kandy", "Colombo",
                LocalDate.now().plusDays(1), LocalTime.of(10, 30), 9, "Open");
        rideService.updateRide(rideId, edit);

        assertEquals(1, rideService.getRideById(rideId).getAvailableSeats());
        assertEquals(LocalTime.of(10, 30), rideService.getRideById(rideId).getRideTime());
    }

    @Test
    void aRideBecomesFullWhenItsSeatIsTakenAndStaysFullOnEdit() throws Exception {
        setUp();
        String booking = newBooking();
        String rideId = newRide();

        requestService.approveRideRequest(requestIdFor(booking, rideId));
        assertEquals("Full", rideService.getRideById(rideId).getStatus());

        RideDTO edit = new RideDTO(rideId, "B001", "U003", "Kandy", "Colombo",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), 1, "Open");
        rideService.updateRide(rideId, edit);

        assertEquals("Full", rideService.getRideById(rideId).getStatus());
    }

    @Test
    void onlyTheDriverWhoPublishedARideOwnsIt() throws Exception {
        setUp();
        String rideId = newRide();

        assertTrue(rideService.driverOwns("U003", rideId));
        assertTrue(!rideService.driverOwns("U004", rideId));
        assertTrue(!rideService.driverOwns("U003", "R999"));
    }

    @Test
    void aRideNeedsARealDriverAndDifferentEnds() throws Exception {
        setUp();
        LocalDate day = LocalDate.now().plusDays(1);

        // a Customer cannot be the driver
        assertThrows(IllegalArgumentException.class, () -> rideService.createRide(
                new RideDTO(null, "B001", "U002", "Kandy", "Colombo", day, LocalTime.of(9, 0), 1, "Open")));
        // start and destination must differ
        assertThrows(IllegalArgumentException.class, () -> rideService.createRide(
                new RideDTO(null, "B001", "U003", "Kandy", "kandy", day, LocalTime.of(9, 0), 1, "Open")));
    }

    @Test
    void ownersManageOnlyTheirOwnBikesAndAdminsManageAll() throws Exception {
        setUp();
        User ownerA = new User("U010", "OwnerA", "oa@x.com", "secret1", "0711111111", "addr", "Owner");
        User ownerB = new User("U011", "OwnerB", "ob@x.com", "secret1", "0711111111", "addr", "Owner");

        com.Webapp.BikeRentalSystem.Model.Bike bike =
                new com.Webapp.BikeRentalSystem.Model.Bike("", "Yamaha", "Scooter", "Galle", 2500.0, true, "Active", "U010");
        assertEquals("OK", bikeService.addBike(bike));

        assertTrue(bikeService.canManage(ownerA, "B002"));
        assertTrue(!bikeService.canManage(ownerB, "B002"));
        assertTrue(bikeService.canManage(admin, "B002"));
        assertTrue(!bikeService.canManage(customer, "B002"));

        // editing keeps the owner even though the edit form never sends one
        com.Webapp.BikeRentalSystem.Model.Bike edited =
                new com.Webapp.BikeRentalSystem.Model.Bike("B002", "Yamaha FZ", "Scooter", "Galle", 2600.0, true, "Active");
        assertEquals("OK", bikeService.updateBike(edited));
        assertEquals("U010", bikeService.getBikeById("B002").getOwnerId());

        // the old bike from the sample data has no owner, so only an Admin manages it
        assertTrue(!bikeService.canManage(ownerA, "B001"));
        assertTrue(bikeService.canManage(admin, "B001"));
    }

    @Test
    void registrationAllowsOwnerAndDriverButNotAdmin() throws Exception {
        setUp();
        com.Webapp.BikeRentalSystem.DTO.RegisterDTO dto = new com.Webapp.BikeRentalSystem.DTO.RegisterDTO();
        dto.setName("New Owner");
        dto.setEmail("newowner@x.com");
        dto.setPassword("secret1");
        dto.setPhone("0771234567");
        dto.setAddress("Kandy");

        dto.setRole("Owner");
        assertEquals("Owner", userService.registerUser(dto).getRole());

        dto.setEmail("another@x.com");
        dto.setRole("Admin");
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
    }

    @Test
    void aBookingCanOnlyBePaidOnce() throws Exception {
        setUp();
        String booking = newBooking();

        Billing billing = paymentService.getBilling(booking, customer);
        LocalDate start = LocalDate.now().plusDays(1);
        paymentService.pay(billing, start, start.plusDays(1), "cash");

        assertEquals(1, paymentService.getAllPayments().size());
        assertThrows(PaymentException.class,
                () -> paymentService.pay(billing, start, start.plusDays(1), "cash"));
    }

    @Test
    void otherCustomersCannotSeeOrPayAnotherBooking() throws Exception {
        setUp();
        String booking = newBooking();
        User stranger = new User("U009", "Other", "o@x.com", "secret1", "0711111111", "addr", "Customer");

        assertThrows(PaymentException.class, () -> paymentService.getBilling(booking, stranger));
        assertEquals("a@x.com", paymentService.getBilling(booking, admin) == null ? "" : "a@x.com");
    }
}
