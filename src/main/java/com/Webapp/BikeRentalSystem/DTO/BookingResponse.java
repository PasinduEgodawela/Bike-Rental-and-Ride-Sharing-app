package com.Webapp.BikeRentalSystem.DTO;

public class BookingResponse {

    private int status;
    private String message;
    private BookingDTO booking;
    private BookingDTO[] bookings;

    public BookingResponse() {
    }

    public BookingResponse(int status, String message, BookingDTO booking, BookingDTO[] bookings) {
        this.status = status;
        this.message = message;
        this.booking = booking;
        this.bookings = bookings;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BookingDTO getBooking() {
        return booking;
    }

    public void setBooking(BookingDTO booking) {
        this.booking = booking;
    }

    public BookingDTO[] getBookings() {
        return bookings;
    }

    public void setBookings(BookingDTO[] bookings) {
        this.bookings = bookings;
    }

    public static BookingResponse message(int status, String message) {
        return new BookingResponse(status, message, null, null);
    }
}
