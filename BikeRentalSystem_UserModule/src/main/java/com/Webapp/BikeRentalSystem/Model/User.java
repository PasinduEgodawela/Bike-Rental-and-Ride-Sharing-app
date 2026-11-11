package com.Webapp.BikeRentalSystem.Model;

import com.Webapp.BikeRentalSystem.Filehandling.TextClean;

public class User {
    private String userId;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String role;

    public User() {}

    public User(String userId, String name, String email, String password,
                String phone, String address, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    
    public User withoutPassword() {
        return new User(userId, name, email, "", phone, address, role);
    }

    public String toFileString() {
        return TextClean.clean(userId) + "|" + TextClean.clean(name) + "|"
                + TextClean.clean(email) + "|" + TextClean.clean(password) + "|"
                + TextClean.clean(phone) + "|" + TextClean.clean(address) + "|"
                + TextClean.clean(role);
    }

    
    public static User fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length != 7) {
            throw new IllegalArgumentException("User record must contain seven fields");
        }
        return new User(p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
    }
}
