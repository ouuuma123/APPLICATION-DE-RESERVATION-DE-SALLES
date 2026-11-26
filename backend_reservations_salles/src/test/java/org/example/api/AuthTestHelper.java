package org.example.api;

import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.*;

public class AuthTestHelper {

    public static SessionFilter loginAs(String email, String password) {
        SessionFilter sessionFilter = new SessionFilter();

        given()
                .filter(sessionFilter)
                .contentType(ContentType.URLENC)
                .formParam("username", email)
                .formParam("password", password)

        .when()
                .post("/login")
        .then()
                .statusCode(302);

        return sessionFilter;
    }

}
