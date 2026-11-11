package com.Webapp.BikeRentalSystem.Model;

import java.time.LocalDate;

public class Cash extends Payment {

    public Cash(String paymentId, String bookingId, String userId, double amount, LocalDate paymentDate) {
        super(paymentId, bookingId, userId, amount, paymentDate, "Pending", "Cash");
    }

    public void processPayment() {
        setStatus("Pending");
    }
}
