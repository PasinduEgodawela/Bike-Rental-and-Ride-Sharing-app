package com.Webapp.BikeRentalSystem.Service;

import com.Webapp.BikeRentalSystem.DTO.BookingDTO;
import com.Webapp.BikeRentalSystem.DTO.BookingResponse;
import com.Webapp.BikeRentalSystem.Model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final BookingService bookingService;
    private final BikeService bikeService;
    private final UserService userService;

    public PaymentService(PaymentRepository repository, BookingService bookingService,
                          BikeService bikeService, UserService userService) {
        this.repository = repository;
        this.bookingService = bookingService;
        this.bikeService = bikeService;
        this.userService = userService;
    }

    /**
     * Builds the billing details from the real booking, bike and user records.
     * The booking must exist, must belong to this user (an Admin can see any), and must still be
     * active (Pending or Confirmed).
     */
    public Billing getBilling(String bookingId, User user) throws PaymentException, IOException {
        BookingResponse response = bookingService.getBookingById(bookingId);
        BookingDTO booking = response.getBooking();

        if (booking == null) {
            throw new PaymentException("Booking not found: " + bookingId);
        }
        if (!"Admin".equals(user.getRole()) && !booking.getUserId().equals(user.getUserId())) {
            throw new PaymentException("This booking does not belong to you.");
        }
        if (!"Pending".equals(booking.getStatus()) && !"Confirmed".equals(booking.getStatus())) {
            throw new PaymentException("This booking is " + booking.getStatus() + ", so it cannot be paid.");
        }

        Bike bike = bikeService.getBikeById(booking.getBikeId());
        if (bike == null) {
            throw new PaymentException("The bike for this booking no longer exists.");
        }

        User customer = userService.getUserById(booking.getUserId());
        if (customer == null) {
            throw new PaymentException("The customer for this booking no longer exists.");
        }

        return new Billing(booking.getBookingId(), booking.getUserId(), booking.getBikeId(),
                bike.getPricePerDay(), booking.getBookingDate(),
                customer.getName(), customer.getEmail(), customer.getPhone());
    }

    public synchronized Payment pay(Billing billing, LocalDate start, LocalDate end, String method)
            throws PaymentException, IOException {
        double amount = billing.calculateTotal(start, end);
        checkNotAlreadyPaid(billing.getBookingId());

        String id = Payment.createId(repository.findAll());
        LocalDate today = LocalDate.now();

        Payment payment;
        if ("cash".equals(method)) {
            payment = new Cash(id, billing.getBookingId(), billing.getUserId(), amount, today);
        } else if ("online".equals(method)) {
            payment = new Online(id, billing.getBookingId(), billing.getUserId(), amount, today);
        } else if ("card".equals(method)) {
            throw new PaymentException("Card details are required.");
        } else {
            throw new PaymentException("Please choose a payment method.");
        }
        return process(payment);
    }

    public synchronized Payment pay(Billing billing, LocalDate start, LocalDate end, String method,
                       String cardName, String cardNumber, String expiry, String cvv)
            throws PaymentException, IOException {
        double amount = billing.calculateTotal(start, end);
        checkNotAlreadyPaid(billing.getBookingId());

        String id = Payment.createId(repository.findAll());

        Card card = new Card(id, billing.getBookingId(), billing.getUserId(), amount, LocalDate.now());
        card.setCardDetails(cardName == null ? "" : cardName,
                cardNumber == null ? "" : cardNumber,
                expiry == null ? "" : expiry,
                cvv == null ? "" : cvv);
        return process(card);
    }

    // A booking can have only one payment. A cash payment is saved as Pending until the
    // Admin marks it Paid, so Pending counts too, otherwise one booking could collect
    // several cash payments.
    private void checkNotAlreadyPaid(String bookingId) throws PaymentException, IOException {
        for (Payment p : repository.findAll()) {
            if (p.getBookingId().equals(bookingId)) {
                throw new PaymentException("Booking " + bookingId + " already has a payment ("
                        + p.getPaymentId() + ", " + p.getStatus() + ").");
            }
        }
    }

    private Payment process(Payment payment) throws PaymentException, IOException {
        payment.processPayment();
        repository.save(payment);
        payment.displayPaymentDetails("New payment");
        return payment;
    }

    public ArrayList<Payment> getAllPayments() throws IOException {
        return repository.findAll();
    }

    // An Admin sees every payment. Anyone else sees only their own.
    public ArrayList<Payment> getPaymentsFor(User user) throws IOException {
        if ("Admin".equals(user.getRole())) {
            return repository.findAll();
        }

        ArrayList<Payment> own = new ArrayList<>();
        for (Payment p : repository.findAll()) {
            if (p.getUserId().equals(user.getUserId())) {
                own.add(p);
            }
        }
        return own;
    }

    public synchronized void updateStatus(String paymentId, String newStatus) throws PaymentException, IOException {
        if (!"Paid".equals(newStatus) && !"Pending".equals(newStatus)) {
            throw new PaymentException("Status must be Paid or Pending.");
        }

        if ("Paid".equals(newStatus)) {
            // keep the one-Paid-payment-per-booking rule
            Payment target = null;
            for (Payment p : repository.findAll()) {
                if (p.getPaymentId().equals(paymentId)) {
                    target = p;
                }
            }
            if (target != null) {
                for (Payment p : repository.findAll()) {
                    if (!p.getPaymentId().equals(paymentId)
                            && p.getBookingId().equals(target.getBookingId())
                            && "Paid".equals(p.getStatus())) {
                        throw new PaymentException("Booking " + target.getBookingId()
                                + " already has a Paid payment (" + p.getPaymentId() + ").");
                    }
                }
            }
        }

        if (!repository.update(paymentId, newStatus)) {
            throw new PaymentException("Payment not found: " + paymentId);
        }
    }

    public synchronized void deletePayment(String paymentId) throws PaymentException, IOException {
        if (!repository.delete(paymentId)) {
            throw new PaymentException("Payment not found: " + paymentId);
        }
    }
}
