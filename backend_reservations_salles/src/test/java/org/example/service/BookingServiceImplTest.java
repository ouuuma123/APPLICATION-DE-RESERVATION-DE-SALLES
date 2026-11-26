package org.example.service;

import org.example.dto.BookingRequestDTO;
import org.example.dto.BookingResponseDTO;
import org.example.exception.BookingConflictException;
import org.example.exception.InvalidBookingException;
import org.example.model.Booking;
import org.example.model.BookingStatus;
import org.example.model.Room;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RoomServiceInterface roomService;

    @Mock
    private UserServiceInterface userService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    // Objets réutilisés dans plusieurs tests
    private Room buildRoom() {
        return Room.builder().id(1L).name("Room 1").capacity(20).location("Location 1").equipment("Equipement 1").build();
    }
    private User buildUser() {
        return User.builder().id(1L).name("Oumaima Ouchlih").email("ouma@gmail.com").password("password123").department("departement").build();
    }
    private BookingRequestDTO buildValidDto() {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setTitle("Booking 1");
        dto.setRoomId(1L);
        dto.setUserId(1L);
        dto.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0));
        dto.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0));
        dto.setAttendeesCount(10);
        return dto;
    }


    @Test
    public void createBooking_avecDonneesValides_creeLaReservationAvecSucces() {
        // Arrange
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any(), eq(-1L)))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        // Act
        BookingResponseDTO result = bookingService.createBooking(dto);

        // Assert
        assertThat(result.getTitle()).isEqualTo("Booking 1");
        assertThat(result.getAttendeesCount()).isEqualTo(10);
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");

        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    public void createBooking_avecChampsObligatoiresManquants_leveExceptionValidation() {
        // Arrange
        BookingRequestDTO dto = buildValidDto();
        dto.setTitle(null);

        // Act & Assert
        InvalidBookingException ex = assertThrows(InvalidBookingException.class, ()-> {
            bookingService.createBooking(dto);
        });
        assertThat(ex.getMessage()).isEqualTo("Title is required");
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(roomService, userService);
    }

    @Test
    public void createBooking_avecEndTimeAvantStartTime_leveExceptionValidation() {
        // Arrange
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        dto.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        dto.setEndTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0));

        // Act & Assert
        InvalidBookingException ex = assertThrows(InvalidBookingException.class, ()-> {
            bookingService.createBooking(dto);
        });

        assertThat(ex.getMessage()).isEqualTo("Start time must be before end time");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void createBooking_avecStartTimeDansLePasse_leveExceptionValidation() {
        // Arrange
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        dto.setStartTime(LocalDateTime.now().minusHours(1));
        dto.setEndTime(LocalDateTime.now().plusHours(1));

        // Act & Assert
        InvalidBookingException ex = assertThrows(InvalidBookingException.class , ()-> {
            bookingService.createBooking(dto);
        });

        assertThat(ex.getMessage()).isEqualTo("Start time must be in the future");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void createBooking_avecDureeInferieureAQuinzeMinutes_leveExceptionValidation() {
        // Arrange
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        dto.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0));
        dto.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(10).withSecond(0));

        // Act & Assert
        InvalidBookingException ex = assertThrows(InvalidBookingException.class , ()-> {
            bookingService.createBooking(dto);
        });

        assertThat(ex.getMessage()).isEqualTo("Booking duration must be at least 15 minutes");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void createBooking_avecDureeSuperieureAQuatreHeures_leveExceptionValidation() {
        // Arrange
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        dto.setStartTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0));
        dto.setEndTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(10).withSecond(1));

        // Act & Assert
        InvalidBookingException ex = assertThrows(InvalidBookingException.class , ()-> {
            bookingService.createBooking(dto);
        });

        assertThat(ex.getMessage()).isEqualTo("Booking duration must not exceed 4 hours");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void createBooking_avecAttendeesSuperieurACapaciteSalle_leveExceptionValidation() {
        // Arrange
        Room room = Room.builder().id(1L).name("Room").capacity(15).location("Location").equipment("Equipement").build();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        dto.setAttendeesCount(20);

        // Act & Assert
        InvalidBookingException ex = assertThrows(InvalidBookingException.class , ()-> {
            bookingService.createBooking(dto);
        });

        assertThat(ex.getMessage()).isEqualTo("Attendees count (20) exceeds room capacity (15)");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void createBooking_avecChevauchementDeCreneau_leveExceptionConflit() {
        // Arrange
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();
        Booking existingBooking = Booking.builder().id(50L).build();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any(), eq(-1L)))
                .thenReturn(List.of(existingBooking));

        // Act & Assert
        BookingConflictException ex = assertThrows(BookingConflictException.class, ()-> {
            bookingService.createBooking(dto);
        });

        assertThat(ex.getMessage()).isEqualTo("Room is already booked for this time slot");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void createBooking_demarrantPileQuandLAncienSeTermine_creeLaReservationAvecSucces() {
        // Arrange
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        // Créer une réservation existante qui se termine à 10:00
        LocalDateTime existingEndTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0);
        LocalDateTime existingStartTime = existingEndTime.minusHours(1);
        Booking existingBooking = Booking.builder()
                .id(50L)
                .startTime(existingStartTime)
                .endTime(existingEndTime)
                .build();

        // La nouvelle réservation commence exactement quand l'ancienne se termine
        dto.setStartTime(existingEndTime);
        dto.setEndTime(existingEndTime.plusHours(1));

        when(bookingRepository.findConflictingBookings(eq(1L), any(), any(), eq(-1L)))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        // Act
        BookingResponseDTO result = bookingService.createBooking(dto);

        // Assert
        assertThat(result.getTitle()).isEqualTo("Booking 1");
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    public void cancelBooking_reservationConfirmee_passeAuStatutAnnulee() {
        // Arrange
        Long bookingId = 1L;
        Booking booking = Booking.builder()
                .id(bookingId)
                .title("Booking 1")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        // Act
        bookingService.cancelBooking(bookingId);

        // Assert
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository, times(1)).save(any());
    }

    @Test
    public void updateBooking_sansConflitAvecElleMeme_metAJourAvecSucces() {
        // Arrange
        Long bookingId = 5L;
        Room room = buildRoom();
        User user = buildUser();
        BookingRequestDTO dto = buildValidDto();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any(), eq(bookingId)))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        bookingService.updateBooking(bookingId, dto);
        verify(bookingRepository).findConflictingBookings(eq(1L), any(), any(), eq(bookingId));

    }

}