package org.example.selenium;

import org.example.selenium.pages.LoginPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginSeleniumTest extends BaseSeleniumTest {

    @Test
    void login_avecIdentifiantsValides_redirigeVersAccueil() {
        LoginPage loginPage = new LoginPage(driver);

        loginPage.open(baseUrl);
        loginPage.login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(driver.getCurrentUrl()).isEqualTo(baseUrl + "/");
    }

    @Test
    void login_avecMotDePasseIncorrect_afficheErreurEtResteSurLogin() {
        LoginPage loginPage = new LoginPage(driver);

        loginPage.open(baseUrl);
        loginPage.login(ADMIN_EMAIL, "mauvais_mdp");

        assertThat(driver.getCurrentUrl()).contains("/login");
        assertThat(loginPage.isErrorDisplayed()).isTrue();
    }
}
