package com.Webapp.BikeRentalSystem.Controller;

import com.Webapp.BikeRentalSystem.DTO.BookingDTO;
import com.Webapp.BikeRentalSystem.DTO.BookingResponse;
import com.Webapp.BikeRentalSystem.Model.Bike;
import com.Webapp.BikeRentalSystem.Model.User;
import com.Webapp.BikeRentalSystem.Service.BikeService;
import com.Webapp.BikeRentalSystem.Service.BookingService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BikeService bikeService;

    public BookingController(BookingService bookingService, BikeService bikeService) {
        this.bookingService = bookingService;
        this.bikeService = bikeService;
    }

    // The bikes in the New Booking drop-down: only bikes that are in service (Active)
    private void addBikeChoices(Model model) {
        List<Bike> choices = new ArrayList<>();
        try {
            for (Bike bike : bikeService.getAllBikes()) {
                if ("Active".equals(bike.getStatus())) {
                    choices.add(bike);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read the bike list: " + e.getMessage());
        }
        model.addAttribute("bikes", choices);
    }

    @GetMapping
    public String getAllBookings(HttpSession session, Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        if ("Admin".equalsIgnoreCase(loggedInUser.getRole())) {

            BookingResponse response = bookingService.getAllBookings();

            model.addAttribute("bookings", response.getBookings());

            return "admin/bookings";
        }

        BookingResponse response = bookingService.getBookingByUserId(loggedInUser.getUserId());

        model.addAttribute("bookings", response.getBookings());

        return "bookings/list";
    }

    @GetMapping("/{id}")
    public String getBookingByUserId(@PathVariable String id, HttpSession session, Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        if (!"Admin".equals(loggedInUser.getRole()) && !loggedInUser.getUserId().equals(id)) {
            return "redirect:/bookings/" + loggedInUser.getUserId();
        }

        BookingResponse response = bookingService.getBookingByUserId(id);

        model.addAttribute("bookings", response.getBookings());

        return "bookings/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {

        model.addAttribute("booking", new BookingDTO());
        addBikeChoices(model);

        return "bookings/create";
    }

    @GetMapping("/new")
    public String showNewBookingForm(@RequestParam("bikeId") String bikeId, Model model) {

        BookingDTO booking = new BookingDTO();
        booking.setBikeId(bikeId);

        model.addAttribute("booking", booking);
        addBikeChoices(model);

        return "bookings/create";
    }

    @PostMapping("/save")
    public String saveBooking(HttpSession session,
                              @Valid @ModelAttribute("booking") BookingDTO bookingDTO,
                              BindingResult result,
                              Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        if (result.hasErrors()) {
            addBikeChoices(model);
            return "bookings/create";
        }

        BookingResponse response = bookingService.saveBooking(loggedInUser.getUserId(), bookingDTO);

        if (response.getStatus() != 200) {
            model.addAttribute("error", response.getMessage());
            addBikeChoices(model);
            return "bookings/create";
        }

        return "redirect:/bookings";
    }

    @GetMapping("/edit/{bookingId}")
    public String editForm(@PathVariable String bookingId,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        BookingResponse response = bookingService.getBookingById(bookingId);
        BookingDTO booking = response.getBooking();

        if (booking == null) {
            ra.addFlashAttribute("error", "Booking not found.");
            return "redirect:/bookings";
        }

        // only the owner or an Admin can open the edit page
        if (!"Admin".equals(loggedInUser.getRole())
                && !booking.getUserId().equals(loggedInUser.getUserId())) {
            ra.addFlashAttribute("error", "You can only edit your own bookings.");
            return "redirect:/bookings";
        }

        model.addAttribute("booking", booking);
        model.addAttribute("isAdmin", "Admin".equals(loggedInUser.getRole()));

        return "bookings/edit";
    }

    @PostMapping("/update/{bookingId}")
    public String updateBooking(@PathVariable String bookingId,
                                @ModelAttribute BookingDTO bookingDTO,
                                BindingResult result,
                                HttpSession session,
                                RedirectAttributes ra) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Please enter a valid booking date.");
            return "redirect:/bookings/edit/" + bookingId;
        }

        BookingResponse response = bookingService.updateBooking(
                bookingId, bookingDTO, loggedInUser.getUserId(), loggedInUser.getRole());

        if (response.getStatus() != 200) {
            ra.addFlashAttribute("error", response.getMessage());
            return "redirect:/bookings/edit/" + bookingId;
        }

        return "redirect:/bookings";
    }

    @PostMapping("/delete/{bookingId}")
    public String deleteBooking(@PathVariable String bookingId,
                                HttpSession session,
                                RedirectAttributes ra) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/users/login";
        }

        BookingResponse response = bookingService.deleteBooking(
                bookingId, loggedInUser.getUserId(), loggedInUser.getRole());

        if (response.getStatus() != 200) {
            ra.addFlashAttribute("error", response.getMessage());
        }

        return "redirect:/bookings";
    }

    // ADMIN: mark a Confirmed booking as Completed (this is what allows the customer to review the bike)
    @PostMapping("/complete/{bookingId}")
    public String completeBooking(@PathVariable String bookingId,
                                  RedirectAttributes ra) {

        BookingResponse response = bookingService.completeBooking(bookingId);

        if (response.getStatus() == 200) {
            ra.addFlashAttribute("message", "Booking " + bookingId + " marked as Completed.");
        } else {
            ra.addFlashAttribute("error", response.getMessage());
        }

        return "redirect:/bookings";
    }
}
