package com.Webapp.BikeRentalSystem.Config;

import com.Webapp.BikeRentalSystem.Model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * One place that checks "is the user logged in" and "does the role allow this page"
 * before any controller runs.
 *
 * - Public pages: home, login, register, logout and the bike list.
 * - Everything else needs a login.
 * - Some pages are also limited to one role (see rolesFor below).
 *
 * Checks that depend on WHO owns a record (for example "only the owner can edit this booking")
 * are done in the services, because only they can read the record.
 */
public class AccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String method = request.getMethod();

        if (isPublic(path, method)) {
            return true;
        }

        User user = loggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/users/login");
            return false;
        }

        String[] allowed = rolesFor(path, method);
        if (allowed == null) {
            return true;                    // any logged-in user
        }

        for (String role : allowed) {
            if (role.equals(user.getRole())) {
                return true;
            }
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to open this page.");
        return false;
    }

    private User loggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute("loggedInUser");
        return user instanceof User ? (User) user : null;
    }

    // Pages anyone can open, logged in or not
    private boolean isPublic(String path, String method) {
        if (path.equals("/") || path.equals("/error")) {
            return true;
        }
        if (path.equals("/users/login") || path.equals("/users/register") || path.equals("/users/logout")) {
            return true;
        }
        // visitors can browse the bike list (the buttons on it depend on the role)
        return path.equals("/bikes") && method.equals("GET");
    }

    // The roles allowed on a page, or null when any logged-in user may open it
    private String[] rolesFor(String path, String method) {

        // adding, editing and deleting bikes: an Owner lists bikes (BikeController checks that an
        // Owner only changes their own), the Admin manages all
        if (path.startsWith("/bikes")) {
            return new String[]{"Admin", "Owner"};
        }

        // ride requests: a customer joins a ride and sees their own requests;
        // approving, rejecting and deleting are for the Admin
        if (path.equals("/rideRequests") || path.equals("/rideRequests/create") || path.equals("/rideRequests/save")) {
            return new String[]{"Admin", "Customer"};
        }
        if (path.startsWith("/rideRequests")) {
            return new String[]{"Admin"};
        }

        // bookings: customers make them, only an Admin completes them
        if (path.equals("/bookings/create") || path.equals("/bookings/new") || path.equals("/bookings/save")) {
            return new String[]{"Customer"};
        }
        if (path.startsWith("/bookings/complete")) {
            return new String[]{"Admin"};
        }

        // rides: a Driver publishes and edits their own rides (RideController checks "their own"),
        // the Admin deletes
        if (path.startsWith("/rides/delete")) {
            return new String[]{"Admin"};
        }
        if (path.startsWith("/rides/create") || path.startsWith("/rides/save") || path.startsWith("/rides/edit")) {
            return new String[]{"Driver"};
        }

        // payments: customers pay, an Admin manages the records
        if (path.equals("/payments")) {
            return new String[]{"Admin", "Customer"};
        }
        if (path.startsWith("/payments/update") || path.startsWith("/payments/delete")) {
            return new String[]{"Admin"};
        }
        if (path.startsWith("/payments")) {
            return new String[]{"Customer"};
        }

        // GET /users?email=... tells you a user's role, so only an Admin may use it
        if (path.equals("/users")) {
            return new String[]{"Admin"};
        }

        return null;
    }
}
