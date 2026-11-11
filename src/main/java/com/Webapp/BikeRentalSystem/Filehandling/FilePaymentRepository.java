package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.Payment;
import com.Webapp.BikeRentalSystem.Model.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Payments are stored in the data folder like every other record:
 * paymentId|bookingId|userId|amount|paymentDate|status|method
 */
@Repository
public class FilePaymentRepository implements PaymentRepository {

    private final Path file;

    public FilePaymentRepository(@Value("${payment.file.path}") String path) throws IOException {
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

    // Returns null when the line is not a valid payment
    private Payment parse(String line) {
        String[] p = line.split("\\|", -1);

        if (p.length != 7) {
            return null;
        }

        try {
            return new Payment(p[0], p[1], p[2], Double.parseDouble(p[3]),
                    LocalDate.parse(p[4]), p[5], p[6]);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public synchronized void save(Payment payment) throws IOException {            // CREATE
        List<String> lines = readRawLines();
        lines.add(toLine(payment));
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    @Override
    public synchronized ArrayList<Payment> findAll() throws IOException {          // READ
        ArrayList<Payment> payments = new ArrayList<>();

        for (String line : readRawLines()) {
            Payment payment = parse(line);

            if (payment != null) {
                payments.add(payment);
            } else {
                System.out.println("Skipping unreadable payment line: " + line);
            }
        }
        return payments;
    }

    @Override
    public synchronized boolean update(String paymentId, String newStatus) throws IOException {   // UPDATE
        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : readRawLines()) {
            Payment payment = parse(line);

            if (payment != null && payment.getPaymentId().equals(paymentId)) {
                payment.setStatus(newStatus);
                result.add(toLine(payment));
                found = true;
            } else {
                result.add(line);          // other lines, even damaged ones, stay as they are
            }
        }

        if (found) {
            Files.write(file, result, StandardCharsets.UTF_8);
        }
        return found;
    }

    @Override
    public synchronized boolean delete(String paymentId) throws IOException {                     // DELETE
        List<String> result = new ArrayList<>();
        boolean found = false;

        for (String line : readRawLines()) {
            Payment payment = parse(line);

            if (payment != null && payment.getPaymentId().equals(paymentId)) {
                found = true;
            } else {
                result.add(line);
            }
        }

        if (found) {
            Files.write(file, result, StandardCharsets.UTF_8);
        }
        return found;
    }

    private String toLine(Payment p) {
        return TextClean.clean(p.getPaymentId()) + "|" + TextClean.clean(p.getBookingId()) + "|"
                + TextClean.clean(p.getUserId()) + "|" + p.getAmount() + "|"
                + p.getPaymentDate() + "|" + TextClean.clean(p.getStatus()) + "|"
                + TextClean.clean(p.getMethod());
    }
}
