package com.Webapp.BikeRentalSystem.Config;

import com.Webapp.BikeRentalSystem.Model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;


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
            return true;                    
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

    
    private boolean isPublic(String path, String method) {
        if (path.equals("/") || path.equals("/error")) {
            return true;
        }
        if (path.equals("/users/login") || path.equals("/users/register") || path.equals("/users/logout")) {
            return true;
        }
        
        return path.equals("/bikes") && method.equals("GET");
    }

    
    private String[] rolesFor(String path, String method) {

        
        if (path.startsWith("/bikes")) {
            return new String[]{"Admin", "Owner"};
        }

        
        if (path.equals("/rideRequests") || path.equals("/rideRequests/create") || path.equals("/rideRequests/save")) {
            return new String[]{"Admin", "Customer"};
        }
        if (path.startsWith("/rideRequests")) {
            return new String[]{"Admin"};
        }

        
        if (path.equals("/bookings/create") || path.equals("/bookings/new") || path.equals("/bookings/save")) {
            return new String[]{"Customer"};
        }
        if (path.startsWith("/bookings/complete")) {
            return new String[]{"Admin"};
        }

       
        if (path.startsWith("/rides/delete")) {
            return new String[]{"Admin"};
        }
        if (path.startsWith("/rides/create") || path.startsWith("/rides/save") || path.startsWith("/rides/edit")) {
            return new String[]{"Driver"};
        }

        
        if (path.equals("/payments")) {
            return new String[]{"Admin", "Customer"};
        }
        if (path.startsWith("/payments/update") || path.startsWith("/payments/delete")) {
            return new String[]{"Admin"};
        }
        if (path.startsWith("/payments")) {
            return new String[]{"Customer"};
        }

        
        if (path.equals("/users")) {
            return new String[]{"Admin"};
        }

        return null;
    }
}
