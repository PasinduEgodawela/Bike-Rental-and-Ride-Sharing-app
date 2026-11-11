package com.Webapp.BikeRentalSystem.Controller;

import com.Webapp.BikeRentalSystem.DTO.RideDTO;
import com.Webapp.BikeRentalSystem.Model.Bike;
import com.Webapp.BikeRentalSystem.Model.Ride;
import com.Webapp.BikeRentalSystem.Model.User;
import com.Webapp.BikeRentalSystem.Service.BikeService;
import com.Webapp.BikeRentalSystem.Service.RideService;
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
@RequestMapping("/rides")
public class RideController {

    private final RideService rideService;
    private final BikeService bikeService;

    public RideController(RideService rideService, BikeService bikeService) {
        this.rideService = rideService;
        this.bikeService = bikeService;
    }

  

    @GetMapping
    public String listRides(HttpSession session, Model model) {

        User user = currentUser(session);
        if (user == null) {
            return "redirect:/users/login";
        }

        String role = user.getRole();

        if ("Admin".equals(role)) {
            model.addAttribute("rides", rideService.readRides());
        } else if ("Driver".equals(role)) {
            model.addAttribute("rides", rideService.getRidesForDriver(user.getUserId()));
        } else {
            List<Ride> rides = rideService.getRidesForCustomer(user.getUserId());
            model.addAttribute("rides", rides);
            model.addAttribute("bookingIds", rideService.getBookingIdsForRides(user.getUserId(), rides));
        }

        return "admin/rides";
    }

  

    @GetMapping("/create")
    public String createRide(HttpSession session,
                             Model model,
                             @RequestParam(required = false) String bikeId) {

        if (!isDriver(session)) {
            return "redirect:/rides";
        }

        RideDTO rideDTO = new RideDTO();
        if (bikeId != null) {
            rideDTO.setBikeId(bikeId);
        }

        model.addAttribute("rideDTO", rideDTO);
        model.addAttribute("bikes", activeBikes());

        return "rides/create";
    }

    @PostMapping("/save")
    public String addRide(HttpSession session,
                          @Valid @ModelAttribute("rideDTO") RideDTO rideDTO,
                          BindingResult result,
                          Model model) {

        if (!isDriver(session)) {
            return "redirect:/rides";
        }

       
        rideDTO.setDriverId(currentUser(session).getUserId());

        if (result.hasErrors()) {
            model.addAttribute("bikes", activeBikes());
            return "rides/create";
        }

        try {
            rideService.createRide(rideDTO);
        } catch (IllegalArgumentException e) {
            result.reject("ride.invalid", e.getMessage());
            model.addAttribute("bikes", activeBikes());
            return "rides/create";
        }

        return "redirect:/rides";
    }

  

    @GetMapping("/edit/{id}")
    public String editRideForm(@PathVariable String id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes ra) {

        if (!isDriver(session)) {
            return "redirect:/rides";
        }

        if (!rideService.driverOwns(currentUser(session).getUserId(), id)) {
            ra.addFlashAttribute("error", "You can only edit your own rides.");
            return "redirect:/rides";
        }

        Ride ride = rideService.getRideById(id);

        RideDTO dto = new RideDTO();
        dto.setBikeId(ride.getBikeId());
        dto.setDriverId(ride.getDriverId());
        dto.setStartLocation(ride.getStartLocation());
        dto.setDestination(ride.getDestination());
        dto.setRideDate(ride.getRideDate());
        dto.setRideTime(ride.getRideTime());
        dto.setAvailableSeats(ride.getAvailableSeats());
        dto.setStatus(ride.getStatus());

        model.addAttribute("rideDTO", dto);
        model.addAttribute("rideId", id);

        return "rides/editRide";
    }

    @PostMapping("/edit/{id}")
    public String updateRide(@PathVariable String id,
                             HttpSession session,
                             @Valid @ModelAttribute("rideDTO") RideDTO rideDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes ra) {

        if (!isDriver(session)) {
            return "redirect:/rides";
        }

        if (!rideService.driverOwns(currentUser(session).getUserId(), id)) {
            ra.addFlashAttribute("error", "You can only edit your own rides.");
            return "redirect:/rides";
        }

        if (result.hasErrors()) {
            model.addAttribute("rideId", id);
            return "rides/editRide";
        }

        try {
            rideService.updateRide(id, rideDTO);
        } catch (IllegalArgumentException e) {
            result.reject("ride.invalid", e.getMessage());
            model.addAttribute("rideId", id);
            return "rides/editRide";
        }

        return "redirect:/rides";
    }

    // ---------- DELETE (Admin only) ----------

    @PostMapping("/delete/{id}")
    public String deleteRide(@PathVariable String id, HttpSession session) {

        User user = currentUser(session);
        if (user == null || !"Admin".equals(user.getRole())) {
            return "redirect:/rides";
        }

        rideService.deleteRide(id);
        return "redirect:/rides";
    }

   
    private List<Bike> activeBikes() {
        List<Bike> result = new ArrayList<>();
        try {
            for (Bike bike : bikeService.getAllBikes()) {
                if ("Active".equals(bike.getStatus())) {
                    result.add(bike);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read the bike list: " + e.getMessage());
        }
        return result;
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("loggedInUser");
        return user instanceof User ? (User) user : null;
    }

    private boolean isDriver(HttpSession session) {
        User user = currentUser(session);
        return user != null && "Driver".equals(user.getRole());
    }
}
