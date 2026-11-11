package com.Webapp.BikeRentalSystem.Model;

import java.time.LocalDate;
import java.time.YearMonth;

public class Card extends Payment {
    private String cardholderName = "";
    private String cardNumber = "";
    private String expiry = "";
    private String cvv = "";

    public Card(String paymentId, String bookingId, String userId, double amount, LocalDate paymentDate) {
        super(paymentId, bookingId, userId, amount, paymentDate, "Pending", "Card");
    }

    public void setCardDetails(String cardholderName, String cardNumber, String expiry, String cvv) {
        this.cardholderName = cardholderName.trim();
        this.cardNumber = cardNumber.replace(" ", "");
        this.expiry = expiry.trim();
        this.cvv = cvv.trim();
    }

    public void processPayment() throws PaymentException {
        if (cardholderName.isEmpty()) {
            throw new PaymentException("Cardholder name is required.");
        }
        if (!cardNumber.matches("\\d{16}")) {
            throw new PaymentException("Card number must be 16 digits.");
        }
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new PaymentException("Expiry date must look like MM/YY.");
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3));
        if (YearMonth.of(year, month).isBefore(YearMonth.now())) {
            throw new PaymentException("This card has expired.");
        }
        if (!cvv.matches("\\d{3,4}")) {
            throw new PaymentException("CVV must be 3 or 4 digits.");
        }
        setStatus("Paid");
    }
}
