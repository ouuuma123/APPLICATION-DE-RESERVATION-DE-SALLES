package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.BookingRequestDTO;
import org.example.dto.BookingResponseDTO;
import org.example.mapper.BookingMapper;
import org.example.model.Booking;
import org.example.model.User;
import org.example.service.BookingServiceInterface;
import org.example.service.UserServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
public class BookingRestController {

    private final BookingServiceInterface bookingService;
    private final UserServiceInterface userService;

    public BookingRestController(
            BookingServiceInterface bookingService, UserServiceInterface userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO dto, Authentication authentication) {
        if (!isAdmin(authentication)) {
            dto.setUserId(currentUser(authentication).getId());
        }
        BookingResponseDTO response = bookingService.createBooking(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> getBookings(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long userId,
            Authentication authentication) {
        boolean admin = isAdmin(authentication);
        List<Booking> bookings;
        if (from != null || to != null) {
            if (from == null || to == null) {
                return ResponseEntity.badRequest().build();
            }
            bookings = bookingService.getCalendarBookings(from, to, roomId);
        } else if (admin) {
            bookings = bookingService.getAllBookings();
        } else {
            bookings = bookingService.getBookingsByUserId(currentUser(authentication).getId());
        }
        if (admin && userId != null) {
            bookings = bookings.stream()
                    .filter(booking -> booking.getUser().getId().equals(userId))
                    .toList();
        }
        return ResponseEntity.ok(bookings.stream().map(BookingMapper::toDTO).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @PathVariable Long id, Authentication authentication) {
        return bookingService.getBookingById(id)
                .filter(booking -> ownsOrAdmin(booking, authentication))
                .map(booking -> ResponseEntity.ok(BookingMapper.toDTO(booking)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingRequestDTO dto,
            Authentication authentication) {
        Booking existing = bookingService.getBookingById(id).orElse(null);
        if (existing == null || !ownsOrAdmin(existing, authentication)) {
            return ResponseEntity.notFound().build();
        }
        if (!isAdmin(authentication)) {
            dto.setUserId(existing.getUser().getId());
        }
        return ResponseEntity.ok(bookingService.updateBooking(id, dto));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id, Authentication authentication) {
        Booking booking = bookingService.getBookingById(id).orElse(null);
        if (booking == null || !ownsOrAdmin(booking, authentication)) {
            return ResponseEntity.notFound().build();
        }
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId).stream()
                .map(BookingMapper::toDTO).toList());
    }

    private boolean ownsOrAdmin(Booking booking, Authentication authentication) {
        return isAdmin(authentication)
                || booking.getUser().getEmail().equalsIgnoreCase(authentication.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
