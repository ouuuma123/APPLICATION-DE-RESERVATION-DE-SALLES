package org.example.selenium;

import org.example.model.Role;
import org.example.model.User;
import org.example.repository.RoleRepository;
import org.example.repository.UserRepository;
import org.example.selenium.pages.LoginPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebDriverScope;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BaseSeleniumTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected WebDriver driver;
    protected String baseUrl;

    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "adminPassword123";
    protected static final String CLIENT_EMAIL = "client@example.com";
    protected static final String CLIENT_PASSWORD = "clientPass123";


    @BeforeEach
    void setUpSeleniumBase() {
        // Préparation des données
        userRepository.deleteAll();
        roleRepository.deleteAll();
        Role adminRole = roleRepository.save(new Role("ADMIN"));
        Role clientRole = roleRepository.save(new Role("CLIENT"));
        createUser(ADMIN_EMAIL, ADMIN_PASSWORD, adminRole);
        createUser(CLIENT_EMAIL, CLIENT_PASSWORD, clientRole);

        // Préparation du navigateur
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        baseUrl = "http://localhost:" + port;

    }

    @AfterEach
    void tearDownSeleniumBase() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected User createUser(String email, String rawPassword, Role role) {
        User user = User.builder()
                .name("Utilisateur Test")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .department("QA")
                .roles(new ArrayList<>(List.of(role)))
                .build();
        return userRepository.save(user);
    }

    protected void loginAs(String email, String password) {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(baseUrl);
        loginPage.login(email, password);
    }
}
