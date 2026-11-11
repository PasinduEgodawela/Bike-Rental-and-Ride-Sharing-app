package com.Webapp.BikeRentalSystem.Controller;

import com.Webapp.BikeRentalSystem.Model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("loggedInUser")
    public User loggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}