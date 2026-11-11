package com.Webapp.BikeRentalSystem.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterDTO {

    @NotBlank(message = "Please enter your full name.")
    @Size(max = 60, message = "Name must be 60 characters or fewer.")
    private String name;

    @NotBlank(message = "Please enter your email.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Please enter a password.")
    @Size(min = 6, max = 50, message = "Password must be 6 to 50 characters.")
    private String password;

    @NotBlank(message = "Please enter your phone number.")
    @Pattern(regexp = "^[0-9+]{7,15}$", message = "Phone number must be 7 to 15 digits.")
    private String phone;

    @Size(max = 100, message = "Address must be 100 characters or fewer.")
    private String address;

    // Customer or Driver. Checked again in UserService.
    private String role;

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
}
