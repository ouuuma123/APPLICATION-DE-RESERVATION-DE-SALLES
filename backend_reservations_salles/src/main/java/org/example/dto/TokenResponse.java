package org.example.dto;

import java.util.List;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserDTO user, List<String> roles) {
}
