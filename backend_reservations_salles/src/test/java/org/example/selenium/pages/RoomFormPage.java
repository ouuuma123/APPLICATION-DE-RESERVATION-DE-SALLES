package org.example.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class RoomFormPage {
    private final WebDriver driver;

    private final By nameField = By.id("name");
    private final By capacityField = By.id("capacity");
    private final By locationField = By.id("location");
    private final By equipmentField = By.id("equipment");
    private final By submitButton = By.id("submit-btn");
    private final By errorMessage = By.id("error-mssg");

    public RoomFormPage(WebDriver driver) {
        this.driver = driver;
    }

    public void openCreateForm(String baseUrl) {
        driver.get(baseUrl + "/rooms/new");
    }

    public void fillAndSubmit(String name, String capacity, String location, String equipment) {
        driver.findElement(nameField).sendKeys(name);
        driver.findElement(capacityField).sendKeys(capacity);
        if(location != null)
            driver.findElement(locationField).sendKeys(location);
        if (equipment != null)
            driver.findElement(equipmentField).sendKeys(equipment);
        driver.findElement(submitButton).click();
    }

    public boolean hasErrorMessage() {
        return !driver.findElements(errorMessage).isEmpty();
    }
}
