package com.Webapp.BikeRentalSystem.Service;

import com.Webapp.BikeRentalSystem.DTO.LoginDTO;
import com.Webapp.BikeRentalSystem.DTO.RegisterDTO;
import com.Webapp.BikeRentalSystem.DTO.UserUpdateDTO;
import com.Webapp.BikeRentalSystem.Filehandling.TextClean;
import com.Webapp.BikeRentalSystem.Filehandling.UserFileHandler;
import com.Webapp.BikeRentalSystem.Model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserFileHandler fileHandler;

    public UserService(UserFileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    
    public synchronized User registerUser(RegisterDTO dto) {
        String role = chooseRole(dto.getRole());

        String email = dto.getEmail().trim();
       
        rejectUnsafe("Email", email);
        rejectUnsafe("Password", dto.getPassword());

        List<User> users = fileHandler.loadUsers();

        for (User existing : users) {
            if (existing.getEmail().equalsIgnoreCase(email)) {
                throw new IllegalArgumentException("Email already registered");
            }
        }

        String newId = fileHandler.nextId(users);
        User u = new User(newId,
                TextClean.clean(dto.getName()),
                email,
                dto.getPassword(),
                TextClean.clean(dto.getPhone()),
                TextClean.clean(dto.getAddress()),
                role);
        users.add(u);
        fileHandler.saveUsers(users);
        return u;
    }

    
    private String chooseRole(String role) {
        if (role == null || role.isBlank() || role.equalsIgnoreCase("Customer")) {
            return "Customer";
        }
        if (role.equalsIgnoreCase("Driver")) {
            return "Driver";
        }
        if (role.equalsIgnoreCase("Owner")) {
            return "Owner";
        }
        throw new IllegalArgumentException("Please choose Customer, Driver or Owner as the role");
    }

   
    private void rejectUnsafe(String label, String value) {
        if (value == null || !value.equals(TextClean.clean(value))) {
            throw new IllegalArgumentException(label
                    + " cannot contain the | character, line breaks, or spaces at the start or end");
        }
    }

 
    public User loginUser(LoginDTO dto) {
        if (dto.getEmail() == null || dto.getPassword() == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        List<User> users = fileHandler.loadUsers();

        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(dto.getEmail().trim())
                    && u.getPassword().equals(dto.getPassword())) {
                return u;
            }
        }
        throw new IllegalArgumentException("Invalid email or password");
    }


    public synchronized User updateUser(String id, UserUpdateDTO dto, User loggedIn) {
        if (!loggedIn.getUserId().equals(id) && !"Admin".equals(loggedIn.getRole())) {
            throw new SecurityException("You can only update your own account");
        }
        List<User> users = fileHandler.loadUsers();

        User target = null;
        for (User u : users) {
            if (u.getUserId().equals(id)) {
                target = u;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (dto.getName() != null && !dto.getName().isBlank())
            target.setName(TextClean.clean(dto.getName()));
        if (dto.getPhone() != null && !dto.getPhone().isBlank())
            target.setPhone(TextClean.clean(dto.getPhone()));
        if (dto.getAddress() != null && !dto.getAddress().isBlank())
            target.setAddress(TextClean.clean(dto.getAddress()));
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (dto.getNewPassword().length() < 6)
                throw new IllegalArgumentException("Password must be at least 6 characters");
            rejectUnsafe("Password", dto.getNewPassword());
            target.setPassword(dto.getNewPassword());
        }
        fileHandler.saveUsers(users);
        return target;
    }

    public synchronized void deleteUser(String id, User loggedIn) {
        if (!loggedIn.getUserId().equals(id) && !"Admin".equals(loggedIn.getRole())) {
            throw new SecurityException("You can only delete your own account");
        }
        List<User> users = fileHandler.loadUsers();

        User target = null;
        int adminCount = 0;
        for (User u : users) {
            if (u.getUserId().equals(id)) {
                target = u;
            }
            if ("Admin".equals(u.getRole())) {
                adminCount++;
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("User not found");
        }
        if ("Admin".equals(target.getRole()) && adminCount <= 1) {
            throw new IllegalArgumentException("The last Admin account cannot be deleted");
        }

        users.remove(target);
        fileHandler.saveUsers(users);
    }

    public void logOutUser(HttpSession session) {
        session.invalidate();
    }

    public User findById(String id) {
        return fileHandler.findById(id);
    }

    public String getUserRoleByEmail(String email) {
        return fileHandler.getUserRoleByEmail(email);
    }

    public User getUserById(String id) {
        return fileHandler.findById(id);
    }

    public List<User> getAllDrivers() {

        return fileHandler.getDrivers();
    }
}
