package org.TestifyFW.framework;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.TestifyFW.reporter.TestListener;

import io.github.bonigarcia.wdm.WebDriverManager;

@Listeners(org.TestifyFW.reporter.TestListener.class)
public class BaseTest {
    public WebDriver driver;
    private String browserType = "chrome"; // Default browser

    @BeforeClass
    public void setup() {
        initializeDriver(browserType);
    }

    public void setBrowserType(String browser) {
        this.browserType = browser;
    }

    public void initializeDriver(String browser) {
        switch (browser.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--remote-allow-origins=*");
                chromeOptions.addArguments("--disable-blink-features=AutomationControlled"); // Prevent detection
                chromeOptions.addArguments("--start-maximized"); // Open in full-screen
                chromeOptions.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.134 Safari/537.36");
                driver = new ChromeDriver(chromeOptions);
                break;

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;

            default:
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver();
                break;
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit(); // Closes browser after tests
        }
    }
}