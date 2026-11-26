package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.User;
import org.example.service.UserServiceInterface;
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
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserServiceInterface userService;

    @GetMapping
    public String listUsers(@RequestParam(required = false) String search, Model model) {
        List<User> users = userService.getAllUsers();
        
        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.toLowerCase();
            users = users.stream()
                    .filter(user -> user.getName().toLowerCase().contains(searchTerm) ||
                            user.getEmail().toLowerCase().contains(searchTerm) ||
                            (user.getDepartment() != null && user.getDepartment().toLowerCase().contains(searchTerm)))
                    .collect(Collectors.toList());
            model.addAttribute("search", search);
        }
        
        model.addAttribute("users", users);
        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "users/form";
    }

    @PostMapping
    public String createUser(@ModelAttribute User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        // Validation manuelle pour contourner le problème de validation du mot de passe
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors: name - Name is required; ");
            return "redirect:/users/new";
        }
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors: email - Email is required; ");
            return "redirect:/users/new";
        }
        
        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
            return "redirect:/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating user: " + e.getMessage());
            return "redirect:/users/new";
        }
    }

    @GetMapping("/{id}")
    public String showUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userService.getUserById(id)
                .map(user -> {
                    model.addAttribute("user", user);
                    return "users/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                    return "redirect:/users";
                });
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userService.getUserById(id)
                .map(user -> {
                    model.addAttribute("user", user);
                    return "users/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                    return "redirect:/users";
                });
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id, @Valid @ModelAttribute User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Validation errors: ");
            bindingResult.getFieldErrors().forEach(error -> 
                errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ")
            );
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage.toString());
            return "redirect:/users/" + id + "/edit";
        }
        
        try {
            userService.updateUser(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
            return "redirect:/users/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
            return "redirect:/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
            return "redirect:/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
            return "redirect:/users";
        }
    }
}
