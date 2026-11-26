package org.example.selenium;

import org.example.model.Room;
import org.example.model.User;
import org.example.repository.RoomRepository;
import org.example.repository.UserRepository;
import org.example.selenium.pages.BookingFormPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingSeleniumTest extends BaseSeleniumTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private Room testRoom;
    private User testUser;

    @BeforeEach
    void setUpRoom() {
        testRoom = roomRepository.save(
                Room.builder().name("Salle 1").capacity(10).build()
        );

        // Créer explicitement un utilisateur de test pour garantir son existence
        testUser = userRepository.save(
                User.builder()
                        .name("Utilisateur Test")
                        .email("testuser@example.com")
                        .password(passwordEncoder.encode("testPassword123"))
                        .department("QA")
                        .roles(new java.util.ArrayList<>())
                        .build()
        );
    }

    @Test
    void createBooking_avecClientConnecte_reussit() {
        loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        BookingFormPage formPage = new BookingFormPage(driver);
        formPage.openCreateForm(baseUrl);
        formPage.fillAndSubmit(
                "Réunion Selenium",
                testRoom.getName(),
                testUser.getName(), // utilise l'utilisateur créé explicitement
                "2026-09-15T10:00",
                "2026-09-15T11:00",
                "5"
        );

        assertThat(driver.getCurrentUrl()).isEqualTo(baseUrl + "/bookings");
    }
}
