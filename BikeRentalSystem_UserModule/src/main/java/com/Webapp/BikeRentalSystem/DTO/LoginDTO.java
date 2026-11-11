package com.Webapp.BikeRentalSystem.DTO;

import jakarta.validation.constraints.NotBlank;

public class LoginDTO {

    @NotBlank(message = "Please enter your email.")
    private String email;

    @NotBlank(message = "Please enter your password.")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
