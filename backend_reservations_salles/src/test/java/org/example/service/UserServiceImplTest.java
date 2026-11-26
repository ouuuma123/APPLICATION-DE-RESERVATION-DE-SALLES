package org.example.service;

import org.example.model.BookingStatus;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void createUser_avecDonneesValides_creeLUtilisateurAvecSucces() {
        // Arrange
        User user = new User("Oumaima", "ouma@gmail.com", "password123", "Departement 1");
        when(userRepository.save(user)).thenReturn(user);

        // Act
        User result = userService.createUser(user);

        // Assert
        assertThat(result.getName()).isEqualTo("Oumaima");
        assertThat(result.getEmail()).isEqualTo("ouma@gmail.com");
        assertThat(result.getPassword()).isEqualTo("password123");
        assertThat(result.getDepartment()).isEqualTo("Departement 1");

        verify(userRepository, times(1)).save(any());
    }

    @Test
    public void createUser_avecEmailDejaExistant_leveExceptionUniciteEmail() {
        // Arrange
        User user = new User("Oumaima", "ouma@gmail.com", "password123", "Departement 1");
        User existingUser = new User("Existing User", "ouma@gmail.com", "password123", "Departement 1");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(existingUser));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            userService.createUser(user);
        });

        assertThat(ex.getMessage()).isEqualTo("Email already exists: " + user.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void createUser_avecEmailManquant_leveExceptionValidation() {
        // Arrange
        User user = new User("Oumaima", null, "password123", "Departement 1");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            userService.createUser(user);
        });

        assertThat(ex.getMessage()).isEqualTo("Email is required");
        verify(userRepository, never()).save(any());
    }

    @Test
    public void createUser_avecEmailFormatInvalide_leveExceptionValidation() {
        // Arrange
        User user = new User("Oumaima", "ouma", "password123", "Departement 1");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            userService.createUser(user);
        });

        assertThat(ex.getMessage()).isEqualTo("Email should be valid");
        verify(userRepository, never()).save(any());
    }

    @Test
    public void createUser_avecNomManquant_leveExceptionValidation() {
        // Arrange
        User user = new User(null, "ouma@gmail.com", "password123", "Departement 1");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            userService.createUser(user);
        });

        assertThat(ex.getMessage()).isEqualTo("Name is required");
        verify(userRepository, never()).save(any());
    }

    @Test
    public void deleteUser_sansReservationActive_supprimeAvecSucces() {
        // Arrange
        Long userId = 1L;
        when(bookingRepository.existsByUserIdAndStatus(userId, BookingStatus.CONFIRMED)).thenReturn(false);

        // Act & Assert
        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    public void deleteUser_avecReservationActive_leveException() {
        // Arrange
        Long userId = 1L;
        when(bookingRepository.existsByUserIdAndStatus(userId, BookingStatus.CONFIRMED)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            userService.deleteUser(userId);
        });

        assertThat(ex.getMessage()).isEqualTo("Cannot delete user with active bookings");

        verify(userRepository, never()).deleteById(userId);
    }

}