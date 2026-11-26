package org.example.controller;

import org.example.dto.BookingRequestDTO;
import org.example.dto.BookingResponseDTO;
import org.example.exception.BookingConflictException;
import org.example.exception.InvalidBookingException;
import org.example.model.Booking;
import org.example.model.BookingStatus;
import org.example.model.Room;
import org.example.model.User;
import org.example.service.BookingServiceInterface;
import org.example.service.RoomServiceInterface;
import org.example.service.UserServiceInterface;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingServiceInterface bookingService;

    @MockBean
    private RoomServiceInterface roomService;

    @MockBean
    private UserServiceInterface userService;

    private Booking buildBooking() {
        Room room = new Room("Salle Atlas", 10, "2ème étage", "Écran, Wifi");
        room.setId(1L);
        User user = new User("Oumaima", "ouma@gmail.com", "password123", "IT");
        user.setId(1L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setTitle("Sprint Planning");
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        booking.setAttendeesCount(5);
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }

    @Test
    @WithMockUser
    public void listBookings_sansParametreRecherche_retourneToutesLesReservations() throws Exception {
        // Arrange
        Booking booking = buildBooking();
        when(bookingService.getAllBookings()).thenReturn(List.of(booking));

        // Act & Assert
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/list"))
                .andExpect(model().attribute("bookings", List.of(booking)));

    }

    @Test
    @WithMockUser
    public void listBookings_avecRecherche_filtreParTitre() throws Exception {
        // Arrange
        Booking booking1 = buildBooking();
        Booking booking2 = buildBooking();
        booking2.setTitle("Booking 2");
        when(bookingService.getAllBookings()).thenReturn(List.of(booking1, booking2));

        // Act & Assert
        mockMvc.perform(get("/bookings")
                        .param("search", "Sprint"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/list"))
                .andExpect(model().attribute("bookings", List.of(booking1)))
                .andExpect(model().attribute("search", "Sprint"));
    }

    @Test
    @WithMockUser
    public void showCreateForm_preparerModelAvecSallesEtUtilisateurs() throws Exception {
        // Arrange
        Room room = new Room("Room 1", 15, "Location", "Equipement");
        User user = new User("Oumaima", "ouma@gmail.com", "password", "IT");

        when(roomService.getAllRooms()).thenReturn(List.of(room));
        when(userService.getAllUsers()).thenReturn(List.of(user));

        // Act & Assert
        mockMvc.perform(get("/bookings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/form"))
                .andExpect(model().attributeExists("booking"))
                .andExpect(model().attribute("rooms", List.of(room)))
                .andExpect(model().attribute("users", List.of(user)))
                .andExpect(model().attribute("selectedRoomId", (Object) null))
                .andExpect(model().attribute("selectedUserId", (Object) null));

    }

    @Test
    @WithMockUser
    public void createBooking_avecDonneesValides_redirigeVersListeAvecSucces() throws Exception {
        // Arrange
        when(bookingService.createBooking(any(BookingRequestDTO.class))).thenReturn(new BookingResponseDTO());

        // Act & Assert
        mockMvc.perform(post("/bookings")
                        .param("title", "Sprint Planning")
                        .param("startTime", "2026-09-10T10:00:00")
                        .param("endTime", "2026-09-10T11:00:00")
                        .param("attendeesCount", "5")
                        .param("roomId", "1")
                        .param("userId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"))
                .andExpect(flash().attribute("successMessage", "Booking created successfully!"));

        var captor = ArgumentCaptor.forClass(BookingRequestDTO.class);
        verify(bookingService, times(1)).createBooking(captor.capture());
        assertThat(captor.getValue().getRoomId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getTitle()).isEqualTo("Sprint Planning");

    }

    @Test
    @WithMockUser
    public void createBooking_avecTitreManquant_redirigeVersFormulaireAvecErreur() throws Exception {
        mockMvc.perform(post("/bookings")
                        .param("startTime", "2026-09-10T10:00:00")
                        .param("endTime", "2026-09-10T11:00:00")
                        .param("attendeesCount", "5")
                        .param("roomId", "1")
                        .param("userId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/new"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(bookingService, never()).createBooking(any());
    }

    @Test
    @WithMockUser
    public void createBooking_avecConflitDeCreneau_redirigeAvecMessageErreurDuService() throws Exception {
        // Arrange
        when(bookingService.createBooking(any(BookingRequestDTO.class)))
                .thenThrow(new BookingConflictException("Room is already booked for this time slot"));

        // Act & Assert
        mockMvc.perform(post("/bookings")
                        .param("title", "Sprint Planning")
                        .param("startTime", "2026-09-10T10:00:00")
                        .param("endTime", "2026-09-10T11:00:00")
                        .param("attendeesCount", "5")
                        .param("roomId", "1")
                        .param("userId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/new"))
                .andExpect(flash().attribute("errorMessage", "Room is already booked for this time slot"));
    }

    @Test
    @WithMockUser
    public void showBooking_reservationExistante_afficheLeDetail() throws Exception {
        // Arrange
        Booking booking = buildBooking();
        when(bookingService.getBookingById(1L)).thenReturn(Optional.of(booking));

        // Act & Assert
        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("booking", booking))
                .andExpect(view().name("bookings/detail"));
    }

    @Test
    @WithMockUser
    public void showBooking_reservationInexistante_redirigeSansMessageFlash() throws Exception {
        // Arrange
        when(bookingService.getBookingById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/bookings/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    @WithMockUser
    public void cancelBooking_succes_redirigeVersDetailAvecSucces() throws Exception {
        // Arrange
        doNothing().when(bookingService).cancelBooking(1L);

        // Act & Assert
        mockMvc.perform(post("/bookings/1/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/1"))
                .andExpect(flash().attribute("successMessage", "Booking cancelled successfully!"));
    }

    @Test
    @WithMockUser
    public void cancelBooking_echec_redirigeAvecMessageErreur() throws Exception {
        // Arrange
        doThrow(new InvalidBookingException("Booking not found with id: 1")).when(bookingService)
                .cancelBooking(1L);

        // Act & Assert
        mockMvc.perform(post("/bookings/1/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings/1"))
                .andExpect(flash().attribute("errorMessage","Error cancelling booking: Booking not found with id: 1"));
    }

    @Test
    @WithMockUser
    public void deleteBooking_succes_redirigeVersListeAvecSucces() throws Exception {
        // Arrange
        doNothing().when(bookingService).deleteBooking(1L);

        // Act & Assert
        mockMvc.perform(post("/bookings/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"))
                .andExpect(flash().attribute("successMessage", "Booking deleted successfully!"));
    }

    @Test
    @WithMockUser
    public void listUserBookings_retourneLesReservationsDeCetUtilisateur() throws Exception {
        // Arrange
        Booking booking = buildBooking();
        when(bookingService.getBookingsByUserId(1L)).thenReturn(List.of(booking));

        // Act & Assert
        mockMvc.perform(get("/bookings/user/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/list"))
                .andExpect(model().attribute("bookings", List.of(booking)))
                .andExpect(model().attribute("userId", 1L));

    }

    @Test
    public void listBookings_sansAuthentification_estRefuse() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized());
    }

}
