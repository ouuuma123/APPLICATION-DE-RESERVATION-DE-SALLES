package org.example.controller;

import org.example.model.Room;
import org.example.service.RoomServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RoomController.class)
public class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomServiceInterface roomService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void listRooms_sansParametreRecherche_retourneToutesLesSalles() throws Exception {
        // Assert
        Room room = new Room("Room1", 10, "2ème étage", "Écran, Wifi");
        when(roomService.getAllRooms()).thenReturn(List.of(room));

        // Act & Assert
        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/list"))
                .andExpect(model().attribute("rooms", List.of(room)));

        verify(roomService, times(1)).getAllRooms();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void listRooms_avecParametreRecherche_filtreParEquipement() throws Exception {
        // Assert
        Room room1 = new Room("Room1", 10, "2ème étage", "Écran, Wifi");
        Room room2 = new Room("Room2", 15, "2ème étage", "Tableau");
        when(roomService.getAllRooms()).thenReturn(List.of(room1, room2));

        // Act & Assert
        mockMvc.perform(get("/rooms").param("search", "wifi"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rooms", List.of(room1)))
                .andExpect(model().attribute("search", "wifi"));

    }


    @Test
    @WithMockUser(roles = "ADMIN")
    public void createRoom_avecDonneesValides_redirigeVersListeAvecSucces() throws Exception {
        // Arrange
        when(roomService.createRoom(any(Room.class))).thenReturn(new Room());

        // Act & Assert
        mockMvc.perform(post("/rooms")
                        .param("name", "Salle 1")
                        .param("capacity", "20")
                        .param("location", "2ème étage")
                        .param("equipment", "écran, wifi")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("successMessage", "Room created successfully!"));

        verify(roomService, times(1)).createRoom(any(Room.class));
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    public void createRoom_avecNomManquant_redirigeVersFormulaireAvecErreur() throws Exception {
        mockMvc.perform(post("/rooms")
                        .param("capacity", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms/new"))
                .andExpect(flash().attributeExists("errorMessage"));
        verify(roomService, never()).createRoom(any());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    public void showRoom_salleExistante_afficheLeDetail() throws Exception {
        // Arrange
        Room room = new Room("Room", 10, "2ème étage", "Écran, Wifi");
        room.setId(1L);
        when(roomService.getRoomById(1L)).thenReturn(Optional.of(room));

        // Act & Assert
        mockMvc.perform(get("/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/detail"))
                .andExpect(model().attribute("room", room));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void showRoom_salleInexistante_redirigeAvecErreur() throws Exception {
        // Arrange
        when(roomService.getRoomById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/rooms/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("errorMessage", "Room not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteRoom_sansReservationActive_redirigeAvecSucces() throws Exception {
        // Arrange
        doNothing().when(roomService).deleteRoom(1L);

        // Act & Assert
        mockMvc.perform(post("/rooms/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("successMessage", "Room deleted successfully!"));
        verify(roomService, times(1)).deleteRoom(1L);
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteRoom_avecReservationActive_redirigeAvecMessageErreur() throws Exception {
        // Arrange
        doThrow(new IllegalStateException(("Cannot delete room with active bookings")))
                .when(roomService).deleteRoom(1L);

        // Act & Assert
        mockMvc.perform(post("/rooms/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("errorMessage", "Error deleting room: Cannot delete room with active bookings"));

    }
}
