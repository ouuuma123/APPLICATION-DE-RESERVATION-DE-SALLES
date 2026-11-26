package org.example.api;

import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.example.api.BaseApiTest.*;
import static org.hamcrest.Matchers.*;

public class BookingRestControllerTest {
    @Test
    void createBooking_avecUtilisateurClient_retourne201() {

        SessionFilter session = AuthTestHelper.loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                      {
                            "title": "Sprint Planning",
                            "roomId": 1,
                            "userId": 1,
                            "startTime": "2026-09-10T10:00:00",
                            "endTime": "2026-09-10T11:00:00"
                      }
                """)
        .when()
                .post("/api/bookings")
        .then()
                .statusCode(201)
                .body("title", equalTo("Sprint Planning"));
    }

    @Test
    void createBooking_sansAuthentification_estRejete() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                      {
                            "title": "Sprint Planning",
                            "roomId": 1,
                            "userId": 1,
                            "startTime": "2026-09-10T10:00:00",
                            "endTime": "2026-09-10T11:00:00"
                      }
                """)
        .when()
                .post("/api/bookings")
        .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    void createBooking_avecTitreManquant_retourne400() {
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                      {
                            "roomId": 1,
                            "userId": 1,
                            "startTime": "2026-09-10T10:00:00",
                            "endTime": "2026-09-10T11:00:00"
                      }
                """)
        .when()
                .post("/api/bookings")
        .then()
                .statusCode(400);
    }

    @Test
    void getBookingById_inexistant_retourne404() {
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        given()
                .filter(session)
                .when()
                .get("/api/bookings/99999")
                .then()
                .statusCode(404);

    }
}
