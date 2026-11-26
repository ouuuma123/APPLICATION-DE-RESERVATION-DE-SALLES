package org.example.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public record UserRoleRequest(
        @NotBlank
        @Pattern(regexp = "ADMIN|EMPLOYEE", message = "Role must be ADMIN or EMPLOYEE")
        String role) {
}
