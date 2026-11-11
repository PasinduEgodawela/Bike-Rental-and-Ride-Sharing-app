package com.Webapp.BikeRentalSystem.Model;

import java.io.IOException;
import java.util.ArrayList;

/** Interface: the CRUD operations for payments. Create, Read, Update, Delete. */
public interface PaymentRepository {
    void save(Payment payment) throws IOException;                           // Create
    ArrayList<Payment> findAll() throws IOException;                         // Read
    boolean update(String paymentId, String newStatus) throws IOException;   // Update
    boolean delete(String paymentId) throws IOException;                     // Delete
}
