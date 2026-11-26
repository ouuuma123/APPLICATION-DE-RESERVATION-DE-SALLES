package org.example.selenium;

import org.example.selenium.pages.RoomFormPage;
import org.example.selenium.pages.RoomListPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoomSeleniumTest extends BaseSeleniumTest {

    @Test
    void createRoom_avecAdminConnecte_creeLaSalleEtRedirigeVersListe() {

        loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        RoomFormPage formPage = new RoomFormPage(driver);
        formPage.openCreateForm(baseUrl);
        formPage.fillAndSubmit("Salle 1", "10", "Batiment B", "Ecran, wifi");

        assertThat(driver.getCurrentUrl()).isEqualTo(baseUrl + "/rooms");

        RoomListPage listPage = new RoomListPage(driver);
        assertThat(listPage.containsRoomNamed("Salle 1")).isTrue();

    }

    @Test
    void createRoom_avecCapaciteManquante_afficheErreurEtResteSurFormulaire() {
        loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        RoomFormPage formPage = new RoomFormPage(driver);
        formPage.openCreateForm(baseUrl);
        formPage.fillAndSubmit("Salle 1", "", null, null);

        assertThat(driver.getCurrentUrl()).isEqualTo(baseUrl + "/rooms/new");
        assertThat(formPage.hasErrorMessage()).isTrue();

    }

    @Test
    void accesRooms_avecClientConnecte_estRefuse() {
        // les clients n'ont pas le droit de créer une salle
        loginAs(CLIENT_EMAIL, CLIENT_PASSWORD);

        driver.get(baseUrl + "/rooms");

        assertThat(driver.getPageSource().toLowerCase()).contains("403");
    }
}
