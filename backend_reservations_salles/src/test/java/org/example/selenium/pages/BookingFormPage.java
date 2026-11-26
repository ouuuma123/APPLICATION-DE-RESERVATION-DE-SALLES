package org.example.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

public class BookingFormPage {

    private final WebDriver driver;

    private final By titleField = By.id("title");
    private final By roomSelect = By.id("room");
    private final By userSelect = By.id("user");
    private final By startTimeField = By.id("startTime");
    private final By endTimeField = By.id("endTime");
    private final By attendeesField = By.id("attendeesCount");
    private final By submitButton = By.id("submit-btn");

    public BookingFormPage(WebDriver driver) {
        this.driver = driver;
    }

    public void openCreateForm(String baseUrl) {
        driver.get(baseUrl + "/bookings/new");
    }

    public void fillAndSubmit(String title, String roomName, String userName, String startTime, String endTime, String attendees) {
        driver.findElement(titleField).sendKeys(title);

        new Select(driver.findElement(roomSelect)).selectByVisibleText(roomName);
        new Select(driver.findElement(userSelect)).selectByVisibleText(userName);

        driver.findElement(startTimeField).sendKeys(startTime);
        driver.findElement(endTimeField).sendKeys(endTime);
        if (attendees != null)
            driver.findElement(attendeesField).sendKeys(attendees);
        driver.findElement(submitButton).click();
    }
}
