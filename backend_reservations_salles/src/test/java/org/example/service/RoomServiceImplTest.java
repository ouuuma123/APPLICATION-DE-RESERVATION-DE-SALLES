package org.example.service;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.example.model.BookingStatus;
import org.example.model.Room;
import org.example.repository.BookingRepository;
import org.example.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ReflectionTestUtils.setField(roomService, "validator", validator);
    }

    @Test
    public void createRoom_avecDonneesValides_creeLaSalleAvecSucces() {
        // Arrange
        Room room = new Room("Room 1", 15, "Location 1", "Equipement 1");
        when(roomRepository.save(room)).thenReturn(room);

        // Act
        Room result = roomService.createRoom(room);

        // Assert
        assertThat(result.getName()).isEqualTo("Room 1");
        assertThat(result.getCapacity()).isEqualTo(15);
        assertThat(result.getLocation()).isEqualTo("Location 1");
        assertThat(result.getEquipment()).isEqualTo("Equipement 1");
        verify(roomRepository, times(1)).save(any());
    }

    @Test
    public void createRoom_avecNomVide_leveExceptionValidation() {
        // Arrange
        Room room = new Room(null, 15, "Location 1", "Equipement 1");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            roomService.createRoom(room);
        });

        assertThat(ex.getMessage()).isEqualTo("Room name is required");
        verify(roomRepository, never()).save(any());
    }

    @Test
    public void createRoom_avecNomHorsLimites_leveExceptionValidation() {
        // Arrange
        Room room1 = new Room("n", 15, "Location 1", "Equipement 1");
        Room room2 = new Room("n".repeat(51), 15, "Location 1", "Equipement 1");

        // Act & Assert
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, ()-> {
            roomService.createRoom(room1);
        });
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, ()-> {
            roomService.createRoom(room2);
        });

        assertThat(ex1.getMessage()).isEqualTo("Room name must be between 2 and 50 characters");
        assertThat(ex2.getMessage()).isEqualTo("Room name must be between 2 and 50 characters");

        verify(roomRepository, never()).save(any());
    }

    @Test
    public void createRoom_avecNomDejaExistant_leveExceptionUniciteNom() {
        // Arrange
        Room room = new Room("Room 1", 15, "Location 1", "Equipement 1");
        when(roomRepository.existsByName("Room 1")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            roomService.createRoom(room);
        });

        assertThat(ex.getMessage()).isEqualTo("Room name must be unique");
        verify(roomRepository, never()).save(any());
    }

    @Test
    public void createRoom_avecCapaciteManquante_leveExceptionValidation() {
        // Arrange
        Room room = new Room("Room 1", null, "Location 1", "Equipement 1");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
            roomService.createRoom(room);
        });

        assertThat(ex.getMessage()).isEqualTo("Capacity is required");
        verify(roomRepository, never()).save(any());
    }

    @Test
    public void createRoom_avecCapaciteInvalide_leveExceptionValidation() {
        // Arrange
        Room room1 = new Room("Room 1", 0, "Location 1", "Equipement 1");
        Room room2 = new Room("Room 2", -1, "Location 1", "Equipement 1");

        // Act & Asset
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, ()-> {
            roomService.createRoom(room1);
        });
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, ()-> {
            roomService.createRoom(room2);
        });

        assertThat(ex1.getMessage()).isEqualTo("Capacity must be at least 1");
        assertThat(ex2.getMessage()).isEqualTo("Capacity must be at least 1");

        verify(roomRepository, never()).save(any());
    }

    @Test
    public void deleteRoom_sansReservationActive_supprimeAvecSucces() {
        // Arrange
        Long roomId = 1L;
        when(bookingRepository.existsByRoomIdAndStatus(roomId, BookingStatus.CONFIRMED)).thenReturn(false);

        // Act & Assert
        roomService.deleteRoom(roomId);

        verify(roomRepository, times(1)).deleteById(roomId);
    }

    @Test
    public void deleteRoom_avecReservationActive_leveException() {
        // Arrange
        Long roomId = 1L;
        when(bookingRepository.existsByRoomIdAndStatus(roomId, BookingStatus.CONFIRMED)).thenReturn(true);

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, ()-> {
            roomService.deleteRoom(roomId);
        });

        assertThat(ex.getMessage()).isEqualTo("Cannot delete room with active bookings");
        verify(roomRepository, never()).deleteById(roomId);
    }
}