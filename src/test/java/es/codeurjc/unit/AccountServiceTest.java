package es.codeurjc.unit;

import es.codeurjc.model.Account;
import es.codeurjc.model.Notification;
import es.codeurjc.model.Transaction;
import es.codeurjc.model.User;
import es.codeurjc.repository.AccountRepository;
import es.codeurjc.repository.TransactionRepository;
import es.codeurjc.service.AccountService;
import es.codeurjc.service.RandomService;
import es.codeurjc.service.notifications.EmailNotificationService;
import es.codeurjc.service.notifications.SmsNotificationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/* PLAN DE PRUEBAS - AccountService
 * --- Métodos Básicos ---
 * 1. createAccount: Prueba que se genera un número, se asigna el usuario y se guarda.
 * 2. getAccount_Success: Prueba que devuelve la cuenta si existe.
 * 3. getAccount_NotFound: Prueba que lanza IllegalArgumentException si no existe.
 * 4. getUserAccounts: Prueba que devuelve la lista de cuentas del usuario.
 * 5. getBalance: Prueba que devuelve el saldo correcto.
 * 6. getTransactions: Prueba que devuelve la lista de transacciones ordenada.
 * 7. rm_Success: Prueba que elimina la cuenta si el saldo es 0.
 * 8. rm_HasBalance: Prueba que lanza IllegalArgumentException si el saldo != 0.
 * --- Deposit (Con descripción) ---
 * 9. deposit_ZeroAmount: Lanza excepción si amount == 0.
 * 10. deposit_NegativeAmount: Lanza excepción si amount < 0.
 * 11. deposit_Exceeds10k: Lanza excepción si amount > 10000.
 * 12. deposit_Exceeds50k: [INACCESSIBLE] Documentado. Nunca se llega aquí porque salta en > 10000.
 * 13. deposit_Success_Email: Ingreso válido y notificación por EMAIL.
 * 14. deposit_Success_Sms: Ingreso válido y notificación por SMS.
 * 15. deposit_Success_NoNotif: Ingreso válido donde no entra ni en EMAIL ni en SMS.
 * --- Quick Deposit (Sin descripción) ---
 * 16. quickDeposit_ZeroAmount: Lanza excepción si amount == 0.
 * 17. quickDeposit_NegativeAmount: Lanza excepción si amount < 0.
 * 18. quickDeposit_Exceeds10k: Lanza excepción si amount > 10000.
 * 19. quickDeposit_Exceeds50k: [INACCESSIBLE] Documentado. Mismo caso que el anterior.
 * 20. quickDeposit_Success_Email: Ingreso válido y notificación por EMAIL.
 * 21. quickDeposit_Success_Sms: Ingreso válido y notificación por SMS.
 * 22. quickDeposit_Success_NoNotif: Ingreso válido sin notificación.
 * --- Withdraw ---
 * 23. withdraw_NegativeOrZero: Lanza excepción si amount <= 0.
 * 24. withdraw_Exceeds5k: Lanza excepción si amount > 5000.
 * 25. withdraw_InsufficientFunds: Lanza excepción si saldo < amount.
 * 26. withdraw_Success_Email: Retiro válido y notificación EMAIL.
 * 27. withdraw_Success_Sms: Retiro válido y notificación SMS.
 * 28. withdraw_Success_NoNotif: Retiro válido sin notificación.
 * --- Transfer ---
 * 29. transfer_NegativeOrZero: Lanza excepción si amount <= 0.
 * 30. transfer_Exceeds20k: Lanza excepción si amount > 20000.
 * 31. transfer_SameAccount: Lanza excepción si origen y destino son la misma cuenta (Referencia ==).
 * 32. transfer_InsufficientFunds: Lanza excepción si saldo origen < amount.
 * 33. transfer_Success_Emails: Transferencia válida. Remitente EMAIL, Destinatario EMAIL.
 * 34. transfer_Success_Sms: Transferencia válida. Remitente SMS, Destinatario SMS.
 * 35. transfer_Success_NoNotifs: Transferencia válida sin notificaciones configuradas.
 */

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    // ==========================================
    // MOCKS DE DEPENDENCIAS EXTERNAS
    // ==========================================
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private EmailNotificationService emailService;
    @Mock
    private SmsNotificationService smsService;
    @Mock
    private RandomService randomService;

    // INYECCIÓN DE MOCKS EN EL SERVICIO A PROBAR
    @InjectMocks
    private AccountService accountService;


    // CRUD y CONSULTAS

    // Hecho por: Adrián Villalba Cuello de Oro
    @Test
    @DisplayName("1. createAccount: Prueba que genera un número, se asigna al usuario y se guarda")
    void createAccountTest() {
        // Given (Preparar datos del User, configurar el mock de randomService y accountRepository.save)
        User mockUser = new User("Ana", "Pass123", "USER");
        when(randomService.nextInt(anyInt())).thenReturn(123456);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Devuelve la cuenta que se le dio

        // When (Llamar a accountService.createAccount)
        Account newAccount = accountService.createAccount(mockUser, Account.AccountType.CHECKING);

        // Then (Hacer asserts para comprobar la cuenta devuelta y usar verify para ver que se guardó)
        assertNotNull(newAccount);
        assertEquals("ES0000123456", newAccount.getAccountNumber());
        assertEquals(mockUser, newAccount.getUser());
        assertEquals(0.0, newAccount.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    // Hecho por: Raúl Tejada Merinero
    @Test
    @DisplayName("2. getAccount_Success: Prueba que devuelve la cuenta si existe")
    void getAccount_SuccessTest() {
        // Given (Configurar accountRepository.findByAccountNumber para que devuelva un Optional con cuenta)
        Account mockAccount = new Account("ES12345", Account.AccountType.SAVINGS, 100);
        when(accountRepository.findByAccountNumber("ES12345")).thenReturn(Optional.of(mockAccount));

        // When (Llamar a accountService.getAccount)
        Account result = accountService.getAccount("ES12345");

        // Then (Comprobar con assertNotNull o assertEquals que devuelve la cuenta correcta)
        assertNotNull(result);
        assertEquals("ES12345", result.getAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber("ES12345");
    }

    // Hecho por: Raúl Tejada Merinero
    @Test
    @DisplayName("3. getAccount_NotFound: Prueba que lanza IllegalArgumentException si no existe")
    void getAccount_NotFoundTest() {
        // Given (Configurar accountRepository.findByAccountNumber para que devuelva Optional.empty())
        when(accountRepository.findByAccountNumber("ES99999")).thenReturn(Optional.empty());

        // When (Llamar a accountService.getAccount usando assertThrows para capturar la excepción)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.getAccount("ES99999");
        });
        
        // Then (Comprobar que el mensaje de la excepción es "Account not found")
        assertEquals("Account not found", exception.getMessage());
    }

    // Hecho por: Adrián Villalba Cuello de Oro
    @Test
    @DisplayName("4. getUserAccounts: Prueba que devuelve la lista de cuentas del usuario")
    void getUserAccountsTest() {
        // Given (Preparar un User y configurar accountRepository.findByUser para devolver una lista)
        User mockUser = new User("Ana", "Pass123", "USER");
        List <Account> listAccounts = List.of(new Account(), new Account());
        when (accountRepository.findByUser(mockUser)).thenReturn(listAccounts);

        // When (Llamar a accountService.getUserAccounts)
        List <Account> myAccounts = accountService.getUserAccounts(mockUser);

        // Then (Comprobar que la lista devuelta no está vacía y tiene el tamaño esperado)
        assertNotNull(myAccounts);
        assertEquals(2, myAccounts.size());
        verify(accountRepository, times(1)).findByUser(mockUser);

    }

    // Hecho por: Adrián Villalba Cuello de Oro
    @Test
    @DisplayName("5. getBalance: Prueba que devuelve el saldo correcto")
    void getBalanceTest() {
        // Given (Configurar accountRepository.findByAccountNumber para devolver una cuenta con saldo X)
        Account mockAccount = new Account();
        mockAccount.setAccountNumber("ES0000123456");
        mockAccount.setBalance(500.0);
        when(accountRepository.findByAccountNumber("ES0000123456")).thenReturn(Optional.of(mockAccount));

        // When (Llamar a accountService.getBalance)
        double myBalance = accountService.getBalance("ES0000123456");

        // Then (Comprobar con assertEquals que el saldo devuelto es X)
        assertNotNull(myBalance);
        assertEquals(500.0, myBalance, 0.001);
        verify(accountRepository, times(1)).findByAccountNumber("ES0000123456");

    }

    // Hecho por: Gonzalo
    @Test
    @DisplayName("6. getTransactions: Prueba que devuelve la lista de transacciones ordenada")
    void getTransactionsTest() {
        // Given (Preparar cuenta y configurar transactionRepository.findByAccountOrderByTimestampDesc)
        Account account = new Account("ES12345", Account.AccountType.SAVINGS, 100);
        when(accountRepository.findByAccountNumber("ES12345")).thenReturn(Optional.of(account));
        Transaction t1 = new Transaction(account, Transaction.TransactionType.DEPOSIT, 100, "Deposit");
        java.util.List<Transaction> transactions = java.util.List.of(t1);
        when(transactionRepository.findByAccountOrderByTimestampDesc(account)).thenReturn(transactions);

        // When (Llamar a accountService.getTransactions)
        java.util.List<Transaction> result = accountService.getTransactions("ES12345");

        // Then (Verificar que devuelve la lista de transacciones esperada)
        assertEquals(transactions, result);
        verify(accountRepository, times(1)).findByAccountNumber("ES12345");
        verify(transactionRepository, times(1)).findByAccountOrderByTimestampDesc(account);
    }

    // Hecho por: Gonzalo
    @Test
    @DisplayName("7. rm_Success: Prueba que elimina la cuenta si el saldo es 0")
    void rm_SuccessTest() {
        // Given (Configurar cuenta con saldo 0 y accountRepository.findByAccountNumber)
        Account account = new Account("ES12345", Account.AccountType.SAVINGS, 0);
        when(accountRepository.findByAccountNumber("ES12345")).thenReturn(Optional.of(account));

        // When (Llamar a accountService.rm)
        accountService.rm("ES12345");

        // Then (Verificar con verify que se llamó a accountRepository.delete)
        verify(accountRepository, times(1)).findByAccountNumber("ES12345");
        verify(accountRepository, times(1)).delete(account);
    }

    // Hecho por: Adrián Villalba Cuello de Oro
    @Test
    @DisplayName("8. rm_HasBalance: Prueba que lanza IllegalArgumentException si el saldo != 0")
    void rm_HasBalanceTest() {
        // Given (Configurar cuenta con saldo mayor a 0 y accountRepository.findByAccountNumber)
        Account mockAccount = new Account();
        mockAccount.setAccountNumber("ES0000123456");
        mockAccount.setBalance(500.0);
        when(accountRepository.findByAccountNumber("ES0000123456")).thenReturn(Optional.of(mockAccount));

        // When (Llamar a accountService.rm usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.rm("ES0000123456");
        });

        // Then (Comprobar el mensaje "Cannot delete account with non-zero balance")
        assertEquals("Cannot delete account with non-zero balance", exception.getMessage());

    }


    // FALLOS DEPOSIT

    // Hecho por: Adrián Villalba Cuello de Oro
    @Test
    @DisplayName("9. deposit_ZeroAmount: Lanza excepción si amount == 0")
    void deposit_ZeroAmountTest() {
        // Given (No hacen falta mocks aquí, solo preparar variables: amount = 0)

        // When (Llamar a accountService.deposit usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES0000123456", 0.0, "Ingreso de prueba");
        });

        // Then (Comprobar el mensaje de excepción correspondiente)
        assertEquals("Amount must be positive", exception.getMessage());

    }

    // Hecho por: Raúl Tejada Merinero
    @Test
    @DisplayName("10. deposit_NegativeAmount: Lanza excepción si amount < 0")
    void deposit_NegativeAmountTest() {
        // Given (No hacen falta mocks, preparar variables: amount = -50)

        // When (Llamar a accountService.deposit usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", -50.0, "Ingreso nómina");
        });
        
        // Then (Comprobar el mensaje de excepción correspondiente)
        assertEquals("Amount must be positive", exception.getMessage());

    }

    // Hecho por: Gonzalo
    @Test
    @DisplayName("11. deposit_Exceeds10k: Lanza excepción si amount > 10000")
    void deposit_Exceeds10kTest() {
        // Given (No hacen falta mocks, preparar variables: amount = 15000)
        double amount = 15000.0;

        // When (Llamar a accountService.deposit usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount, "Deposito grande");
        });

        // Then (Comprobar el mensaje de excepción correspondiente)
        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());
    }

    // Hecho por: Gonzalo
    @Test
    @DisplayName("12. deposit_Exceeds50k: [INACCESSIBLE] La rama de > 50000 nunca se alcanza porque salta en 10000")
    void deposit_Exceeds50kTest() {
        // Given (Comentar que esta rama es inalcanzable, preparar amount = 60000)
        double amount = 60000.0;

        // When (Llamar a accountService.deposit usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount, "Deposito enorme");
        });

        // Then (Comprobar que falla por el límite de 10k, documentando el bad smell)
        // Documentamos el bad smell: la condicion > 50000 es inalcanzable (dead code) ya que todo > 10000 lanza la excepción previa.
        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());
    }

    // Hecho por: Gonzalo
    @Test
    @DisplayName("16. quickDeposit_ZeroAmount: Lanza excepción si amount == 0")
    void quickDeposit_ZeroAmountTest() {
        // Given (Preparar variables: amount = 0, sin descripción)
        double amount = 0.0;

        // When (Llamar al deposit rápido de accountService usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount);
        });

        // Then (Comprobar el mensaje de excepción)
        assertEquals("Amount must be positive", exception.getMessage());
    }

    // Hecho por: Gonzalo
    @Test
    @DisplayName("17. quickDeposit_NegativeAmount: Lanza excepción si amount < 0")
    void quickDeposit_NegativeAmountTest() {
        // Given (Preparar variables: amount = -10, sin descripción)
        double amount = -10.0;

        // When (Llamar al deposit rápido de accountService usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount);
        });

        // Then (Comprobar el mensaje de excepción)
        assertEquals("Amount must be positive", exception.getMessage());
    }

    // Hecho por: Gonzalo
    @Test
    @DisplayName("18. quickDeposit_Exceeds10k: Lanza excepción si amount > 10000")
    void quickDeposit_Exceeds10kTest() {
        // Given (Preparar variables: amount = 20000, sin descripción)
        double amount = 20000.0;

        // When (Llamar al deposit rápido de accountService usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount);
        });

        // Then (Comprobar el mensaje de excepción)
        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());
    }

    // Hecho por: Blas
    @Test
    @DisplayName("19. quickDeposit_Exceeds50k: [INACCESSIBLE] Rama inalcanzable")
    void quickDeposit_Exceeds50kTest() {
        // Given (Comentar que esta rama es inalcanzable igual que la 12)
        double depositAmount = 50001.0;

        // When (Llamar al deposit rápido)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", depositAmount);
        });

        // Then (Comprobar que falla por el límite de 10k)
        // Dead code
        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());

    }


    // ÉXITOS DEPOSIT

    // Hecho por: Blas
    @Test
    @DisplayName("13. deposit_Success_Email: Ingreso válido y notificación por EMAIL")
    void deposit_Success_EmailTest() {
        // Given (Configurar User con NotificationType.EMAIL, configurar Mocks de BD)
        User user = new User();

        user.setNotificationType(User.NotificationType.EMAIL);
        Account userAccount = new Account("ES123", Account.AccountType.CHECKING, 100);
        userAccount.setUser(user);

        // When (Llamar a accountService.deposit con cantidad válida)
        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        // Deposit
        accountService.deposit("ES123", 50, "Ingreso nómina");

        // Then (Verificar guardado en BD y verificar que se llamó a emailService.sendNotification)
        assertEquals(150, userAccount.getBalance());

        verify(accountRepository, times(1)).findByAccountNumber("ES123");
        verify(accountRepository, times(1)).findByAccountNumber("ES123");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verify(emailService, times(1)).sendNotification(
            eq(user),
            eq(Notification.NotificationType.DEPOSIT),
            anyString(),
            anyString()
        );

        verifyNoInteractions(smsService);
    }

    // Hecho por: Blas
    @Test
    @DisplayName("14. deposit_Success_Sms: Ingreso válido y notificación por SMS")
    void deposit_Success_SmsTest() {
        // Given (Configurar User con NotificationType.SMS, configurar Mocks de BD)
        User user = new User();
        user.setNotificationType(User.NotificationType.SMS);
        Account userAccount = new Account("ES123", Account.AccountType.SAVINGS, 200);
        userAccount.setUser(user);

        // When (Llamar a accountService.deposit con cantidad válida)
        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        // Deposiro
        accountService.deposit("ES123", 75, "Ingreso de cliente");

        // Then (Verificar guardado en BD y verificar que se llamó a smsService.sendNotification)
        assertEquals(275, userAccount.getBalance());

        verify(accountRepository, times(1)).findByAccountNumber("ES123");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verify(smsService, times(1)).sendNotification(
                eq(user),
                eq(Notification.NotificationType.DEPOSIT),
                anyString(),
                anyString()
        );

        verifyNoInteractions(emailService);

    }

    // Hecho por: Raúl Tejada Merinero
    @Test
    @DisplayName("15. deposit_Success_NoNotif: Ingreso válido donde no entra ni en EMAIL ni en SMS")
    void deposit_Success_NoNotifTest() {
        // Given (Configurar User con NotificationType nulo o distinto, configurar Mocks de BD)
        User user = new User();
        user.setNotificationType(null); // Sin notificaciones configuradas
        Account account = new Account("ES123", Account.AccountType.CHECKING, 100);
        account.setUser(user);

        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When (Llamar a accountService.deposit con cantidad válida)
        accountService.deposit("ES123", 50, "Regalo");

        // Then (Verificar guardado y usar verifyNoInteractions() con emailService y smsService)
        assertEquals(150, account.getBalance()); // 100 + 50
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verifyNoInteractions(emailService); // Verifica que NO se mandó email
        verifyNoInteractions(smsService);   // Verifica que NO se mandó SMS
    }

    // Hecho por: Blas
    @Test
    @DisplayName("20. quickDeposit_Success_Email: Ingreso rápido y notificación por EMAIL")
    void quickDeposit_Success_EmailTest() {
        // Given (Configurar User con EMAIL, configurar Mocks de BD)
        User user = new User();

        user.setNotificationType(User.NotificationType.EMAIL);
        Account userAccount = new Account("ES125", Account.AccountType.CHECKING, 300);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES125")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        // When (Llamar al deposit rápido con cantidad válida)
        accountService.deposit("ES125", 100);

        // Then (Verificar guardado en BD y llamada a emailService)
        assertEquals(400, userAccount.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber("ES125");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verify(emailService, times(1)).sendNotification(
            eq(user),
            eq(Notification.NotificationType.DEPOSIT),
            anyString(),
            anyString()
        );

        verifyNoInteractions(smsService);

    }

    // Hecho por: Blas
    @Test
    @DisplayName("21. quickDeposit_Success_Sms: Ingreso rápido y notificación por SMS")
    void quickDeposit_Success_SmsTest() {
        // Given (Configurar User con SMS, configurar Mocks de BD)
        User user = new User();
        user.setNotificationType(User.NotificationType.SMS);
        Account userAccount = new Account("ES126", Account.AccountType.SAVINGS, 250);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES126")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        // When (Llamar al deposit rápido con cantidad válida)
        accountService.deposit("ES126", 150);

        // Then (Verificar guardado en BD y llamada a smsService)
        assertEquals(400, userAccount.getBalance());

        verify(accountRepository, times(1)).findByAccountNumber("ES126");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verify(smsService, times(1)).sendNotification(
            eq(user),
            eq(Notification.NotificationType.DEPOSIT),
            anyString(),
            anyString()
        );

        verifyNoInteractions(emailService);

    }

    // Hecho por: Blas
    @Test
    @DisplayName("22. quickDeposit_Success_NoNotif: Ingreso rápido sin notificación")
    void quickDeposit_Success_NoNotifTest() {
        // Given (Configurar User sin notificaciones configuradas, configurar Mocks de BD)
        User user = new User();
        user.setNotificationType(null); // Sin notificaciones
        Account userAccount = new Account("ES127", Account.AccountType.SAVINGS, 150);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES127")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        // When (Llamar al deposit rápido con cantidad válida)
        accountService.deposit("ES127", 50);

        // Then (Verificar guardado y verificar que no se llamaron servicios de notificación)
        assertEquals(200, userAccount.getBalance());

        verify(accountRepository, times(1)).findByAccountNumber("ES127");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);

        verifyNoInteractions(emailService, smsService);

    }


    // RETIROS / WITHDRAW)

    // Hecho por: Adrián Villalba Cuello de Oro
    @Test
    @DisplayName("23. withdraw_NegativeOrZero: Lanza excepción si amount <= 0")
    void withdraw_NegativeOrZeroTest() {
        // Given (Preparar cantidad inválida <= 0)
        String accountNumber = "ES0000123456";
        double amount = -3.0;

        // When (Llamar a accountService.withdraw usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.withdraw(accountNumber, amount, "Retirada de prueba");
        });

        // Then (Comprobar mensaje de excepción)
        assertEquals("Amount must be positive", exception.getMessage());

    }

    // Hecho por: [Nombre del Alumno]
    @Test
    @DisplayName("24. withdraw_Exceeds5k: Lanza excepción si amount > 5000")
    void withdraw_Exceeds5kTest() {
        // Given (Preparar cantidad inválida > 5000)

        // When (Llamar a accountService.withdraw usando assertThrows)

        // Then (Comprobar mensaje de excepción)

    }

    // Hecho por: Raúl Tejada Merinero
    @Test
    @DisplayName("25. withdraw_InsufficientFunds: Lanza excepción si saldo < amount")
    void withdraw_InsufficientFundsTest() {
        // Given (Configurar Mock de BD para devolver cuenta con saldo menor que el retiro solicitado)
        Account account = new Account("ES123", Account.AccountType.CHECKING, 50); // Solo tiene 50€
        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(account));

        // When (Llamar a accountService.withdraw usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.withdraw("ES123", 100, "Compra cara"); // Intenta sacar 100€
        });
        
        // Then (Comprobar mensaje de "Insufficient funds")
        assertEquals("Insufficient funds", exception.getMessage());

    }

    // Hecho por: [Nombre del Alumno]
    @Test
    @DisplayName("26. withdraw_Success_Email: Retiro válido y notificación EMAIL")
    void withdraw_Success_EmailTest() {
        // Given (Configurar cuenta con fondos, User con EMAIL, Mocks de BD)

        // When (Llamar a accountService.withdraw)

        // Then (Verificar resta de saldo, guardado en BD y llamada a emailService)

    }

    // Hecho por: Blas
    @Test
    @DisplayName("27. withdraw_Success_Sms: Retiro válido y notificación SMS")
    void withdraw_Success_SmsTest() {
        // Given (Configurar cuenta con fondos, User con SMS, Mocks de BD)
        User user = new User();
        user.setNotificationType(User.NotificationType.SMS);
        Account userAccount = new Account("ES128", Account.AccountType.CHECKING, 500);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES128")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        // When (Llamar a accountService.withdraw)
        accountService.withdraw("ES128", 150, "Compra tienda");

        // Then (Verificar resta de saldo, guardado en BD y llamada a smsService)
        assertEquals(350, userAccount.getBalance());

        verify(accountRepository, times(1)).findByAccountNumber("ES128");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verify(smsService, times(1)).sendNotification(
                eq(user),
                eq(Notification.NotificationType.WITHDRAWAL),
                anyString(),
                anyString()
        );

        verifyNoInteractions(emailService);

    }

    // Hecho por: [Nombre del Alumno]
    @Test
    @DisplayName("28. withdraw_Success_NoNotif: Retiro válido sin notificación")
    void withdraw_Success_NoNotifTest() {
        // Given (Configurar cuenta con fondos, User sin notificación, Mocks de BD)

        // When (Llamar a accountService.withdraw)

        // Then (Verificar resta de saldo, guardado y cerciorarse de no llamar a notificaciones)

    }


    // FALLOS TRANSFER

    // Hecho por: Adrián Villalba Cuello de Oro
    @Test
    @DisplayName("29. transfer_NegativeOrZero: Lanza excepción si amount <= 0")
    void transfer_NegativeOrZeroTest() {
        // Given (Preparar amount <= 0)
        String fromAccountNumber = "ES0000123456";
        String toAccountNumber = "ES0000654321";
        double amount = -3.0;

        // When (Llamar a accountService.transfer usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer(fromAccountNumber, toAccountNumber, amount);
        });

        // Then (Comprobar mensaje de excepción)
        assertEquals("Amount must be positive", exception.getMessage());

    }

    // Hecho por: [Nombre del Alumno]
    @Test
    @DisplayName("30. transfer_Exceeds20k: Lanza excepción si amount > 20000")
    void transfer_Exceeds20kTest() {
        // Given (Preparar amount > 20000)

        // When (Llamar a accountService.transfer usando assertThrows)

        // Then (Comprobar mensaje de excepción)

    }

    // Hecho por: Raúl Tejada Merinero
    @Test
    @DisplayName("31. transfer_SameAccount: Lanza excepción si origen y destino son la misma cuenta")
    void transfer_SameAccountTest() {
        // Given (Mock BD devuelve la misma instancia de cuenta para origen y destino)
        Account account = new Account("ES123", Account.AccountType.CHECKING, 500);
        // Cuando busque el origen y el destino, devolvemos la MISMA cuenta
        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(account));

        // When (Llamar a accountService.transfer usando assertThrows)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("ES123", "ES123", 50);
        });

        // Then (Comprobar mensaje "Cannot transfer to same account")
        assertEquals("Cannot transfer to same account", exception.getMessage());

    }

    // Hecho por: [Nombre del Alumno]
    @Test
    @DisplayName("32. transfer_InsufficientFunds: Lanza excepción si saldo origen < amount")
    void transfer_InsufficientFundsTest() {
        // Given (Mock BD devuelve cuenta origen sin fondos y cuenta destino normal)

        // When (Llamar a accountService.transfer usando assertThrows)

        // Then (Comprobar mensaje "Insufficient funds")

    }

    // ÉXITOS TRANSFER

    // Hecho por: [Nombre del Alumno]
    @Test
    @DisplayName("33. transfer_Success_Emails: Transferencia válida. Remitente EMAIL, Destinatario EMAIL")
    void transfer_Success_EmailsTest() {
        // Given (Mock BD devuelve cuentas válidas con User en EMAIL)

        // When (Llamar a accountService.transfer)

        // Then (Verificar intercambio de saldos, guardado de transacciones y llamadas a emailService)

    }

    // Hecho por: [Nombre del Alumno]
    @Test
    @DisplayName("34. transfer_Success_Sms: Transferencia válida. Remitente SMS, Destinatario SMS")
    void transfer_Success_SmsTest() {
        // Given (Mock BD devuelve cuentas válidas con User en SMS)

        // When (Llamar a accountService.transfer)

        // Then (Verificar intercambio de saldos, guardado de transacciones y llamadas a smsService)
    }

    // Hecho por: Raúl Tejada Merinero
    @Test
    @DisplayName("35. transfer_Success_NoNotifs: Transferencia válida sin notificaciones configuradas")
    void transfer_Success_NoNotifsTest() {
        // Given (Mock BD devuelve cuentas válidas con User sin notificaciones)
        User user = new User();
        user.setNotificationType(null); // Sin notificaciones
        Account origen = new Account("ES1", Account.AccountType.CHECKING, 500);
        Account destino = new Account("ES2", Account.AccountType.SAVINGS, 100);
        origen.setUser(user);
        destino.setUser(user);

        when(accountRepository.findByAccountNumber("ES1")).thenReturn(Optional.of(origen));
        when(accountRepository.findByAccountNumber("ES2")).thenReturn(Optional.of(destino));

        // When (Llamar a accountService.transfer)
        accountService.transfer("ES1", "ES2", 200);

        // Then (Verificar transferencia completa usando verifyNoInteractions() en notificaciones)
        assertEquals(300, origen.getBalance());
        assertEquals(300, destino.getBalance());
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // 2 transacciones creadas
        verifyNoInteractions(emailService, smsService);
    }

}
