package org.example.api;

import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.example.api.BaseApiTest.*;
import static org.hamcrest.Matchers.*;

public class UserRestControllerTest {

    @Test
    void createUser_avecRoleAdmin_retourne201() {
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                      {"name": "Ouma imaa", "email": "ouma@gmail.com", "password": "ouu123", "department": "IT"}
                """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(201)
                .body("email", equalTo("ouma@gmail.com"))
                .body("$", not(hasKey("password")));
    }

    @Test
    void createUser_avecRoleClient_retourne403() {
        SessionFilter session = AuthTestHelper.loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                      {"name": "Ouma imaa", "email": "ouma@gmail.com", "password": "ouu123", "department": "IT"}
                """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(403);
    }

    @Test
    void createUser_avecEmailInvalide_retourne400() {
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        given()
                .filter(session)
                .contentType(ContentType.JSON)
                .body("""
                      {"name": "Ouma imaa", "email": "oumaaaa", "password": "ouu123", "department": "IT"}
                """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(400);
    }

    @Test
    void getAllUsers_avecRoleAdmin_retourneListe() {
        SessionFilter session = AuthTestHelper.loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        given()
                .filter(session)
        .when()
                .get("/api/users")
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }
}
