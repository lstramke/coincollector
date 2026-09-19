package io.github.lstramke.coincollector.model.DTOs.Requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be 8 characters minimum")
    String oldPassword,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be 8 characters minimum")
    String newPassword
) {}
