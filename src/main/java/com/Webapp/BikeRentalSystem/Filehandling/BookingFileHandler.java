package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.Booking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class BookingFileHandler {

    private final Path file;

    public BookingFileHandler(@Value("${booking.file.path}") String path) throws IOException {

        file = Paths.get(path).toAbsolutePath().normalize();

        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        if (Files.notExists(file)) {
            Files.createFile(file);
        }
    }

    // Every non-empty line of the file, exactly as it is stored
    private List<String> readRawLines() throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    public synchronized Booking[] findAll() throws IOException {
        List<Booking> bookings = new ArrayList<>();

        for (String line : readRawLines()) {
            Booking booking = parse(line);

            if (booking != null) {
                bookings.add(booking);
            } else {
                System.out.println("Skipping unreadable booking line: " + line);
            }
        }

        return bookings.toArray(new Booking[0]);
    }

    public synchronized Booking findById(String id) throws IOException {

        for (Booking booking : findAll()) {
            if (booking.getBookingId().equals(id)) {
                return booking;
            }
        }

        return null;
    }

    public synchronized Booking create(Booking save) throws IOException {

        String newId = RecordIds.next("BID", readRawLines());

        Booking saved = new Booking(
                newId,
                TextClean.clean(save.getUserId()),
                TextClean.clean(save.getBikeId()),
                save.getBookingDate(),
                TextClean.clean(save.getStatus())
        );

        List<String> lines = readRawLines();
        lines.add(format(saved));
        Files.write(file, lines, StandardCharsets.UTF_8);

        return saved;
    }

    // Replaces one booking. Lines that cannot be read are written back unchanged.
    public synchronized boolean update(Booking update) throws IOException {

        List<String> lines = readRawLines();
        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            Booking booking = parse(line);

            if (booking != null && booking.getBookingId().equals(update.getBookingId())) {
                result.add(format(update));
                found = true;
            } else {
                result.add(line);
            }
        }

        if (!found) {
            return false;
        }

        Files.write(file, result, StandardCharsets.UTF_8);
        return true;
    }

    // Removes one booking. Lines that cannot be read are written back unchanged.
    public synchronized boolean delete(String id) throws IOException {

        List<String> lines = readRawLines();
        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            Booking booking = parse(line);

            if (booking != null && booking.getBookingId().equals(id)) {
                found = true;
            } else {
                result.add(line);
            }
        }

        if (!found) {
            return false;
        }

        Files.write(file, result, StandardCharsets.UTF_8);
        return true;
    }

    public synchronized Booking[] getBookingsByUserId(String userId) throws IOException {

        List<Booking> userBookings = new ArrayList<>();

        for (Booking booking : findAll()) {

            if (booking.getUserId().equals(userId)) {
                userBookings.add(booking);
            }
        }

        return userBookings.toArray(new Booking[0]);
    }


    private String format(Booking booking) {
        return TextClean.clean(booking.getBookingId()) + "|"
                + TextClean.clean(booking.getUserId()) + "|"
                + TextClean.clean(booking.getBikeId()) + "|"
                + booking.getBookingDate() + "|"
                + TextClean.clean(booking.getStatus());
    }

    // Returns null when the line is not a valid booking
    private Booking parse(String line) {
        String[] booking = line.split("\\|", -1);

        if (booking.length != 5) {
            return null;
        }

        try {
            return new Booking(
                    booking[0],
                    booking[1],
                    booking[2],
                    LocalDate.parse(booking[3]),
                    booking[4]
            );
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
