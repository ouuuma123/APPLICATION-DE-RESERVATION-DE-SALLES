package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.ProfileUpdateRequest;
import org.example.dto.RegisterRequest;
import org.example.dto.UserDTO;
import org.example.dto.UserRoleRequest;
import org.example.mapper.BookingMapper;
import org.example.model.User;
import org.example.service.AuthService;
import org.example.service.UserServiceInterface;
import org.example.model.Role;
import org.example.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserServiceInterface userService;
    private final AuthService authService;
    private final RoleRepository roleRepository;

    public UserRestController(
            UserServiceInterface userService, AuthService authService, RoleRepository roleRepository) {
        this.userService = userService;
        this.authService = authService;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
    public UserDTO getMyProfile(Authentication authentication) {
        return BookingMapper.toDTO(currentUser(authentication));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CLIENT')")
    public UserDTO updateMyProfile(
            @Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        user.setName(request.name());
        user.setDepartment(request.department());
        return BookingMapper.toDTO(userService.updateUser(user.getId(), user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(
                request.getName(), request.getEmail(), request.getPassword(), request.getDepartment());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingMapper.toDTO(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers().stream().map(BookingMapper::toDTO).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(BookingMapper.toDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id, @Valid @RequestBody ProfileUpdateRequest request) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setName(request.name());
        user.setDepartment(request.department());
        return ResponseEntity.ok(BookingMapper.toDTO(userService.updateUser(id, user)));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id, @Valid @RequestBody UserRoleRequest request) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Role employeeRole = roleRepository.findByName("EMPLOYEE")
                .orElseGet(() -> roleRepository.save(new Role("EMPLOYEE")));
        Role legacyClientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new Role("CLIENT")));
        user.getRoles().clear();
        user.getRoles().add(employeeRole);
        user.getRoles().add(legacyClientRole);
        if ("ADMIN".equals(request.role())) {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
            user.getRoles().add(adminRole);
        }
        return ResponseEntity.ok(BookingMapper.toDTO(userService.updateUser(id, user)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
