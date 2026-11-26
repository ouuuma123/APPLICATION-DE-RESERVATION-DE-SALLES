package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomAvailabilityDTO {
    private Long roomId;
    private String roomName;
    private Integer capacity;
    private List<TimeSlotDTO> occupiedSlots;
    private List<TimeSlotDTO> availableSlots;
}
