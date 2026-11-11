package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.Bike;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class BikeFileHandler {

    private final Path file;

    // The file location comes from application.properties (bike.file.path)
    public BikeFileHandler(@Value("${bike.file.path}") String path) {
        this.file = Paths.get(path).toAbsolutePath().normalize();
    }

    // Every non-empty line of the file
    private List<String> readRawLines() throws IOException {
        List<String> lines = new ArrayList<>();

        if (Files.notExists(file)) {
            return lines;               // no file yet means no bikes yet
        }

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    // READ: file -> list of bikes. A line that cannot be read is skipped (and printed)
    // so the bikes page still opens.
    public synchronized List<Bike> loadBikes() throws IOException {
        List<Bike> bikes = new ArrayList<>();

        for (String line : readRawLines()) {
            try {
                bikes.add(Bike.fromFileString(line));
            } catch (IllegalArgumentException e) {
                System.out.println("Skipping unreadable bike line: " + line);
            }
        }
        return bikes;
    }

    // WRITE: list of bikes -> file. Lines that could not be read are kept at the end.
    public synchronized void saveBikes(List<Bike> bikes) throws IOException {
        List<String> lines = new ArrayList<>();

        for (Bike bike : bikes) {
            lines.add(bike.toFileString());
        }

        for (String line : readRawLines()) {
            try {
                Bike.fromFileString(line);
            } catch (IllegalArgumentException e) {
                lines.add(line);
            }
        }

        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
