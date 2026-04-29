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
    @DisplayName("SmokeTest: Comprobar que el número de versión se muestra en el login")
    void checkVersionOnLoginTest() {

        driver.get(getBaseUrl() + "/login");

        // 2. Esperamos a que el body de la página cargue
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        // 3. Obtenemos todo el texto visible de la página web
        String pageText = driver.findElement(By.tagName("body")).getText();

        // 4. Verificamos que la versión "1.0.0" aparece en algún lugar del texto
        // Nota: Si el HTML de vuestra app tiene un ID específico para la versión (ej: id="app-version"),
        // podrías buscar ese elemento concreto, pero buscar en el texto de la página es la forma más a prueba de fallos.
        assertTrue(pageText.contains("1.0.0") || pageText.contains("v1.0.0") || pageText.contains("DEV"), 
            "¡Fallo crítico! No se encontró la versión esperada (1.0.0 o DEV) en la página de login.");
    }
}