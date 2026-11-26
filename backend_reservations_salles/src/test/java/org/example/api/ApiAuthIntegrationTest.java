package org.example.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiAuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginProvidesJwtAcceptedByProtectedProfileEndpoint() {
        ResponseEntity<Map> login = restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", "admin@company.com", "password", "admin123"),
                Map.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().get("tokenType")).isEqualTo("Bearer");
        String token = (String) login.getBody().get("accessToken");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> profile = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profile.getBody().get("email")).isEqualTo("admin@company.com");
        assertThat((List<?>) profile.getBody().get("roles")).anyMatch("ADMIN"::equals);
    }

    @Test
    void publicRegistrationCreatesEmployeeAndReturnsUsableToken() {
        String email = "angular-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<Map> registration = restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "name", "Angular Employee",
                        "email", email,
                        "password", "employeePass123",
                        "department", "Engineering"),
                Map.class);

        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat((List<?>) registration.getBody().get("roles")).anyMatch("EMPLOYEE"::equals);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) registration.getBody().get("accessToken"));
        ResponseEntity<Map> profile = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profile.getBody().get("email")).isEqualTo(email);
    }

    @Test
    void protectedProfileRejectsAnonymousRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void employeeCanBookARoomAndOverlappingRequestIsRejected() {
        String email = "booking-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<Map> registration = restTemplate.postForEntity(
                "/api/auth/register",
                Map.of("name", "Booking Employee", "email", email, "password", "employeePass123"),
                Map.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) registration.getBody().get("accessToken"));

        ResponseEntity<List> roomResponse = restTemplate.exchange(
                "/api/rooms?minCapacity=1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);
        assertThat(roomResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map room = (Map) roomResponse.getBody().get(0);

        LocalDateTime start = LocalDateTime.now().plusDays(20).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(30);
        Map<String, Object> booking = Map.of(
                "title", "Angular API test",
                "roomId", room.get("id"),
                "startTime", start.toString(),
                "endTime", end.toString(),
                "attendeesCount", 2);

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/bookings",
                HttpMethod.POST,
                new HttpEntity<>(booking, headers),
                Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("userId")).isNotNull();

        String calendarRange = "/api/bookings?from=" + start.minusMinutes(1) + "&to=" + end.plusMinutes(1);
        ResponseEntity<List> calendar = restTemplate.exchange(
                calendarRange, HttpMethod.GET, new HttpEntity<>(headers), List.class);
        assertThat(calendar.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(calendar.getBody()).hasSize(1);

        ResponseEntity<String> conflict = restTemplate.exchange(
                "/api/bookings",
                HttpMethod.POST,
                new HttpEntity<>(booking, headers),
                String.class);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Void> cancelled = restTemplate.exchange(
                "/api/bookings/" + created.getBody().get("id") + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Void.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<List> notifications = restTemplate.exchange(
                "/api/notifications", HttpMethod.GET, new HttpEntity<>(headers), List.class);
        assertThat(notifications.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notifications.getBody()).hasSize(2);
        assertThat(notifications.getBody().stream()
                .map(item -> ((Map) item).get("type"))
                .anyMatch("BOOKING_CANCELLED"::equals)).isTrue();
    }

    @Test
    void angularOriginCanMakeCorsPreflightRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:4200");
        headers.setAccessControlRequestMethod(HttpMethod.GET);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/rooms",
                HttpMethod.OPTIONS,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:4200");
    }
}
