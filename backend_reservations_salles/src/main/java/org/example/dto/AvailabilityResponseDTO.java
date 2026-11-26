package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponseDTO {
    private Long roomId;
    private LocalDate date;
    private List<TimeSlotDTO> occupiedSlots;
    private List<TimeSlotDTO> availableSlots;
}
