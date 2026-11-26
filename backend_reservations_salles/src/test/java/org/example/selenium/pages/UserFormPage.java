package org.example.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class UserFormPage {

    private final WebDriver driver;

    private final By nameField = By.id("name");
    private final By emailField = By.id("email");
    private final By departmentField = By.id("department");
    private final By submitButton = By.id("submit-btn");
    private final By errorMessage = By.id("error-mssg");

    public UserFormPage(WebDriver driver) {
        this.driver = driver;
    }

    public void openCreateForm(String baseUrl) {
        driver.get(baseUrl + "/users/new");
    }

    public void fillANdSubmit(String name, String email, String department) {
        driver.findElement(nameField).sendKeys(name);
        driver.findElement(emailField).sendKeys(email);
        if (department != null)
            driver.findElement(departmentField).sendKeys(department);
        driver.findElement(submitButton).click();
    }

    public boolean hasErrorMessage() {
        return !driver.findElements(errorMessage).isEmpty();
    }
}
