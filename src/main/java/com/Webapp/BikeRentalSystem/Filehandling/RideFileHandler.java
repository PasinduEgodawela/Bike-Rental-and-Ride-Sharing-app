package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.Ride;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RideFileHandler {

    private final FileHandler fileHandler;
    private final String path;

    // ride.file.path = full path of rides.txt (set in application.properties)
    public RideFileHandler(FileHandler fileHandler,
                           @Value("${ride.file.path}") String filePath) {
        this.fileHandler = fileHandler;
        this.path = filePath;
    }

    // Reads rides.txt and turns every line into a Ride object
    public List<Ride> loadRides() {
        List<Ride> rides = new ArrayList<>();
        for (String line : fileHandler.readLines(path)) {
            try {
                rides.add(Ride.fromFileString(line));
            } catch (RuntimeException ex) {
                System.out.println("Skipping unreadable ride line: " + line);
            }
        }
        return rides;
    }

    // Writes the whole list back to rides.txt.
    // Lines that could not be read are kept at the end instead of being deleted.
    public void saveRides(List<Ride> rides) {
        List<String> lines = new ArrayList<>();
        for (Ride r : rides) {
            lines.add(r.toFileString());
        }

        for (String line : fileHandler.readLines(path)) {
            try {
                Ride.fromFileString(line);
            } catch (RuntimeException ex) {
                lines.add(line);
            }
        }

        fileHandler.writeLines(path, lines);
    }

    // R001, R002, R003 ...
    public String nextId(List<Ride> rides) {
        List<String> ids = new ArrayList<>();
        for (Ride r : rides) {
            ids.add(r.getRideId());
        }
        return fileHandler.nextId("R", ids);
    }
}
