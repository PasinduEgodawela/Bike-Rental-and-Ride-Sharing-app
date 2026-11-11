package com.Webapp.BikeRentalSystem.Model;

import java.time.LocalDate;

public class Online extends Payment {

    public Online(String paymentId, String bookingId, String userId, double amount, LocalDate paymentDate) {
        super(paymentId, bookingId, userId, amount, paymentDate, "Pending", "Online");
    }

    public void processPayment() {
        setStatus("Paid");
    }
}
