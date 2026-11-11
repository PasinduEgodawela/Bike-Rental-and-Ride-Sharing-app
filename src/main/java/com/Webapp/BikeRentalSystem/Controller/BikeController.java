package com.Webapp.BikeRentalSystem.Controller;


import com.Webapp.BikeRentalSystem.Model.Bike;
import com.Webapp.BikeRentalSystem.Model.User;
import com.Webapp.BikeRentalSystem.Service.BikeService;
import com.Webapp.BikeRentalSystem.Service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

// Everyone can look at the bike list. Add, edit and delete are for an Admin or an Owner
// (AccessInterceptor). An Owner can only change the bikes they listed.
@Controller
@RequestMapping("/bikes")
public class BikeController {

    private final BikeService service;
    private final BookingService bookingService;

    public BikeController(BikeService service, BookingService bookingService) {
        this.service = service;
        this.bookingService = bookingService;
    }


    @GetMapping
    public String showBikes(@RequestParam(value = "sort", required = false) String sort,
                            @RequestParam(value = "message", required = false) String message,
                            Model model) throws IOException {

        // the sorting is done in BikeService
        List<Bike> bikes = service.getAllBikes(sort);

        model.addAttribute("bikes", bikes);
        model.addAttribute("message", message);
        return "bikes/bikes";
    }


    @GetMapping("/add")
    public String showAddForm(Model model) throws IOException {
        model.addAttribute("nextId", service.getNextBikeId());
        return "bikes/addBike";
    }


    // There is no bikeId parameter: the service makes the ID when it saves the bike
    @PostMapping("/add")
    public String addBike(@RequestParam("model") String bikeModel,
                          @RequestParam("type") String type,
                          @RequestParam("location") String location,
                          @RequestParam("pricePerDay") double pricePerDay,
                          @RequestParam("available") boolean available,
                          @RequestParam("status") String status,
                          HttpSession session,
                          Model model,
                          RedirectAttributes redirectAttributes) throws IOException {

        // the bike belongs to whoever lists it
        User user = (User) session.getAttribute("loggedInUser");
        Bike bike = new Bike("", bikeModel, type, location, pricePerDay, available, status, user.getUserId());
        String result = service.addBike(bike);

        if (result.equals("OK")) {
            redirectAttributes.addAttribute("message", "Bike added successfully");
            return "redirect:/bikes";
        }


        model.addAttribute("error", result);
        model.addAttribute("nextId", service.getNextBikeId());
        return "bikes/addBike";
    }


    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) throws IOException {
        Bike bike = service.getBikeById(id);

        if (bike == null) {
            return "redirect:/bikes";
        }
        if (!service.canManage((User) session.getAttribute("loggedInUser"), id)) {
            redirectAttributes.addAttribute("message", "You can only change your own bikes.");
            return "redirect:/bikes";
        }

        model.addAttribute("bike", bike);
        return "bikes/editBike";
    }


    @PostMapping("/edit")
    public String updateBike(@RequestParam("bikeId") String bikeId,
                             @RequestParam("model") String bikeModel,
                             @RequestParam("type") String type,
                             @RequestParam("location") String location,
                             @RequestParam("pricePerDay") double pricePerDay,
                             @RequestParam("available") boolean available,
                             @RequestParam("status") String status,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) throws IOException {

        if (!service.canManage((User) session.getAttribute("loggedInUser"), bikeId)) {
            redirectAttributes.addAttribute("message", "You can only change your own bikes.");
            return "redirect:/bikes";
        }

        Bike bike = new Bike(bikeId, bikeModel, type, location, pricePerDay, available, status);
        String result = service.updateBike(bike);

        if (result.equals("OK")) {
            redirectAttributes.addAttribute("message", "Bike updated successfully");
            return "redirect:/bikes";
        }

        model.addAttribute("error", result);
        model.addAttribute("bike", bike);
        return "bikes/editBike";
    }


    @PostMapping("/delete")
    public String deleteBike(@RequestParam("bikeId") String bikeId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) throws IOException {

        if (!service.canManage((User) session.getAttribute("loggedInUser"), bikeId)) {
            redirectAttributes.addAttribute("message", "You can only delete your own bikes.");
            return "redirect:/bikes";
        }

        // a bike with a Pending or Confirmed booking cannot be deleted
        if (bookingService.hasActiveBookingForBike(bikeId)) {
            redirectAttributes.addAttribute("message",
                    "This bike has active bookings, so it cannot be deleted.");
            return "redirect:/bikes";
        }

        String result = service.deleteBike(bikeId);

        if (result.equals("OK")) {
            redirectAttributes.addAttribute("message", "Bike deleted successfully");
        } else {
            redirectAttributes.addAttribute("message", result);
        }
        return "redirect:/bikes";
    }
}
