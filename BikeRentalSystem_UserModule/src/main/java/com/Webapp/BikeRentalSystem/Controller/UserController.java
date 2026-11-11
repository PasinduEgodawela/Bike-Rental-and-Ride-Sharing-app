package com.Webapp.BikeRentalSystem.Controller;

import com.Webapp.BikeRentalSystem.DTO.LoginDTO;
import com.Webapp.BikeRentalSystem.DTO.RegisterDTO;
import com.Webapp.BikeRentalSystem.DTO.UserUpdateDTO;
import com.Webapp.BikeRentalSystem.Service.UserService;
import com.Webapp.BikeRentalSystem.Model.User;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ---------- REGISTER ----------

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
                           BindingResult result,
                           Model model) {

        if (result.hasErrors()) {
            return "user/register";
        }

        try {
            userService.registerUser(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "user/register";
        }

        return "redirect:/users/login";
    }

    // ---------- LOGIN ----------

    @GetMapping("/login")
    public String showLogin(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "user/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginDTO") LoginDTO dto,
                        BindingResult result,
                        Model model,
                        HttpSession session) {

        if (result.hasErrors()) {
            return "user/login";
        }

        try {
            User user = userService.loginUser(dto);

            
            session.setAttribute("loggedInUser", user.withoutPassword());

            return "redirect:/users/profile";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "user/login";
        }
    }

    // ---------- LOGOUT ----------

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        userService.logOutUser(session);

        return "redirect:/users/login";
    }

    // ---------- PROFILE ----------

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/users/login";
        }

        model.addAttribute("user", user);

        UserUpdateDTO dto = new UserUpdateDTO();

        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());

        model.addAttribute("userUpdateDTO", dto);

        return "user/profile";
    }

    // ---------- UPDATE PROFILE ----------

    @PostMapping("/profile")
    public String updateProfile(
            @ModelAttribute("userUpdateDTO") UserUpdateDTO dto,
            BindingResult result,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/users/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "user/profile";
        }

        try {

            User updated = userService.updateUser(
                    user.getUserId(),
                    dto,
                    user
            );

            session.setAttribute("loggedInUser", updated.withoutPassword());

        } catch (Exception e) {

            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);

            return "user/profile";
        }

        return "redirect:/users/profile";
    }

    // ---------- GET USER ROLE BY EMAIL (Admin only, checked in AccessInterceptor) ----------

    @GetMapping
    @ResponseBody
    public String getUserRoleByEmail(@RequestParam String email) {

        String role = userService.getUserRoleByEmail(email);

        if (role == null) {
            return "User not found";
        }

        return role;
    }

    // ---------- DELETE USER ----------

    @PostMapping("/delete")
    public String deleteUser(
            @RequestParam(required = false) String id,
            HttpSession session,
            RedirectAttributes ra) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/users/login";
        }


        String targetId;

        if (id != null && "Admin".equalsIgnoreCase(user.getRole())) {
            targetId = id;
        } else {
            targetId = user.getUserId();
        }

        try {
            userService.deleteUser(targetId, user);     
        } catch (IllegalArgumentException | SecurityException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/users/profile";
        }

        if (targetId.equals(user.getUserId())) {
            session.invalidate();
            return "redirect:/users/register";
        }

        return "redirect:/users/profile";
    }
}
