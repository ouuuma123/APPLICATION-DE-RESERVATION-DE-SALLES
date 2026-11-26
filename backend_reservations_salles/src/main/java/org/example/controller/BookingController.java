package org.example.controller;

import jakarta.validation.Valid;
import org.example.exception.BookingConflictException;
import org.example.model.Booking;
import org.example.model.Room;
import org.example.model.User;
import org.example.service.BookingServiceInterface;
import org.example.service.RoomServiceInterface;
import org.example.service.UserServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/bookings")
@PreAuthorize("isAuthenticated()")
public class BookingController {

    @Autowired
    private BookingServiceInterface bookingService;

    @Autowired
    private RoomServiceInterface roomService;

    @Autowired
    private UserServiceInterface userService;

    @GetMapping
    public String listBookings(@RequestParam(required = false) String search, Model model) {
        List<Booking> bookings = bookingService.getAllBookings();
        
        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.toLowerCase();
            bookings = bookings.stream()
                    .filter(booking -> booking.getTitle().toLowerCase().contains(searchTerm) ||
                            (booking.getRoom() != null && booking.getRoom().getName().toLowerCase().contains(searchTerm)) ||
                            (booking.getUser() != null && booking.getUser().getName().toLowerCase().contains(searchTerm)))
                    .collect(Collectors.toList());
            model.addAttribute("search", search);
        }
        
        model.addAttribute("bookings", bookings);
        return "bookings/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("booking", new Booking());
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("selectedRoomId", null);
        model.addAttribute("selectedUserId", null);
        return "bookings/form";
    }

    @PostMapping
    public String createBooking(@ModelAttribute Booking booking,
                               @RequestParam(required = false) Long roomId,
                               @RequestParam(required = false) Long userId,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        // Validation manuelle du titre pour contourner les problèmes de validation JSR-303
        if (booking.getTitle() == null || booking.getTitle().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors: title - Title is required; ");
            return "redirect:/bookings/new";
        }
        
        try {
            bookingService.createBooking(mapToRequestDTO(booking, roomId, userId));
            redirectAttributes.addFlashAttribute("successMessage", "Booking created successfully!");
            return "redirect:/bookings";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/new";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/new";
        } catch (BookingConflictException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/new";
        }
    }

    @GetMapping("/{id}")
    public String showBooking(@PathVariable Long id, Model model) {
        return bookingService.getBookingById(id)
                .map(booking -> {
                    model.addAttribute("booking", booking);
                    return "bookings/detail";
                })
                .orElse("redirect:/bookings");
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        return bookingService.getBookingById(id)
                .map(booking -> {
                    model.addAttribute("booking", booking);
                    model.addAttribute("rooms", roomService.getAllRooms());
                    model.addAttribute("users", userService.getAllUsers());
                    model.addAttribute("selectedRoomId", booking.getRoom() != null ? booking.getRoom().getId() : null);
                    model.addAttribute("selectedUserId", booking.getUser() != null ? booking.getUser().getId() : null);
                    return "bookings/form";
                })
                .orElse("redirect:/bookings");
    }

    @PostMapping("/{id}")
    public String updateBooking(@PathVariable Long id, @Valid @ModelAttribute Booking booking,
                               @RequestParam(required = false) Long roomId,
                               @RequestParam(required = false) Long userId,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Validation errors: ");
            bindingResult.getFieldErrors().forEach(error -> 
                errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ")
            );
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage.toString());
            return "redirect:/bookings/" + id + "/edit";
        }
        
        try {
            bookingService.updateBooking(id, mapToRequestDTO(booking, roomId, userId));
            redirectAttributes.addFlashAttribute("successMessage", "Booking updated successfully!");
            return "redirect:/bookings/" + id;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully!");
            return "redirect:/bookings/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling booking: " + e.getMessage());
            return "redirect:/bookings/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.deleteBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking deleted successfully!");
            return "redirect:/bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting booking: " + e.getMessage());
            return "redirect:/bookings";
        }
    }

    @GetMapping("/user/{userId}")
    public String listUserBookings(@PathVariable Long userId, Model model) {
        model.addAttribute("bookings", bookingService.getBookingsByUserId(userId));
        model.addAttribute("userId", userId);
        return "bookings/list";
    }

    private org.example.dto.BookingRequestDTO mapToRequestDTO(Booking booking, Long roomId, Long userId) {
        org.example.dto.BookingRequestDTO dto = new org.example.dto.BookingRequestDTO();
        dto.setTitle(booking.getTitle());
        dto.setRoomId(roomId != null ? roomId : (booking.getRoom() != null ? booking.getRoom().getId() : null));
        dto.setUserId(userId != null ? userId : (booking.getUser() != null ? booking.getUser().getId() : null));
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setAttendeesCount(booking.getAttendeesCount());
        return dto;
    }
}
