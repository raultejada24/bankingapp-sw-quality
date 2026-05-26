package es.codeurjc.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import es.codeurjc.BankingApplication;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BankingApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SmokeTest {

    @LocalServerPort
    int port;

    private WebDriver driver;
    private WebDriverWait wait;
    private static final int TIMEOUT_SECONDS = 10;

    @BeforeEach
    void setUp() {
        // Lee el navegador (por defecto chrome)
        String browser = System.getProperty("browser", "chrome").toLowerCase();

        switch (browser) {
            case "firefox":
                org.openqa.selenium.firefox.FirefoxOptions firefoxOptions = new org.openqa.selenium.firefox.FirefoxOptions();
                firefoxOptions.addArguments("-headless");
                driver = new org.openqa.selenium.firefox.FirefoxDriver(firefoxOptions);
                break;
            case "edge":
                org.openqa.selenium.edge.EdgeOptions edgeOptions = new org.openqa.selenium.edge.EdgeOptions();
                edgeOptions.addArguments("--headless");
                driver = new org.openqa.selenium.edge.EdgeDriver(edgeOptions);
                break;
            case "safari":
                driver = new org.openqa.selenium.safari.SafariDriver();
                break;
            case "chrome":
            default:
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--incognito");
                chromeOptions.addArguments("--headless"); // OBLIGATORIO para GitHub Actions
                
                Map<String, Object> prefs = new HashMap<>();
                prefs.put("credentials_enable_service", false);
                prefs.put("profile.password_manager_enabled", false);
                chromeOptions.setExperimentalOption("prefs", prefs);

                driver = new ChromeDriver(chromeOptions);
                break;
        }

        driver.manage().deleteAllCookies();
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String getBaseUrl() {
        String appUrl = System.getProperty("app.url"); 
        if (appUrl != null && !appUrl.isEmpty()) {
            return appUrl; // Si el workflow le pasa la URL de Azure, usa la de Azure
        }
        return "http://localhost:" + port; // Si no le pasa nada, usa el puerto aleatorio local
    }

    @Test
    @DisplayName("SmokeTest: Comprobar que el número de versión (1.X.X) se muestra en el login")
    void checkVersionOnLoginTest() {

        driver.get(getBaseUrl() + "/login");

        // 1. Esperamos a que el body de la página cargue
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        // 2. Obtenemos todo el texto visible de la página web
        String pageText = driver.findElement(By.tagName("body")).getText();

        // 3. Expresión regular: Busca la letra 'v' (opcional) seguida de un '1.', un número, un '.', y otro número.
        // Ejemplos que detectará: 1.0.0, 1.2.0, 1.3.1, v1.5.0
        Pattern versionPattern = Pattern.compile("v?1\\.\\d+\\.\\d+");
        Matcher matcher = versionPattern.matcher(pageText);

        // 4. Verificamos que encuentra el patrón en el texto o que pone "DEV"
        assertTrue(matcher.find() || pageText.contains("DEV"), 
            "¡Fallo crítico! No se encontró ninguna versión con formato 1.X.X o DEV en la página de login.");
    }
}