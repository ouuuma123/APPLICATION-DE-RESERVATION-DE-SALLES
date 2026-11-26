package org.example.api;

import org.example.model.Role;
import org.example.model.User;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import io.restassured.RestAssured;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "adminPassword123";

    protected static final String CLIENT_EMAIL = "client@example.com";
    protected static final String CLIENT_PASSWORD = "clientPass123";

    @BeforeEach
    void setUpBase() {
        RestAssured.port = port;

        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(new Role("ADMIN"));
        Role clientRole = roleRepository.save(new Role("CLIENT"));

        createUser(ADMIN_EMAIL, ADMIN_PASSWORD, adminRole);
        createUser(CLIENT_EMAIL, CLIENT_PASSWORD, clientRole);
    }

    protected User createUser(String email,String rawPassword, Role role) {
        User user = User.builder()
                .name("Utilisateur Test")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .department("QA")
                .roles(new ArrayList<>(List.of(role)))
                .build();
        return userRepository.save(user);
    }
}
