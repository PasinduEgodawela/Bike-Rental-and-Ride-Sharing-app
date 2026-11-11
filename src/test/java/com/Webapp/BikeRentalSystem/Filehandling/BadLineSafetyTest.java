package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.Bike;
import com.Webapp.BikeRentalSystem.Model.Booking;
import com.Webapp.BikeRentalSystem.Model.Ride;
import com.Webapp.BikeRentalSystem.Model.RideRequest;
import com.Webapp.BikeRentalSystem.Model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One damaged line must not stop the app, and saving must not delete it. */
class BadLineSafetyTest {

    @TempDir
    Path tempDir;

    @Test
    void damagedUserLineDoesNotStopLoginsAndIsKept() throws Exception {
        Path file = tempDir.resolve("users.txt");
        Files.write(file, List.of(
                "U001|Admin|a@x.com|secret1|0711111111|addr|Admin",
                "U002|broken line with no pipes",
                "U003|Driver1|d@x.com|secret2|0712222222|addr|Driver"));

        UserFileHandler handler = new UserFileHandler(new FileHandler(), file.toString());

        List<User> users = handler.loadUsers();
        assertEquals(2, users.size());

        handler.saveUsers(users);
        assertTrue(Files.readAllLines(file).contains("U002|broken line with no pipes"));
    }

    @Test
    void damagedBikeLineDoesNotBreakTheBikePageAndIsKept() throws Exception {
        Path file = tempDir.resolve("bikes.txt");
        Files.write(file, List.of(
                "B001|Honda|Motorbike|Kandy|800.0|true|Active",
                "B002|Bad|Scooter|Colombo|notANumber|true|Active"));

        BikeFileHandler handler = new BikeFileHandler(file.toString());

        List<Bike> bikes = handler.loadBikes();
        assertEquals(1, bikes.size());

        handler.saveBikes(bikes);
        assertTrue(Files.readAllLines(file).contains("B002|Bad|Scooter|Colombo|notANumber|true|Active"));
    }

    @Test
    void updatingABookingKeepsDamagedLinesAndDoesNotReuseTheirId() throws Exception {
        Path file = tempDir.resolve("bookings.txt");
        Files.write(file, List.of(
                "BID001|U002|B001|2026-11-10|Pending",
                "BID007|only|three"));

        BookingFileHandler handler = new BookingFileHandler(file.toString());

        handler.update(new Booking("BID001", "U002", "B001", LocalDate.parse("2026-11-10"), "Confirmed"));

        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.contains("BID007|only|three"));
        assertTrue(lines.contains("BID001|U002|B001|2026-11-10|Confirmed"));

        // BID007 is damaged, but its number is still taken
        Booking created = handler.create(new Booking(null, "U005", "B002", LocalDate.parse("2026-12-01"), "Pending"));
        assertEquals("BID008", created.getBookingId());
    }

    @Test
    void oldRideRequestLinesWithSixFieldsStillLoad() throws Exception {
        Path file = tempDir.resolve("rideRequests.txt");
        Files.write(file, List.of("RRID001|BID001|U002|B001|2026-11-10|Approved"));

        RideRequestFileHandler handler = new RideRequestFileHandler(file.toString());

        RideRequest[] all = handler.findAll();
        assertEquals(1, all.length);
        assertEquals("", all[0].getRideId());

        handler.update(all[0]);                      // rewritten with seven fields
        assertEquals("RRID001|BID001|U002|B001|2026-11-10|Approved|", Files.readAllLines(file).get(0));
    }

    @Test
    void pipesTypedByUsersCannotBreakALine() {
        User user = new User("U009", "Sam|Perera", "s@x.com", "secret1", "0711111111", "12|Main\nRoad", "Customer");

        User back = User.fromFileString(user.toFileString());
        assertEquals("Sam/Perera", back.getName());
        assertEquals("12/Main Road", back.getAddress());
    }

    @Test
    void invalidRecordsThrowInsteadOfCrashing() {
        assertThrows(IllegalArgumentException.class, () -> User.fromFileString("U001|only|three"));
        assertThrows(IllegalArgumentException.class, () -> Bike.fromFileString("B001|x"));
        assertThrows(IllegalArgumentException.class, () -> Ride.fromFileString("R001|U003|B001|a|b|not-a-date|10:00|2|Open"));
    }
}
