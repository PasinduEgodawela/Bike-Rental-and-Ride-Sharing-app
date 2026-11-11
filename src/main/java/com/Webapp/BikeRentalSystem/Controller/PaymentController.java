package com.Webapp.BikeRentalSystem.Controller;

import com.Webapp.BikeRentalSystem.Model.Billing;
import com.Webapp.BikeRentalSystem.Model.Payment;
import com.Webapp.BikeRentalSystem.Model.User;
import com.Webapp.BikeRentalSystem.Service.PaymentService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 1. Display payments where admin sees all and the customer only sees theirs
    @GetMapping
    public String getAllPayments(HttpSession session, Model model) {
        User loggedInUser = currentUser(session);
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        List<Payment> payments;
        try {
            payments = paymentService.getPaymentsFor(loggedInUser);
        } catch (Exception e) {
            payments = new ArrayList<>();       // the page needs a list even when reading fails
            model.addAttribute("error", "Error loading payments: " + e.getMessage());
        }

        model.addAttribute("payments", payments);
        model.addAttribute("role", loggedInUser.getRole());

        return "payments/list";
    }

    // 2. display booking Details when payment starts
    @GetMapping("/booking")
    public String getBooking(@RequestParam String id, HttpSession session, Model model, RedirectAttributes ra) {
        User loggedInUser = currentUser(session);
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        try {
            Billing billing = paymentService.getBilling(id, loggedInUser);
            model.addAttribute("booking", billing);
            model.addAttribute("today", LocalDate.now());
            return "payments/booking";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings";
        }
    }

    // 3. calculates rental cost
    @GetMapping("/calculate")
    public String calculate(
            @RequestParam String id,
            @RequestParam String start,
            @RequestParam String end,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User loggedInUser = currentUser(session);
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        try {
            Billing billing = paymentService.getBilling(id, loggedInUser);
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);

            int days = billing.calculateRentalDays(startDate, endDate);
            double total = billing.calculateTotal(days);

            model.addAttribute("booking", billing);
            model.addAttribute("days", days);
            model.addAttribute("pricePerDay", billing.getPricePerDay());
            model.addAttribute("total", total);
            model.addAttribute("start", start);
            model.addAttribute("end", end);

            return "payments/calculate";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addAttribute("id", id);
            return "redirect:/payments/booking";
        }
    }

    // 4. displays the payment form
    @GetMapping("/pay")
    public String paymentForm(
            @RequestParam String bookingId,
            @RequestParam String start,
            @RequestParam String end,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User loggedInUser = currentUser(session);
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        try {
            paymentService.getBilling(bookingId, loggedInUser);   // the booking must be yours and still active
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings";
        }

        model.addAttribute("bookingId", bookingId);
        model.addAttribute("start", start);
        model.addAttribute("end", end);

        return "payments/pay";
    }

    // 5. processes payment
    @PostMapping("/pay")
    public String pay(
            @RequestParam String bookingId,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam String method,
            @RequestParam(required = false) String cardName,
            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false) String expiry,
            @RequestParam(required = false) String cvv,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User loggedInUser = currentUser(session);
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        try {
            Billing billing = paymentService.getBilling(bookingId, loggedInUser);
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);

            Payment payment;

            if ("card".equals(method)) {
                payment = paymentService.pay(
                        billing, startDate, endDate, method,
                        cardName, cardNumber, expiry, cvv
                );
            } else {
                payment = paymentService.pay(
                        billing, startDate, endDate, method
                );
            }

            model.addAttribute("payment", payment);
            return "payments/success";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Payment failed: " + e.getMessage());
            ra.addAttribute("bookingId", bookingId);
            ra.addAttribute("start", start);
            ra.addAttribute("end", end);
            return "redirect:/payments/pay";
        }
    }

    // 6. updates payment status
    @PostMapping("/update")
    public String update(
            @RequestParam String paymentId,
            @RequestParam String status,
            HttpSession session,
            RedirectAttributes ra) {

        User loggedInUser = currentUser(session);
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        try {
            paymentService.updateStatus(paymentId, status);
            ra.addFlashAttribute("message", "Payment status updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/payments";
    }

    // 7. delete payment(only by admin)
    @PostMapping("/delete/{paymentId}")
    public String deletePayment(
            @PathVariable String paymentId,
            HttpSession session,
            RedirectAttributes ra) {

        User loggedInUser = currentUser(session);
        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        try {
            paymentService.deletePayment(paymentId);
            ra.addFlashAttribute("message", "Payment deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/payments";
    }

    // helper method to get logged-in user from session
    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("loggedInUser");
        if (user instanceof User) {
            return (User) user;
        }
        return null;
    }
}
