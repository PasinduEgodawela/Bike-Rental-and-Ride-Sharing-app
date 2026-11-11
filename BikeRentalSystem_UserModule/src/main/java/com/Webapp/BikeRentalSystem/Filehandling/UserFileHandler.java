package com.Webapp.BikeRentalSystem.Filehandling;

import com.Webapp.BikeRentalSystem.Model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserFileHandler {

    private final FileHandler fileHandler;
    private final String path;

    public UserFileHandler(FileHandler fileHandler,
                           @Value("${user.file.path}") String filePath) {
        this.fileHandler = fileHandler;
        this.path = filePath;          // the path from application.properties
    }

    // A line that cannot be read is skipped (and printed), so one bad line
    // does not stop every login.
    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        for (String line : fileHandler.readLines(path)) {
            try {
                users.add(User.fromFileString(line));
            } catch (RuntimeException e) {
                System.out.println("Skipping unreadable user line: " + line);
            }
        }
        return users;
    }

    // Writes the users back. Lines that could not be read are kept at the end,
    // so saving never deletes a record just because it was damaged.
    public void saveUsers(List<User> users) {
        List<String> lines = new ArrayList<>();
        for (User u : users) {
            lines.add(u.toFileString());
        }

        for (String line : fileHandler.readLines(path)) {
            try {
                User.fromFileString(line);
            } catch (RuntimeException e) {
                lines.add(line);
            }
        }

        fileHandler.writeLines(path, lines);
    }

    public String getUserRoleByEmail(String email) {

        if (email == null || email.isEmpty()) {
            return null;
        }

        List<User> users = loadUsers();

        for (User user : users) {

            if (email.equalsIgnoreCase(user.getEmail())) {
                return user.getRole();
            }
        }

        return null;
    }

    public User findById(String id) {

        List<User> users = loadUsers();

        for (User user : users) {
            if (user.getUserId().equals(id)) {
                return user;
            }
        }

        return null;
    }

    public String nextId(List<User> users) {
        List<String> ids = new ArrayList<>();
        for (User u : users) {
            ids.add(u.getUserId());
        }
        return fileHandler.nextId("U", ids);
    }

    public List<User> getDrivers() {

        List<User> users = loadUsers();
        List<User> drivers = new ArrayList<>();

        for (User user : users) {

            if (user.getRole() != null &&
                    user.getRole().trim().equalsIgnoreCase("Driver")) {

                drivers.add(user);
            }
        }

        return drivers;
    }
}
