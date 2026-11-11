
package com.Webapp.BikeRentalSystem.Controller;

import com.Webapp.BikeRentalSystem.DTO.ReviewDTO;
import com.Webapp.BikeRentalSystem.DTO.ReviewResponse;
import com.Webapp.BikeRentalSystem.Model.User;
import com.Webapp.BikeRentalSystem.Service.ReviewService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // CUSTOMER: GET /reviews
    @GetMapping
    public String customerReviews(
            @RequestParam(required = false) String bikeId,
            HttpSession session,
            Model model) {

        User user = currentUser(session);

        if (user == null) {
            return "redirect:/users/login";
        }

        if (!"Customer".equalsIgnoreCase(user.getRole())) {
            return redirectForOtherRole(user);
        }

        boolean hasBike = bikeId != null && !bikeId.isBlank();

        ReviewResponse response = hasBike
                ? reviewService.getReviewsByBike(bikeId)
                : reviewService.getAllReviews();

        ReviewDTO[] reviews = response.getReviews() == null
                ? new ReviewDTO[0]
                : response.getReviews();

        ReviewDTO form = new ReviewDTO();
        form.setBikeId(hasBike ? bikeId : null);

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", reviews.length);
        model.addAttribute("averageRating", response.getAverageRating());
        model.addAttribute("bikeId", bikeId);
        model.addAttribute("review", form);
        model.addAttribute("role", "Customer");
        model.addAttribute("currentUserId", user.getUserId());

        return "reviews/customer";
    }

    // ADMIN: GET /reviews/admin
    @GetMapping("/admin")
    public String adminReviews(
            HttpSession session,
            Model model) {

        User user = currentUser(session);

        if (user == null) {
            return "redirect:/users/login";
        }

        if (!"Admin".equalsIgnoreCase(user.getRole())) {
            return redirectForOtherRole(user);
        }

        ReviewResponse response = reviewService.getAllReviews();

        ReviewDTO[] reviews = response.getReviews() == null
                ? new ReviewDTO[0]
                : response.getReviews();

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", reviews.length);
        model.addAttribute("averageRating", response.getAverageRating());

        return "reviews/admin";
    }

    // CUSTOMER: GET /reviews/my
    @GetMapping("/my")
    public String myReviews(
            HttpSession session,
            Model model) {

        User user = currentUser(session);

        if (user == null) {
            return "redirect:/users/login";
        }

        if (!"Customer".equalsIgnoreCase(user.getRole())) {
            return redirectForOtherRole(user);
        }

        ReviewResponse response =
                reviewService.getReviewsByUser(user.getUserId());

        ReviewDTO[] reviews = response.getReviews() == null
                ? new ReviewDTO[0]
                : response.getReviews();

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", reviews.length);
        model.addAttribute("averageRating", response.getAverageRating());
        model.addAttribute("review", new ReviewDTO());
        model.addAttribute("bikeId", null);
        model.addAttribute("role", "Customer");
        model.addAttribute("currentUserId", user.getUserId());

        return "reviews/customer";
    }

    // CUSTOMER: POST /reviews/save
    @PostMapping("/save")
    public String saveReview(
            @Valid @ModelAttribute ReviewDTO reviewDTO,
            BindingResult result,
            HttpSession session,
            RedirectAttributes ra) {

        User user = currentUser(session);

        if (user == null) {
            return "redirect:/users/login";
        }

        if (!"Customer".equalsIgnoreCase(user.getRole())) {
            ra.addFlashAttribute("error",
                    "Only customers can write reviews.");
            return redirectForOtherRole(user);
        }

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return redirectToBike(reviewDTO.getBikeId());
        }

        ReviewResponse response =
                reviewService.saveReview(reviewDTO, user.getUserId());

        flash(ra, response);

        return redirectToBike(reviewDTO.getBikeId());
    }

    // CUSTOMER OWNER ONLY: GET /reviews/edit/{reviewId}
    @GetMapping("/edit/{reviewId}")
    public String editForm(
            @PathVariable String reviewId,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User user = currentUser(session);

        if (user == null) {
            return "redirect:/users/login";
        }

        if (!"Customer".equalsIgnoreCase(user.getRole())) {
            return redirectForOtherRole(user);
        }

        ReviewResponse response =
                reviewService.getReviewById(reviewId);

        ReviewDTO review = response.getReview();

        if (review == null) {
            ra.addFlashAttribute("error", "Review not found.");
            return "redirect:/reviews";
        }

        if (!user.getUserId().equals(review.getUserId())) {
            ra.addFlashAttribute("error",
                    "You can only edit your own reviews.");
            return "redirect:/reviews";
        }

        model.addAttribute("review", review);

        return "reviews/edit";
    }

    // CUSTOMER OWNER ONLY: POST /reviews/update/{reviewId}
    @PostMapping("/update/{reviewId}")
    public String updateReview(
            @PathVariable String reviewId,
            @Valid @ModelAttribute ReviewDTO reviewDTO,
            BindingResult result,
            HttpSession session,
            RedirectAttributes ra) {

        User user = currentUser(session);

        if (user == null) {
            return "redirect:/users/login";
        }

        if (!"Customer".equalsIgnoreCase(user.getRole())) {
            ra.addFlashAttribute("error",
                    "Only customers can edit reviews.");
            return redirectForOtherRole(user);
        }

        ReviewResponse existingResponse =
                reviewService.getReviewById(reviewId);

        ReviewDTO existing = existingResponse.getReview();

        if (existing == null ||
                !user.getUserId().equals(existing.getUserId())) {

            ra.addFlashAttribute("error",
                    "You can only edit your own reviews.");
            return "redirect:/reviews";
        }

        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return redirectToBike(existing.getBikeId());
        }

        ReviewResponse response = reviewService.updateReview(
                reviewId,
                reviewDTO,
                user.getUserId(),
                user.getRole()
        );

        flash(ra, response);

        return redirectToBike(existing.getBikeId());
    }

    // CUSTOMER OWNER or ADMIN: POST /reviews/delete/{reviewId}
    @PostMapping("/delete/{reviewId}")
    public String deleteReview(
            @PathVariable String reviewId,
            HttpSession session,
            RedirectAttributes ra) {

        User user = currentUser(session);

        if (user == null) {
            return "redirect:/users/login";
        }

        boolean isAdmin = "Admin".equalsIgnoreCase(user.getRole());
        boolean isCustomer = "Customer".equalsIgnoreCase(user.getRole());

        if (!isAdmin && !isCustomer) {
            ra.addFlashAttribute("error",
                    "Only customers and admins can delete reviews.");
            return redirectForOtherRole(user);
        }

        ReviewResponse existingResponse =
                reviewService.getReviewById(reviewId);

        ReviewDTO existing = existingResponse.getReview();

        if (existing == null) {
            ra.addFlashAttribute("error", "Review not found.");
            return isAdmin ? "redirect:/reviews/admin" : "redirect:/reviews";
        }

        // a customer can only delete their own review; an Admin can delete any
        if (isCustomer && !user.getUserId().equals(existing.getUserId())) {
            ra.addFlashAttribute("error",
                    "You can only delete your own reviews.");
            return "redirect:/reviews";
        }

        ReviewResponse response = reviewService.deleteReview(
                reviewId,
                user.getUserId(),
                user.getRole()
        );

        flash(ra, response);

        return isAdmin ? "redirect:/reviews/admin" : redirectToBike(existing.getBikeId());
    }

    // Get logged-in user from session
    private User currentUser(HttpSession session) {
        Object user = session.getAttribute("loggedInUser");

        if (user instanceof User) {
            return (User) user;
        }

        return null;
    }

    // Admin goes to the admin review page. Any other role (a Driver) goes to their profile,
    // so the two review pages can never send them back and forth forever.
    private String redirectForOtherRole(User user) {
        if ("Admin".equalsIgnoreCase(user.getRole())) {
            return "redirect:/reviews/admin";
        }
        return "redirect:/users/profile";
    }

    private void flash(
            RedirectAttributes ra,
            ReviewResponse response) {

        if (response != null) {
            ra.addFlashAttribute(
                    response.getStatus() == 200 ? "message" : "error",
                    response.getMessage()
            );
        }
    }

    private String redirectToBike(String bikeId) {
        if (bikeId != null && !bikeId.isBlank()) {
            return "redirect:/reviews?bikeId=" + bikeId;
        }

        return "redirect:/reviews";
    }
}
