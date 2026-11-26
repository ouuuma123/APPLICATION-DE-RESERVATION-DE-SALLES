package org.example.controller;

import org.example.model.User;
import org.example.service.UserServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserServiceInterface userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void listUsers_sansParametreRecherche_retourneToutesLesListes() throws Exception {
        // Arrange
        User user = new User();
        user.setName("Oumaima");
        user.setEmail("oumaima@gmail.com");
        when(userService.getAllUsers()).thenReturn(List.of(user));

        // Act & Assert
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", List.of(user)));
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void listUsers_avecParametreRecherche_filtreLesResultats() throws Exception {
        // Arrange
        User user1 = new User();
        user1.setName("Oumaima");
        user1.setEmail("oumaima@gmail.com");
        user1.setDepartment("IT");

        User user2 = new User();
        user2.setName("Nouur");
        user2.setEmail("nouur@gmail.com");
        user2.setDepartment("RH");

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/users").param("search", "oumaima"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attribute("users", List.of(user1)))
                .andExpect(model().attribute("search", "oumaima"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createUser_avecDonneesValides_redirigeVersListeAvecSucces() throws Exception {
        // Arrange
        when(userService.createUser(any(User.class))).thenReturn(new User());

        // Act & Assert
        mockMvc.perform(post("/users")
                        .param("name", "oumaima")
                        .param("email", "ouma@gmail.com")
                        .param("department", "IT")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("successMessage", "User created successfully!"));
        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createUser_avecDonneesInvalides_redirigeVersFormulaireAvecErreur() throws Exception {
        mockMvc.perform(post("/users")
                        .param("email", "oumaima@gmail.com")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/new"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void showUser_utilisateurExistant_afficheLeDetail() throws Exception {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setName("Oumaima");

        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/detail"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void showUser_utilisateurInexistant_redirigeAvecErreur() throws Exception {
        // Arrange
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/users/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("errorMessage", "User not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteUser_succes_redirigeAvecMessageSucces() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1L);

        // Act & Assert
        mockMvc.perform(post("/users/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attribute("successMessage", "User deleted successfully!"));
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    public void listUsers_sansAuthentification_estRefuse() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }
}