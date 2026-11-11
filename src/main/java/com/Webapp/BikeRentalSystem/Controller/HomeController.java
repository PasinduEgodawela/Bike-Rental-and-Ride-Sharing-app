package com.Webapp.BikeRentalSystem.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    // Home page: templates/home.html
    @GetMapping("/")
    public String home() {
        return "home";
    }
}