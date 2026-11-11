package com.Webapp.BikeRentalSystem.Model;

import java.io.IOException;
import java.util.ArrayList;

public interface PaymentRepository {
    void save(Payment payment) throws IOException;                           // Create
    ArrayList<Payment> findAll() throws IOException;                         // Read
    boolean update(String paymentId, String newStatus) throws IOException;   // Update
    boolean delete(String paymentId) throws IOException;                     // Delete
}
