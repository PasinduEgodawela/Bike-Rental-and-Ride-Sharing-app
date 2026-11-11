package com.Webapp.BikeRentalSystem.Filehandling;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileHandler {

    public synchronized List<String> readLines(String path) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) return new ArrayList<>();

            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(p)) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public synchronized void writeLines(String path, List<String> lines) {
        try {
            Path p = Paths.get(path);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            Files.write(p, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // U001, U002 ... : the highest number + 1. An ID that is not in the right form is skipped.
    public String nextId(String prefix, List<String> ids) {
        int max = 0;
        for (String id : ids) {
            if (id != null && id.startsWith(prefix)) {
                try {
                    int number = Integer.parseInt(id.substring(prefix.length()));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException e) {
                    // not counted
                }
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }
}
