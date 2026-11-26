package org.example.dto;

import jakarta.validation.constraints.NotNull;

public record RoomStatusRequest(@NotNull Boolean active) {
}
