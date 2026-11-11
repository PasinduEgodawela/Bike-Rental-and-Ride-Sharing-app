package com.Webapp.BikeRentalSystem.Model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The billing details for one booking: who is paying, the price per day and
 * how the total is calculated.
 *
 * It only holds data and does the calculation. PaymentService fills it in
 * from the real booking, bike and user records.
 */
public class Billing {
    private String bookingId;
    private String userId;
    private String bikeId;
    private double pricePerDay;
    private LocalDate bookingDate;
    private String customerName;
    private String email;
    private String phone;

    public Billing(String bookingId, String userId, String bikeId, double pricePerDay,
                   LocalDate bookingDate, String customerName, String email, String phone) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.bikeId = bikeId;
        this.pricePerDay = pricePerDay;
        this.bookingDate = bookingDate;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
    }

    public int calculateRentalDays(LocalDate start, LocalDate end) throws PaymentException {
        if (start == null || end == null) {
            throw new PaymentException("Start date and end date are required.");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new PaymentException("Start date cannot be in the past.");
        }
        if (end.isBefore(start)) {
            throw new PaymentException("End date must be on or after the start date.");
        }
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    public double calculateTotal(int days) {
        return days * pricePerDay;
    }

    public double calculateTotal(LocalDate start, LocalDate end) throws PaymentException {
        return calculateTotal(calculateRentalDays(start, end));
    }

    // The date to suggest as the start date: the booking date, or today if that has passed
    public LocalDate getSuggestedStart() {
        if (bookingDate == null || bookingDate.isBefore(LocalDate.now())) {
            return LocalDate.now();
        }
        return bookingDate;
    }

    public String getBookingId() { return bookingId; }
    public String getUserId() { return userId; }
    public String getBikeId() { return bikeId; }
    public double getPricePerDay() { return pricePerDay; }
    public LocalDate getBookingDate() { return bookingDate; }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setBikeId(String bikeId) {
        this.bikeId = bikeId;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
