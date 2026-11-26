package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.AvailabilityResponseDTO;
import org.example.dto.RoomDTO;
import org.example.dto.RoomStatusRequest;
import org.example.mapper.BookingMapper;
import org.example.model.Room;
import org.example.service.BookingServiceInterface;
import org.example.service.RoomServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomRestController {

    private final RoomServiceInterface roomService;
    private final BookingServiceInterface bookingService;

    public RoomRestController(RoomServiceInterface roomService, BookingServiceInterface bookingService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody Room room) {
        Room createdRoom = roomService.createRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingMapper.toDTO(createdRoom));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
    public ResponseEntity<List<RoomDTO>> getAllRooms(
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) Boolean active,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        List<RoomDTO> rooms = roomService.getAllRooms().stream()
                .filter(room -> isAdmin
                        ? active == null || room.isActive() == active
                        : room.isActive())
                .filter(room -> minCapacity == null || room.getCapacity() >= minCapacity)
                .filter(room -> equipment == null || equipment.isBlank()
                        || (room.getEquipment() != null
                        && room.getEquipment().toLowerCase().contains(equipment.toLowerCase()))
                        || (room.getAmenities() != null && room.getAmenities().stream()
                        .anyMatch(amenity -> amenity.equalsIgnoreCase(equipment))))
                .map(BookingMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long id, Authentication authentication) {
        return roomService.getRoomById(id)
                .filter(room -> isAdmin(authentication) || room.isActive())
                .map(room -> ResponseEntity.ok(BookingMapper.toDTO(room)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
    public ResponseEntity<List<RoomDTO>> getAvailableRooms(
            @RequestParam java.time.LocalDateTime startTime,
            @RequestParam java.time.LocalDateTime endTime,
            @RequestParam(required = false) Integer attendeesCount,
            @RequestParam(required = false) String equipment) {
        return ResponseEntity.ok(bookingService
                .getAvailableRooms(startTime, endTime, attendeesCount, equipment)
                .stream().map(BookingMapper::toDTO).toList());
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
    public ResponseEntity<AvailabilityResponseDTO> getAvailability(
            @PathVariable Long id,
            @RequestParam LocalDate date,
            Authentication authentication) {
        Room room = roomService.getRoomById(id).orElse(null);
        if (room == null || (!isAdmin(authentication) && !room.isActive())) {
            return ResponseEntity.notFound().build();
        }
        var availability = bookingService.getAvailability(id, date);
        AvailabilityResponseDTO response = new AvailabilityResponseDTO(
                id,
                date,
                availability.getOccupiedSlots(),
                availability.getAvailableSlots()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, @Valid @RequestBody Room room) {
        return ResponseEntity.ok(BookingMapper.toDTO(roomService.updateRoom(id, room)));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDTO> setRoomActive(
            @PathVariable Long id, @Valid @RequestBody RoomStatusRequest request) {
        Room room = roomService.setRoomActive(id, request.active());
        return ResponseEntity.ok(BookingMapper.toDTO(room));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
