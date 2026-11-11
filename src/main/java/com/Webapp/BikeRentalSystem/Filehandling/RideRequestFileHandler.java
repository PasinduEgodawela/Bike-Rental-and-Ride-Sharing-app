package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.RideRequest;
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

/**
 * File format (one request per line):
 * requestId|bookingId|passengerId|bikeId|requestDate|status|rideId
 * An older line with only six fields (no rideId) can still be read.
 */
@Component
public class RideRequestFileHandler {

    private final Path file;

    public RideRequestFileHandler(@Value("${rideRequest.file.path}") String path) throws IOException {

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

    public synchronized RideRequest[] findAll() throws IOException {

        List<RideRequest> requests = new ArrayList<>();

        for (String line : readRawLines()) {

            RideRequest request = parse(line);

            if (request != null) {
                requests.add(request);
            } else {
                System.out.println("Skipping unreadable ride request line: " + line);
            }
        }

        return requests.toArray(new RideRequest[0]);
    }

    public synchronized RideRequest findById(String id) throws IOException {

        for (RideRequest request : findAll()) {

            if (request.getRequestId().equals(id)) {
                return request;
            }
        }

        return null;
    }

    public synchronized RideRequest create(RideRequest save) throws IOException {

        String newId = RecordIds.next("RRID", readRawLines());

        RideRequest saved = new RideRequest(
                newId,
                TextClean.clean(save.getBookingId()),
                TextClean.clean(save.getPassengerId()),
                TextClean.clean(save.getBikeId()),
                save.getRequestDate(),
                TextClean.clean(save.getStatus()),
                TextClean.clean(save.getRideId())
        );

        List<String> lines = readRawLines();
        lines.add(format(saved));
        Files.write(file, lines, StandardCharsets.UTF_8);

        return saved;
    }

    // Replaces one request. Lines that cannot be read are written back unchanged.
    public synchronized boolean update(RideRequest update) throws IOException {

        List<String> lines = readRawLines();
        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            RideRequest request = parse(line);

            if (request != null && request.getRequestId().equals(update.getRequestId())) {
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

    // Removes one request. Lines that cannot be read are written back unchanged.
    public synchronized boolean delete(String id) throws IOException {

        List<String> lines = readRawLines();
        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            RideRequest request = parse(line);

            if (request != null && request.getRequestId().equals(id)) {
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


    private String format(RideRequest request) {

        return TextClean.clean(request.getRequestId()) + "|"
                + TextClean.clean(request.getBookingId()) + "|"
                + TextClean.clean(request.getPassengerId()) + "|"
                + TextClean.clean(request.getBikeId()) + "|"
                + request.getRequestDate() + "|"
                + TextClean.clean(request.getStatus()) + "|"
                + TextClean.clean(request.getRideId());
    }

    // Returns null when the line is not a valid ride request
    private RideRequest parse(String line) {

        String[] request = line.split("\\|", -1);

        if (request.length != 6 && request.length != 7) {
            return null;
        }

        String rideId = request.length == 7 ? request[6].trim() : "";

        try {
            return new RideRequest(
                    request[0].trim(),
                    request[1].trim(),
                    request[2].trim(),
                    request[3].trim(),
                    LocalDate.parse(request[4].trim()),
                    request[5].trim(),
                    rideId
            );

        } catch (RuntimeException ex) {
            return null;
        }
    }
}
