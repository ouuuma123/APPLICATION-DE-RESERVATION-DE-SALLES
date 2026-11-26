package org.example.integration;

import org.example.model.BookingStatus;
import org.example.repository.BookingRepository;
import org.example.repository.RoomRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @AfterEach
    void cleanUp() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void workflowComplet_creationUtilisateurSalleEtReservation_toutEstCoherent() throws Exception {
        // étape 1 : créer un utilisateur
        mockMvc.perform(post("/users")
                        .param("name", "Ouma imaa")
                        .param("email", "ouma@gmail.com")
                        .param("department", "IT")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/new"));

        var savedUser = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("ouma@gmail.com"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Utilisateur non trouvé en base"));

        // étape 2 : créer une salle
        mockMvc.perform(post("/rooms")
                        .param("name", "Salle 1")
                        .param("capacity", "10")
                        .param("location", "2ème étage")
                        .param("equipment", "écran, wifi")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"));

        var savedRoom = roomRepository.findAll().stream()
                .filter(r -> r.getName().equals("Salle 1"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Salle non trouvée en base"));

        // étape 3 : créer une réservation
        mockMvc.perform(post("/bookings")
                        .param("title", "Sprint Planning")
                        .param("startTime", "2026-09-10T09:00:00")
                        .param("endTime", "2026-09-10T10:00:00")
                        .param("attendeesCount", "5")
                        .param("roomId", savedRoom.getId().toString())
                        .param("userId", savedUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        // étape 4 : vérifier la cohérence dans la liste ET la recherche
        var savedBooking = bookingRepository.findAll().stream()
                .filter(b -> b.getTitle().equals("Sprint Planning"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Réservation non trouvée en base"));

        assertThat(savedBooking.getRoom().getId()).isEqualTo(savedRoom.getId());
        assertThat(savedBooking.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("bookings"));

        mockMvc.perform(get("/bookings")
                        .param("search", "sprint"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createRoom_avecNomDejaExistantEnBase_estRefuseeSansCreerDeDoublon() throws Exception {
        // étape 1 : on crée une salle
        mockMvc.perform(post("/rooms")
                        .param("name", "Salle 1")
                        .param("capacity", "15")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"));

        assertThat(roomRepository.findAll()).hasSize(1);

        // étape 2 : on essaye de créer une autre classe avce le même nom
        mockMvc.perform(post("/rooms")
                        .param("name", "Salle 1")
                        .param("capacity", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms/new"))
                .andExpect(flash().attribute("errorMessage","Error creating room: Room name must be unique"));

        var rooms = roomRepository.findAll();
        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getCapacity()).isEqualTo(15);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteRoom_avecReservationConfirmeeReelleEnBase_estBloqueeEtLaSalleSubsiste() throws Exception {
        // étape 1 : on construit le workflow complet
        mockMvc.perform(post("/users")
                        .param("name", "ouma imaa")
                        .param("email", "oumaa@gmail.com")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/rooms")
                        .param("name", "Salle 1")
                        .param("capacity", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        var user = userRepository.findAll().get(0);
        var room = roomRepository.findAll().get(0);

        mockMvc.perform(post("/bookings")
                        .param("title", "Sprint Planning")
                        .param("startTime", "2026-09-10T09:00:00")
                        .param("endTime", "2026-09-10T10:00:00")
                        .param("attendeesCount", "5")
                        .param("roomId", room.getId().toString())
                        .param("userId", user.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(bookingRepository.findAll()).hasSize(1);

        // étape 2 : on tente de supprimer la salle
        mockMvc.perform(post("/rooms/" + room.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("errorMessage", "Error deleting room: Cannot delete room with active bookings"));

        // étape 3: vérification
        assertThat(roomRepository.findById(room.getId())).isPresent();
        assertThat(bookingRepository.findAll()).hasSize(1);
    }
}
