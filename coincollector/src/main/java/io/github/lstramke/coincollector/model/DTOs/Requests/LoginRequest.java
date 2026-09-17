package io.github.lstramke.coincollector.model.DTOs.Requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be 8 characters minimum")
    String password
) {}
