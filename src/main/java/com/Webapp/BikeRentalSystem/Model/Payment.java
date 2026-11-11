package com.Webapp.BikeRentalSystem.Model;

import com.Webapp.BikeRentalSystem.Model.PaymentException;

import java.time.LocalDate;

public class Payment {
    private String paymentId;
    private String bookingId;
    private String userId;
    private double amount;
    private LocalDate paymentDate;
    private String status;
    private String method;

    public Payment() {
        this("P000", "B000", "U000", 0.0, LocalDate.now(), "Pending", "Unknown");
    }

    public Payment(String paymentId, String bookingId, String userId, double amount, LocalDate paymentDate, String status, String method) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = status;
        this.method = method;
    }

    /** Children replace this. By default nothing is checked. */
    public void processPayment() throws PaymentException {
        this.status = "Pending";
    }


    // Next ID = highest existing number + 1. Counting the records would repeat an ID
    // after a payment is deleted (P001, P002 -> delete P001 -> count gives P002 again).
    public static String createId(java.util.List<Payment> existingPayments) {
        int highest = 0;
        for (Payment p : existingPayments) {
            String id = p.getPaymentId();
            if (id != null && id.startsWith("P")) {
                try {
                    int number = Integer.parseInt(id.substring(1));
                    if (number > highest) {
                        highest = number;
                    }
                } catch (NumberFormatException ignored) {
                    // an ID in some other format is simply not counted
                }
            }
        }
        return String.format("P%03d", highest + 1);
    }

    public void displayPaymentDetails() {
        displayPaymentDetails("Payment");
    }

    public void displayPaymentDetails(String title) {
        System.out.println(title + " " + paymentId + " | booking " + bookingId + " | Rs." + amount
                + " | " + method + " | " + status + " | " + paymentDate);
    }

    public String getPaymentId()       { return paymentId; }
    public String getBookingId()       { return bookingId; }
    public String getUserId()          { return userId; }
    public double getAmount()          { return amount; }
    public LocalDate getPaymentDate()  { return paymentDate; }
    public String getStatus()          { return status; }
    public String getMethod()          { return method; }

    public void setPaymentId(String paymentId)        { this.paymentId = paymentId; }
    public void setBookingId(String bookingId)        { this.bookingId = bookingId; }
    public void setUserId(String userId)              { this.userId = userId; }
    public void setAmount(double amount)              { this.amount = amount; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public void setStatus(String status)              { this.status = status; }
    public void setMethod(String method)              { this.method = method; }
}
