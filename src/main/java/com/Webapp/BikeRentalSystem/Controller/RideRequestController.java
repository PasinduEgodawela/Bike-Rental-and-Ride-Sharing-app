package com.Webapp.BikeRentalSystem.Controller;

import com.Webapp.BikeRentalSystem.DTO.BookingDTO;
import com.Webapp.BikeRentalSystem.DTO.RideRequestDTO;
import com.Webapp.BikeRentalSystem.DTO.RideRequestResponse;
import com.Webapp.BikeRentalSystem.Model.User;
import com.Webapp.BikeRentalSystem.Service.BookingService;
import com.Webapp.BikeRentalSystem.Service.RideRequestService;
import com.Webapp.BikeRentalSystem.Service.RideService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

// A Customer joins a ride by sending a request for one of their own bookings and can see their own
// requests. Approving, rejecting and deleting are for the Admin (AccessInterceptor).
// Requests are handled first in, first out.
@Controller
@RequestMapping("/rideRequests")
public class RideRequestController {

    private final RideRequestService rideRequestService;
    private final BookingService bookingService;
    private final RideService rideService;

    public RideRequestController(
            RideRequestService rideRequestService,
            BookingService bookingService,
            RideService rideService) {

        this.rideRequestService = rideRequestService;
        this.bookingService = bookingService;
        this.rideService = rideService;
    }

    @GetMapping
    public String getAllRideRequests(HttpSession session, Model model) {

        RideRequestResponse response =
                rideRequestService.getAllRideRequests();

        RideRequestDTO[] requests = response.getRideRequests();

        // A customer only sees their own requests
        User user = currentUser(session);
        if (user != null && !"Admin".equals(user.getRole()) && requests != null) {
            List<RideRequestDTO> own = new ArrayList<>();
            for (RideRequestDTO r : requests) {
                if (user.getUserId().equals(r.getPassengerId())) {
                    own.add(r);
                }
            }
            requests = own.toArray(new RideRequestDTO[0]);
        }

        model.addAttribute("rideRequests", requests);

        // only this request can be approved or rejected right now
        model.addAttribute("frontRequestId",
                rideRequestService.getFrontRequestId());

        return "rideRequests/list";
    }

    // The passenger and bike are read from the real booking, not from the link.
    // A customer can only use their own booking. rideId (optional) preselects the ride to join.
    @GetMapping("/create")
    public String createForm(@RequestParam String bookingId,
                             @RequestParam(required = false) String rideId,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {

        BookingDTO booking = bookingService.getBookingById(bookingId).getBooking();

        if (booking == null) {
            ra.addFlashAttribute("error", "Booking " + bookingId + " was not found.");
            return "redirect:/rides";
        }
        if (!mayUseBooking(session, booking)) {
            ra.addFlashAttribute("error", "You can only join a ride with your own booking.");
            return "redirect:/rides";
        }

        RideRequestDTO dto = new RideRequestDTO();

        dto.setBookingId(booking.getBookingId());
        dto.setPassengerId(booking.getUserId());
        dto.setBikeId(booking.getBikeId());
        if (rideId != null) {
            dto.setRideId(rideId);
        }

        model.addAttribute("rideRequest", dto);
        model.addAttribute("rides", rideService.getOpenRidesForBike(booking.getBikeId()));

        return "rideRequests/create";
    }

    @PostMapping("/save")
    public String saveRideRequest(@ModelAttribute("rideRequest") RideRequestDTO rideRequestDTO,
                                  HttpSession session,
                                  RedirectAttributes ra) {

        BookingDTO booking = bookingService.getBookingById(rideRequestDTO.getBookingId()).getBooking();

        if (booking != null && !mayUseBooking(session, booking)) {
            ra.addFlashAttribute("error", "You can only join a ride with your own booking.");
            return "redirect:/rides";
        }

        RideRequestResponse response = rideRequestService.saveRideRequest(rideRequestDTO);

        flash(ra, response);

        return "redirect:/rideRequests";
    }

    @PostMapping("/approve/{requestId}")
    public String approveRideRequest(@PathVariable String requestId, RedirectAttributes ra) {

        flash(ra, rideRequestService.approveRideRequest(requestId));

        return "redirect:/rideRequests";
    }

    @PostMapping("/reject/{requestId}")
    public String rejectRideRequest(@PathVariable String requestId, RedirectAttributes ra) {

        flash(ra, rideRequestService.rejectRideRequest(requestId));

        return "redirect:/rideRequests";
    }

    @PostMapping("/delete/{requestId}")
    public String deleteRideRequest(
            @PathVariable String requestId, RedirectAttributes ra) {

        flash(ra, rideRequestService.deleteRideRequest(requestId));

        return "redirect:/rideRequests";
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("loggedInUser");
        return user instanceof User ? (User) user : null;
    }

    // The Admin may act for anyone. A customer only for a booking that is theirs.
    private boolean mayUseBooking(HttpSession session, BookingDTO booking) {
        User user = currentUser(session);
        if (user == null) {
            return false;
        }
        return "Admin".equals(user.getRole()) || user.getUserId().equals(booking.getUserId());
    }

    // The message goes to the next page: green for success (200), red for a problem
    private void flash(RedirectAttributes ra, RideRequestResponse response) {
        if (response.getStatus() == 200) {
            ra.addFlashAttribute("message", response.getMessage());
        } else {
            ra.addFlashAttribute("error", response.getMessage());
        }
    }
}
