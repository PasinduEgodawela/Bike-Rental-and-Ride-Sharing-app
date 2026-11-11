package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.Review;
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
public class ReviewFileHandler {

    private final Path file;

    public ReviewFileHandler(@Value("${review.file.path}") String path) throws IOException {

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

    // Returns null when the line is not a valid review
    private Review parse(String line) {
        try {
            return Review.fromFileString(line);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public synchronized Review[] findAll() throws IOException {
        List<Review> reviews = new ArrayList<>();

        for (String line : readRawLines()) {
            Review review = parse(line);

            if (review != null) {
                reviews.add(review);
            } else {
                System.out.println("Skipping unreadable review line: " + line);
            }
        }

        return reviews.toArray(new Review[0]);
    }

    public synchronized Review findById(String id) throws IOException {
        for (Review review : findAll()) {
            if (review.getReviewId().equals(id)) {
                return review;
            }
        }
        return null;
    }

    public synchronized Review create(Review save) throws IOException {

        Review saved = new Review(
                RecordIds.next("RV", readRawLines()),
                save.getUserId(),
                save.getBikeId(),
                save.getRating(),
                save.getComment(),
                save.getReviewDate()
        );

        List<String> lines = readRawLines();
        lines.add(saved.toFileString());
        Files.write(file, lines, StandardCharsets.UTF_8);

        return saved;
    }

    // Replaces one review. Lines that cannot be read are written back unchanged.
    public synchronized boolean update(Review update) throws IOException {

        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : readRawLines()) {
            Review review = parse(line);

            if (review != null && review.getReviewId().equals(update.getReviewId())) {
                result.add(update.toFileString());
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

    // Removes one review. Lines that cannot be read are written back unchanged.
    public synchronized boolean delete(String id) throws IOException {

        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : readRawLines()) {
            Review review = parse(line);

            if (review != null && review.getReviewId().equals(id)) {
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
}
