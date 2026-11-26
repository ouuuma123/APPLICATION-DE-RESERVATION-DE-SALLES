package org.example.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RoomListPage {

    private final WebDriver driver;

    public RoomListPage(WebDriver driver) {
        this.driver = driver;
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/rooms");
    }

    public List<WebElement> getRoomLinks() {
        return driver.findElements(By.cssSelector("a[href^='/rooms/']"));
    }

    public boolean containsRoomNamed(String roomName) {
        return driver.getPageSource().contains(roomName);
    }
}
