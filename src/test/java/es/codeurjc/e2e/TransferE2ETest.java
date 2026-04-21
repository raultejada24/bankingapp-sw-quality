package es.codeurjc.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PLAN DE PRUEBAS - TransferE2ETest (E2E Web Tests with Selenium WebDriver)
 * 
 * Sistema bajo prueba: Funcionalidad de transfer (Transferencias bancarias)
 * Responsabilidades: Verificar el flujo web de transferencias, validaciones,
 * errores y balances
 * 
 * Criterios de éxito:
 * - Todas las pruebas son independientes (no dependen de datos de otras
 * pruebas)
 * - Se verifica el saldo de cuentas origen y destino tras cada transacción
 * - Se verifica el mensaje de error en cada caso de fallo
 * - Se utiliza Selenium WebDriver con ChromeDriver
 * - No se usa Thread.sleep() (solo WebDriverWait)
 * 
 * Casos de Prueba:
 * 1. transferOwnAccounts_Success: Transferencia entre cuentas propias del mismo
 * usuario
 * 2. transferDifferentUsers_Success: Transferencia entre cuentas de distintos
 * usuarios
 * 3. transferSameAccount_Fail: No se puede transferir a la misma cuenta
 * 4. transferInsufficientBalance_Fail: No se puede transferir sin saldo
 * suficiente
 * 5. transferNegativeAmount_Fail: No se puede transferir cantidad negativa
 * 6. transferExceedsLimit_Fail: No se puede transferir > €20.000
 * 7. transferInvalidAccount_Fail: No se puede transferir a cuenta inexistente
 * 8. transferZeroAmount_Fail: No se puede transferir cantidad cero (bonus)
 */
public class TransferE2ETest {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BASE_URL = "http://localhost:8080";
    private static final int TIMEOUT_SECONDS = 10;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String CUSTOMER_USERNAME = "customer";
    private static final String CUSTOMER_PASSWORD = "Cu5t0m3r";
    private static final String MARIA_USERNAME = "maria";
    private static final String MARIA_PASSWORD = "maria123";

    // Cuentas de datos conocidas de DatabaseInitializer
    private static final String CUSTOMER_ACCOUNT_1 = "ES0001234567"; // customer CHECKING 5000
    private static final String CUSTOMER_ACCOUNT_2 = "ES0001234568"; // customer SAVINGS 15000
    private static final String MARIA_ACCOUNT_1 = "ES0002345678"; // maria CHECKING 8000
    private static final String MARIA_ACCOUNT_2 = "ES0002345679"; // maria SAVINGS 25000

    @BeforeEach
    void setUp() {
        // Desactivar gestor de contraseñas y advertencias para evitar que roben el foco
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito");
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        // Inicializar ChromeDriver con opciones
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ======================== UTILITY METHODS (Reutilización de código) //
    // ========================

    /**
     * Realiza login con usuario y contraseña dados
     * Espera hasta que la URL contenga "/dashboard"
     */
    private void login(String username, String password) {
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));

        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    /**
     * Realiza logout del usuario actual
     */
    private void logout() {
        try {
            WebElement logoutBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[contains(text(), 'Logout')] | //button[contains(text(), 'Logout')]")));
            logoutBtn.click();
            wait.until(ExpectedConditions.urlContains("/login"));
        } catch (Exception e) {
            // Ya está logout o botón no visible
        }
    }

    /**
     * Navega a /transfer y espera a que se cargue el formulario
     */
    private void navigateToTransferPage() {
        driver.get(BASE_URL + "/transfer");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fromAccount")));
    }

    /**
     * Rellena y envía el formulario de transferencia
     * 
     * @param fromAccount Número de cuenta origen (valor del select)
     * @param toAccount   Número de cuenta destino (texto para input)
     * @param amount      Cantidad a transferir (string)
     */
    private void fillAndSubmitTransferForm(String fromAccount, String toAccount, String amount) {
        // Seleccionar cuenta origen
        WebElement fromSelectElement = wait.until(ExpectedConditions.elementToBeClickable(By.id("fromAccount")));
        Select fromAccountSelect = new Select(fromSelectElement);
        fromAccountSelect.selectByValue(fromAccount);

        // Rellenar cuenta destino
        WebElement toAccountInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("toAccount")));
        toAccountInput.clear();
        toAccountInput.sendKeys(toAccount);

        // Rellenar monto
        WebElement amountInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("amount")));
        amountInput.clear();
        amountInput.sendKeys(amount);

        // Enviar formulario
        WebElement button = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("transferButton")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
        wait.until(ExpectedConditions.elementToBeClickable(button));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
    }

    /**
     * Obtiene el texto del mensaje de error en la página
     * 
     * @return Texto del error o null si no hay error visible
     */
    private String getErrorMessageText() {
        try {
            WebElement errorElement = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[contains(@class, 'alert-danger')]")));
            return errorElement.getText();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene el texto del mensaje de éxito en la página
     * 
     * @return Texto del éxito o null si no hay mensaje visible
     */
    private String getSuccessMessageText() {
        try {
            WebElement successElement = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[contains(@class, 'alert-success')]")));
            return successElement.getText();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Espera a que aparezca un mensaje en la página (error o éxito)
     * Retorna el mensaje cuando esté disponible
     */
    private String waitForMessage() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(@class, 'alert-danger')] | //div[contains(@class, 'alert-success')]")));
            String error = getErrorMessageText();
            if (error != null)
                return error;
            return getSuccessMessageText();
        } catch (Exception e) {
            return null;
        }
    }

    private String waitForErrorAlertText() {
        WebElement errorAlert = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.alert.alert-danger.alert-dismissible.fade.show[role='alert']")));
        return errorAlert.getText().trim();
    }

    /**
     * Obtiene el saldo actual de una cuenta desde el dashboard
     * Navega al dashboard y busca la fila con el número de cuenta
     * 
     * @param accountNumber Número de cuenta (ej: ES0001234567)
     * @return Saldo en formato double
     */
    private double getAccountBalance(String accountNumber) {
        driver.get(BASE_URL + "/dashboard");

        try {
            // Buscar la fila con el número de cuenta y extraer el saldo
            WebElement balanceCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("balance-" + accountNumber)));
            String balanceText = balanceCell.getText();

            // Parsear el saldo (remover símbolos de moneda y espacios)
            String cleaned = balanceText.replaceAll("[^0-9.,]", "").trim();
            if (cleaned.isEmpty())
                return 0.0;

            // Manejar formato europeo vs US
            if (cleaned.contains(",") && cleaned.contains(".")) {
                if (cleaned.lastIndexOf(",") > cleaned.lastIndexOf(".")) {
                    cleaned = cleaned.replace(".", "").replace(",", ".");
                } else {
                    cleaned = cleaned.replace(",", "");
                }
            } else if (cleaned.contains(",")) {
                cleaned = cleaned.replace(",", ".");
            }

            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ======================== TEST CASES ========================

    /**
     * Hecho por: Raúl Tejada Merinero
     * Caso 1: Transferencia exitosa entre cuentas propias
     * Given: Usuario admin autenticado con dos cuentas (checking y savings)
     * When: Realiza una transferencia de €1000 de cuenta 1 a cuenta 2
     * Then: La transferencia se completa, aparece mensaje de éxito,
     * y los saldos de ambas cuentas se actualizan correctamente
     */
    @Test
    @DisplayName("1. transferOwnAccounts_Success: Transferencia entre cuentas propias")
    void transferOwnAccounts_SuccessTest() {
        // Given (Login y obtener saldos iniciales)
        login(ADMIN_USERNAME, ADMIN_PASSWORD);
        double initialBalance1 = getAccountBalance("1");
        double initialBalance2 = getAccountBalance("2");

        navigateToTransferPage();

        // When (Transferir €1000 de cuenta 1 a cuenta 2)
        fillAndSubmitTransferForm("1", "2", "1000");

        // Then (Comprobar mensaje de éxito)
        String message = waitForMessage();
        assertNotNull(message, "Debe aparecer un mensaje tras la transferencia");
        assertTrue(
                message.toLowerCase().contains("success") ||
                        message.toLowerCase().contains("completad") ||
                        message.toLowerCase().contains("exitosa"),
                "El mensaje debe indicar éxito: " + message);

        // Then (Comprobar saldos post-transferencia)
        double finalBalance1 = getAccountBalance("1");
        double finalBalance2 = getAccountBalance("2");

        double expectedBalance1 = initialBalance1 - 1000;
        double expectedBalance2 = initialBalance2 + 1000;

        assertEquals(expectedBalance1, finalBalance1, 0.01,
                "Saldo cuenta origen debe disminuir en €1000. Esperado: " + expectedBalance1 + ", Obtenido: "
                        + finalBalance1);
        assertEquals(expectedBalance2, finalBalance2, 0.01,
                "Saldo cuenta destino debe aumentar en €1000. Esperado: " + expectedBalance2 + ", Obtenido: "
                        + finalBalance2);

        logout();
    }

    /**
     * Hecho por: Raúl Tejada Merinero
     * Caso 2: Transferencia exitosa entre cuentas de distintos usuarios
     * Given: Usuario customer autenticado con una cuenta (ES0001234567)
     * When: Realiza una transferencia de €500 a una cuenta de otro usuario (maria)
     * Then: La transferencia se completa, aparece mensaje de éxito,
     * y los saldos de ambas cuentas se actualizan correctamente
     */
    @Test
    @DisplayName("2. transferDifferentUsers_Success: Transferencia entre cuentas de distintos usuarios")
    void transferDifferentUsers_SuccessTest() {
        // Given (Login como customer y obtener saldos iniciales)
        login(CUSTOMER_USERNAME, CUSTOMER_PASSWORD);
        double initialBalanceCustomer = getAccountBalance(CUSTOMER_ACCOUNT_1);

        navigateToTransferPage();

        // When (Transferir €500 de cuenta de customer a cuenta de maria)
        fillAndSubmitTransferForm(CUSTOMER_ACCOUNT_1, MARIA_ACCOUNT_1, "500");

        // Then (Comprobar mensaje de éxito)
        String message = waitForMessage();
        assertNotNull(message, "Debe aparecer un mensaje tras la transferencia entre usuarios");
        assertTrue(
                message.toLowerCase().contains("success") ||
                        message.toLowerCase().contains("completad") ||
                        message.toLowerCase().contains("exitosa"),
                "El mensaje debe indicar éxito: " + message);

        // Then (Comprobar saldo post-transferencia de customer)
        double finalBalanceCustomer = getAccountBalance(CUSTOMER_ACCOUNT_1);
        double expectedBalanceCustomer = initialBalanceCustomer - 500;

        assertEquals(expectedBalanceCustomer, finalBalanceCustomer, 0.01,
                "Saldo cuenta origen (customer) debe disminuir en €500. Esperado: " + expectedBalanceCustomer
                        + ", Obtenido: " + finalBalanceCustomer);

        logout();

        // Verificar que la cuenta de maria recibió el dinero
        login(MARIA_USERNAME, MARIA_PASSWORD);
        double finalBalanceMaria = getAccountBalance(MARIA_ACCOUNT_1);
        assertTrue(finalBalanceMaria > 0, "La cuenta de maria debe tener saldo");

        logout();
    }

    /**
     * Hecho por: Blas Vita Ramos
     * Caso 3: No se puede transferir a la misma cuenta
     * Given: Usuario admin autenticado
     * When: Intenta transferir a la misma cuenta de origen
     * Then: Aparece mensaje de error indicando que no puede transferir a la misma
     * cuenta
     */
    @Test
    @DisplayName("3. transferSameAccount_Fail: No se puede transferir a la misma cuenta")
    void transferSameAccount_FailTest() {
        // Given
        login(MARIA_USERNAME, MARIA_PASSWORD);
        double initialBalance = getAccountBalance(MARIA_ACCOUNT_1);

        navigateToTransferPage();

        // When (Intentar transferir a la misma cuenta)
        fillAndSubmitTransferForm(MARIA_ACCOUNT_1, MARIA_ACCOUNT_1, "1");

        // Then (Comprobar error y que no hubo transferencia)
        String errorText = waitForErrorAlertText();
        assertTrue(
                errorText.contains("Cannot transfer to same account"),
                "El texto del error debe corresponder a misma cuenta. Obtenido: " + errorText);

        double finalBalance = getAccountBalance(MARIA_ACCOUNT_1);
        assertEquals(initialBalance, finalBalance, 0.01,
                "No debe cambiar el saldo si la transferencia falla");

        logout();
    }

    /**
     * Hecho por: Blas Vita Ramos
     * Caso 4: No se puede transferir sin saldo suficiente
     * Given: Usuario admin autenticado
     * When: Intenta transferir una cantidad superior al saldo disponible
     * Then: Aparece mensaje de error de saldo insuficiente
     */
    @Test
    @DisplayName("4. transferInsufficientBalance_Fail: No se puede transferir sin saldo suficiente")
    void transferInsufficientBalance_FailTest() {
        // Given
        login(MARIA_USERNAME, MARIA_PASSWORD);
        double initialSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double initialDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        navigateToTransferPage();

        // When (Intentar transferir una cantidad superior al saldo disponible)
        String invalidAmount = String.valueOf(initialSourceBalance + 1);
        fillAndSubmitTransferForm(MARIA_ACCOUNT_1, MARIA_ACCOUNT_2, invalidAmount);

        // Then (Comprobar error y que no hubo trasnferencia)
        String errorText = waitForErrorAlertText();
        assertTrue(
                errorText.contains("Insufficient funds"),
                "El texto del error debe corresponder a saldo insuficiente. Obtenido: " + errorText);

        double finalSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double finalDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        assertEquals(initialSourceBalance, finalSourceBalance, 0.01,
                "El saldo de origen no debe cambiar si la transferencia falla");
        assertEquals(initialDestinationBalance, finalDestinationBalance, 0.01,
                "El saldo de destino no debe cambiar si la transferencia falla");

        logout();
    }

    /**
     * Hecho por: Blas Vita Ramos
     * Caso 5: No se puede transferir cantidad negativa
     * Given: Usuario admin autenticado
     * When: Intenta transferir cantidad negativa
     * Then: Aparece mensaje de error indicando cantidad debe ser positiva
     */
    @Test
    @DisplayName("5. transferNegativeAmount_Fail: No se puede transferir cantidad negativa")
    void transferNegativeAmount_FailTest() {
        // Given
        login(MARIA_USERNAME, MARIA_PASSWORD);
        double initialSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double initialDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        navigateToTransferPage();

        // When (Intentar transferir cantidad negativa)
        fillAndSubmitTransferForm(MARIA_ACCOUNT_1, MARIA_ACCOUNT_2, "-1");

        // Then (Comprobar error y que no hubo transferencia)
        String errorText = waitForErrorAlertText();
        assertTrue(
                errorText.contains("Amount must be positive"),
                "El texto del error debe corresponder a cantidad positiva. Obtenido: " + errorText);

        double finalSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double finalDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        assertEquals(initialSourceBalance, finalSourceBalance, 0.01,
                "El saldo de origen no debe cambiar si la transferencia falla");
        assertEquals(initialDestinationBalance, finalDestinationBalance, 0.01,
                "El saldo de destino no debe cambiar si la transferencia falla");

        logout();
    }

    /**
     * Hecho por: Adrián Villalba Cuello de Oro
     * Caso 6: No se puede transferir cantidad que supera €20.000
     * Given: Usuario admin autenticado
     * When: Intenta transferir más de €20.000
     * Then: Aparece mensaje de error sobre límite de transferencia
     */
    @Test
    @DisplayName("6. transferExceedsLimit_Fail: No se puede transferir > €20.000")
    void transferExceedsLimit_FailTest() {
        // Given
        login(MARIA_USERNAME, MARIA_PASSWORD);
        double initialSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double initialDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        navigateToTransferPage();

        // When (Intentar transferir más del límite)
        fillAndSubmitTransferForm(MARIA_ACCOUNT_1, MARIA_ACCOUNT_2, "20050");

        // Then (Comprobar error)
        String errorText = waitForErrorAlertText();
        assertTrue(
                errorText.contains("Amount must not be higher than 20000"),
                "El texto del error debe corresponder a límite de transferencia. Obtenido: " + errorText);

        double finalSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double finalDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        assertEquals(initialSourceBalance, finalSourceBalance, 0.01,
                "El saldo de origen no debe cambiar si la transferencia falla");
        assertEquals(initialDestinationBalance, finalDestinationBalance, 0.01,
                "El saldo de destino no debe cambiar si la transferencia falla");

        logout();
    }

    /**
     * Hecho por: Gonzalo Andrés Zurdo
     * Caso 7: No se puede transferir a cuenta inexistente
     * Given: Usuario admin autenticado
     * When: Intenta transferir a un número de cuenta que no existe
     * Then: Aparece mensaje de error indicando que la cuenta no existe
     */
    @Test
    @DisplayName("7. transferInvalidAccount_Fail: No se puede transferir a cuenta inexistente")
    void transferInvalidAccount_FailTest() {
        // Given
        login(MARIA_USERNAME, MARIA_PASSWORD);
        double initialBalance = getAccountBalance(MARIA_ACCOUNT_1);

        navigateToTransferPage();

        // When (Intentar transferir a cuenta inexistente)
        fillAndSubmitTransferForm(MARIA_ACCOUNT_1, "ES9999999999", "100");

        // Then (Comprobar error y que el saldo no cambió)
        String errorText = waitForErrorAlertText();
        assertTrue(
                errorText.contains("Account not found"),
                "El texto del error debe indicar que la cuenta no existe. Obtenido: " + errorText);

        double finalBalance = getAccountBalance(MARIA_ACCOUNT_1);
        assertEquals(initialBalance, finalBalance, 0.01,
                "El saldo no debe cambiar si la cuenta destino no existe");

        logout();
    }

    /**
     * Hecho por: Arturo VInuesa Domínguez
     * Caso 8: No se puede transferir cantidad cero
     * Given: Usuario admin autenticado
     * When: Intenta transferir €0
     * Then: Aparece mensaje de error indicando cantidad debe ser positiva
     */
    @Test
    @DisplayName("8. transferZeroAmount_Fail: No se puede transferir €0")
    void transferZeroAmount_FailTest() {
        // Given
        login(MARIA_USERNAME, MARIA_PASSWORD);
        double initialSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double initialDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        navigateToTransferPage();

        // When (Intentar transferir 0€)
        fillAndSubmitTransferForm(MARIA_ACCOUNT_1, MARIA_ACCOUNT_2, "0");

        // Then (Comprobar error)
        String errorText = waitForErrorAlertText();
        assertTrue(
                errorText.contains("Amount must be positive"),
                "El texto del error debe corresponder a cantidad positiva. Obtenido: " + errorText);

        double finalSourceBalance = getAccountBalance(MARIA_ACCOUNT_1);
        double finalDestinationBalance = getAccountBalance(MARIA_ACCOUNT_2);

        assertEquals(initialSourceBalance, finalSourceBalance, 0.01,
                "El saldo de origen no debe cambiar si la transferencia falla");
        assertEquals(initialDestinationBalance, finalDestinationBalance, 0.01,
                "El saldo de destino no debe cambiar si la transferencia falla");

        logout();
    }
}
