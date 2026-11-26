package org.example.api;

import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.example.api.BaseApiTest.*;
import static org.hamcrest.Matchers.*;

public class RoomRestControllerTest {

    @Test
    void createRoom_avecRoleAdmin_retourne201() {
        // Step 1: on se connecte en tant qu'ADMIN et on récupère la session
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Step 2
        given()  // je prépare la rêquete
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                      { "name":  "Salle 1", "capacity":  10 }
                """)
        .when()  // j'envoie la requête
                .post("/api/rooms")
        .then()  // je vérifie la réponse
                .statusCode(201)
                .body("name", equalTo("Salle 1"))
                .body("id", notNullValue());
    }

    @Test
    void createRoom_avecRoleClient_retourne403() {
        SessionFilter session = AuthTestHelper.loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                    {"name":  "Salle A", "capacity":  10 }
                """)
        .when()
                .post("/api/rooms")
        .then()
                .statusCode(403); // Forbidden
    }

    @Test
    void createRoom_sansAuthentification_estRejete() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {"name":  "Salle A", "capacity":  10 }
                """)
        .when()
                .post("/api/rooms")
        .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    void getAllRooms_avecRoleAdmin_retourneListe() {
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        // on créer d'abord une salle pour avoir quelque chose à lister
        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                    {"name":  "Salle A", "capacity":  10 }
                """)
                .post("/api/rooms");
        given()
                .filter(session)
        .when()
                .get("/api/rooms")
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("name", hasItem("Salle A"));
    }

    @Test
    void createRoom_avecNomManquant_retourne400() {
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                    {"capacity":  10 }
                """)
        .when()
                .post("/api/rooms")
        .then()
                .statusCode(400);
    }
}
