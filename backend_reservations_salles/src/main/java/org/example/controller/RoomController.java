package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.Room;
import org.example.service.RoomServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class RoomController {

    @Autowired
    private RoomServiceInterface roomService;

    @GetMapping
    public String listRooms(@RequestParam(required = false) String search, Model model) {
        List<Room> rooms = roomService.getAllRooms();
        
        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.toLowerCase();
            rooms = rooms.stream()
                    .filter(room -> room.getName().toLowerCase().contains(searchTerm) ||
                            (room.getLocation() != null && room.getLocation().toLowerCase().contains(searchTerm)) ||
                            (room.getEquipment() != null && room.getEquipment().toLowerCase().contains(searchTerm)))
                    .collect(Collectors.toList());
            model.addAttribute("search", search);
        }
        
        model.addAttribute("rooms", rooms);
        return "rooms/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("room", new Room());
        return "rooms/form";
    }

    @PostMapping
    public String createRoom(@Valid @ModelAttribute Room room, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Validation errors: ");
            bindingResult.getFieldErrors().forEach(error -> 
                errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ")
            );
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage.toString());
            return "redirect:/rooms/new";
        }
        
        try {
            roomService.createRoom(room);
            redirectAttributes.addFlashAttribute("successMessage", "Room created successfully!");
            return "redirect:/rooms";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating room: " + e.getMessage());
            return "redirect:/rooms/new";
        }
    }

    @GetMapping("/{id}")
    public String showRoom(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return roomService.getRoomById(id)
                .map(room -> {
                    model.addAttribute("room", room);
                    return "rooms/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Room not found");
                    return "redirect:/rooms";
                });
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return roomService.getRoomById(id)
                .map(room -> {
                    model.addAttribute("room", room);
                    return "rooms/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Room not found");
                    return "redirect:/rooms";
                });
    }

    @PostMapping("/{id}")
    public String updateRoom(@PathVariable Long id, @Valid @ModelAttribute Room room, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Validation errors: ");
            bindingResult.getFieldErrors().forEach(error -> 
                errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ")
            );
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage.toString());
            return "redirect:/rooms/" + id + "/edit";
        }
        
        try {
            roomService.updateRoom(id, room);
            redirectAttributes.addFlashAttribute("successMessage", "Room updated successfully!");
            return "redirect:/rooms/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating room: " + e.getMessage());
            return "redirect:/rooms/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully!");
            return "redirect:/rooms";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting room: " + e.getMessage());
            return "redirect:/rooms";
        }
    }
}
