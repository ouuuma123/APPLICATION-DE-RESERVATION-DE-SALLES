package org.example.selenium;

import org.example.selenium.pages.UserFormPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserSeleniumTest extends BaseSeleniumTest {

    @Test
    void createUser_viaFormulaireActuel_echoueCarPasswordManquant() {
        loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        UserFormPage formPage = new UserFormPage(driver);

        formPage.openCreateForm(baseUrl);
        formPage.fillANdSubmit("Nouvel Utilisateur", "nv@example.com", "IT");

        assertThat(driver.getCurrentUrl()).isEqualTo(baseUrl + "/users/new");
        assertThat(formPage.hasErrorMessage()).isTrue();
    }

    @Test
    void accesUsers_avecClientConnecte_estRefuse() {
        loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        driver.get(baseUrl + "/users");

        assertThat(driver.getPageSource().toLowerCase()).contains("403");
    }
}
