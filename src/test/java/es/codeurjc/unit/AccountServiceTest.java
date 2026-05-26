package es.codeurjc.unit;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import es.codeurjc.model.Account;
import es.codeurjc.model.Notification;
import es.codeurjc.model.Transaction;
import es.codeurjc.model.User;
import es.codeurjc.repository.AccountRepository;
import es.codeurjc.repository.TransactionRepository;
import es.codeurjc.service.AccountService;
import es.codeurjc.service.DailyWithdrawalLimitExceededException;
import es.codeurjc.service.RandomService;
import es.codeurjc.service.UserService;
import es.codeurjc.service.notifications.EmailNotificationService;
import es.codeurjc.service.notifications.SmsNotificationService;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

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
    @Mock
    private UserService userService;

    @InjectMocks
    private AccountService accountService;

    private User adultUser(long id, User.NotificationType notificationType) {
        User user = new User();
        user.setId(id);
        user.setBirthDate(LocalDate.now().minusYears(20));
        user.setNotificationType(notificationType);
        return user;
    }

    private User minorUser(long id, User.NotificationType notificationType) {
        User user = new User();
        user.setId(id);
        user.setBirthDate(LocalDate.now().minusYears(17));
        user.setNotificationType(notificationType);
        return user;
    }

    @Test
    @DisplayName("1. createAccount: Prueba que genera un numero, se asigna al usuario y se guarda")
    void createAccountTest() {
        User mockUser = new User("Ana", "Pass123", "USER");
        when(randomService.nextInt(anyInt())).thenReturn(123456);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account newAccount = accountService.createAccount(mockUser, Account.AccountType.CHECKING);

        assertNotNull(newAccount);
        assertEquals("ES0000123456", newAccount.getAccountNumber());
        assertEquals(mockUser, newAccount.getUser());
        assertEquals(0.0, newAccount.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("2. createAccount_Collision: Prueba que si falla 5 veces la generacion, lanza excepcion")
    void createAccount_CollisionTest() {
        User mockUser = new User("Ana", "Pass123", "USER");
        when(randomService.nextInt(anyInt())).thenReturn(123456);
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(new Account()));

        assertThrows(AccountService.AccountNumberGenerationException.class, () -> {
            accountService.createAccount(mockUser, Account.AccountType.CHECKING);
        });

        verify(accountRepository, times(5)).findByAccountNumber(anyString());
    }

    @Test
    @DisplayName("3. getAccount_Success: Prueba que devuelve la cuenta si existe")
    void getAccount_SuccessTest() {
        Account mockAccount = new Account("ES12345", Account.AccountType.SAVINGS, 100);
        when(accountRepository.findByAccountNumber("ES12345")).thenReturn(Optional.of(mockAccount));

        Account result = accountService.getAccount("ES12345");

        assertNotNull(result);
        assertEquals("ES12345", result.getAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber("ES12345");
    }

    @Test
    @DisplayName("4. getAccount_NotFound: Prueba que lanza IllegalArgumentException si no existe")
    void getAccount_NotFoundTest() {
        when(accountRepository.findByAccountNumber("ES99999")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.getAccount("ES99999");
        });

        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    @DisplayName("5. getUserAccounts: Prueba que devuelve la lista de cuentas del usuario")
    void getUserAccountsTest() {
        User mockUser = new User("Ana", "Pass123", "USER");
        List<Account> listAccounts = List.of(new Account(), new Account());
        when(accountRepository.findByUser(mockUser)).thenReturn(listAccounts);

        List<Account> myAccounts = accountService.getUserAccounts(mockUser);

        assertNotNull(myAccounts);
        assertEquals(2, myAccounts.size());
        verify(accountRepository, times(1)).findByUser(mockUser);
    }

    @Test
    @DisplayName("6. getBalance: Prueba que devuelve el saldo correcto")
    void getBalanceTest() {
        Account mockAccount = new Account();
        mockAccount.setAccountNumber("ES0000123456");
        mockAccount.setBalance(500.0);
        when(accountRepository.findByAccountNumber("ES0000123456")).thenReturn(Optional.of(mockAccount));

        double myBalance = accountService.getBalance("ES0000123456");

        assertNotNull(myBalance);
        assertEquals(500.0, myBalance, 0.001);
        verify(accountRepository, times(1)).findByAccountNumber("ES0000123456");
    }

    @Test
    @DisplayName("7. getTransactions: Prueba que devuelve la lista de transacciones ordenada")
    void getTransactionsTest() {
        Account account = new Account("ES12345", Account.AccountType.SAVINGS, 100);
        when(accountRepository.findByAccountNumber("ES12345")).thenReturn(Optional.of(account));
        Transaction t1 = new Transaction(account, Transaction.TransactionType.DEPOSIT, 100, "Deposit");
        java.util.List<Transaction> transactions = java.util.List.of(t1);
        when(transactionRepository.findByAccountOrderByTimestampDesc(account)).thenReturn(transactions);

        java.util.List<Transaction> result = accountService.getTransactions("ES12345");

        assertEquals(transactions, result);
        verify(accountRepository, times(1)).findByAccountNumber("ES12345");
        verify(transactionRepository, times(1)).findByAccountOrderByTimestampDesc(account);
    }

    @Test
    @DisplayName("8. rm_Success: Prueba que elimina la cuenta si el saldo es 0")
    void rm_SuccessTest() {
        Account account = new Account("ES12345", Account.AccountType.SAVINGS, 0);
        when(accountRepository.findByAccountNumber("ES12345")).thenReturn(Optional.of(account));

        accountService.rm("ES12345");

        verify(accountRepository, times(1)).findByAccountNumber("ES12345");
        verify(accountRepository, times(1)).delete(account);
    }

    @Test
    @DisplayName("9. rm_HasBalance: Prueba que lanza IllegalArgumentException si el saldo != 0")
    void rm_HasBalanceTest() {
        Account mockAccount = new Account();
        mockAccount.setAccountNumber("ES0000123456");
        mockAccount.setBalance(500.0);
        when(accountRepository.findByAccountNumber("ES0000123456")).thenReturn(Optional.of(mockAccount));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.rm("ES0000123456");
        });

        assertEquals("Cannot delete account with non-zero balance", exception.getMessage());
    }

    @Test
    @DisplayName("10. deposit_ZeroAmount: Lanza excepcion si amount == 0")
    void deposit_ZeroAmountTest() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES0000123456", 0.0, "Ingreso de prueba");
        });

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("11. deposit_NegativeAmount: Lanza excepcion si amount < 0")
    void deposit_NegativeAmountTest() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", -50.0, "Ingreso nomina");
        });

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("12. validateMoneyPrecision: Lanza excepcion si el importe es NaN o Infinito")
    void validateMoneyPrecision_SpecialValuesTest() {
        assertThrows(AccountService.InvalidAmountException.class, () -> {
            accountService.deposit("ES123", Double.NaN);
        });

        assertThrows(AccountService.InvalidAmountException.class, () -> {
            accountService.withdraw("ES123", Double.POSITIVE_INFINITY, "test");
        });
    }

    @Test
    @DisplayName("13. deposit_Exceeds10k: Lanza excepcion si amount > 10000")
    void deposit_Exceeds10kTest() {
        double amount = 15000.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount, "Deposito grande");
        });

        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());
    }

    @Test
    @DisplayName("14. deposit_Exceeds50k: INACCESSIBLE rama inalcanzable porque salta en 10000")
    void deposit_Exceeds50kTest() {
        double amount = 60000.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount, "Deposito enorme");
        });

        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());
    }

    @Test
    @DisplayName("15. deposit_Success_Email: Ingreso valido y notificacion por EMAIL")
    void deposit_Success_EmailTest() {
        User user = new User();
        user.setNotificationType(User.NotificationType.EMAIL);
        Account userAccount = new Account("ES123", Account.AccountType.CHECKING, 100);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        accountService.deposit("ES123", 50, "Ingreso nomina");

        assertEquals(150, userAccount.getBalance());
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

    @Test
    @DisplayName("16. deposit_Success_Sms: Ingreso valido y notificacion por SMS")
    void deposit_Success_SmsTest() {
        User user = new User();
        user.setNotificationType(User.NotificationType.SMS);
        Account userAccount = new Account("ES123", Account.AccountType.SAVINGS, 200);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        accountService.deposit("ES123", 75, "Ingreso de cliente");

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

    @Test
    @DisplayName("17. deposit_Success_NoNotif: Ingreso valido sin notificacion")
    void deposit_Success_NoNotifTest() {
        User user = new User();
        user.setNotificationType(null);
        Account account = new Account("ES123", Account.AccountType.CHECKING, 100);
        account.setUser(user);

        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.deposit("ES123", 50, "Regalo");

        assertEquals(150, account.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verifyNoInteractions(emailService);
        verifyNoInteractions(smsService);
    }

    @Test
    @DisplayName("18. sendNotification_NullType: No hace nada si el tipo es null")
    void sendNotification_NullTypeTest() {
        User user = new User();
        user.setNotificationType(null);
        Account account = new Account("ES123", Account.AccountType.CHECKING, 100);
        account.setUser(user);
        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.deposit("ES123", 50);

        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("19. quickDeposit_ZeroAmount: Lanza excepcion si amount == 0")
    void quickDeposit_ZeroAmountTest() {
        double amount = 0.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount);
        });

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("20. quickDeposit_NegativeAmount: Lanza excepcion si amount < 0")
    void quickDeposit_NegativeAmountTest() {
        double amount = -10.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount);
        });

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("21. quickDeposit_Exceeds10k: Lanza excepcion si amount > 10000")
    void quickDeposit_Exceeds10kTest() {
        double amount = 20000.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", amount);
        });

        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());
    }

    @Test
    @DisplayName("22. quickDeposit_Exceeds50k: INACCESSIBLE rama inalcanzable")
    void quickDeposit_Exceeds50kTest() {
        double depositAmount = 50001.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.deposit("ES12345", depositAmount);
        });

        assertEquals("Amount exceeds maximum deposit limit", exception.getMessage());
    }

    @Test
    @DisplayName("23. quickDeposit_Success_Email: Ingreso rapido y notificacion EMAIL")
    void quickDeposit_Success_EmailTest() {
        User user = new User();
        user.setNotificationType(User.NotificationType.EMAIL);
        Account userAccount = new Account("ES125", Account.AccountType.CHECKING, 300);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES125")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        accountService.deposit("ES125", 100);

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

    @Test
    @DisplayName("24. quickDeposit_Success_Sms: Ingreso rapido y notificacion SMS")
    void quickDeposit_Success_SmsTest() {
        User user = new User();
        user.setNotificationType(User.NotificationType.SMS);
        Account userAccount = new Account("ES126", Account.AccountType.SAVINGS, 250);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES126")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        accountService.deposit("ES126", 150);

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

    @Test
    @DisplayName("25. quickDeposit_Success_NoNotif: Ingreso rapido sin notificacion")
    void quickDeposit_Success_NoNotifTest() {
        User user = new User();
        user.setNotificationType(null);
        Account userAccount = new Account("ES127", Account.AccountType.SAVINGS, 150);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES127")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        accountService.deposit("ES127", 50);

        assertEquals(200, userAccount.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber("ES127");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("26. quickDeposit_DefaultDescription: Ingreso rapido guarda descripcion defecto")
    void quickDeposit_DefaultDescriptionTest() {
        User user = new User();
        user.setNotificationType(null);
        Account account = new Account("ES140", Account.AccountType.CHECKING, 200);
        account.setUser(user);
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        when(accountRepository.findByAccountNumber("ES140")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.deposit("ES140", 25);

        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        assertEquals("Quick deposit", transactionCaptor.getValue().getDescription());
        assertEquals(Transaction.TransactionType.DEPOSIT, transactionCaptor.getValue().getType());
        assertEquals(225, account.getBalance());
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("27. withdraw_NegativeOrZero: Lanza excepcion si amount <= 0")
    void withdraw_NegativeOrZeroTest() {
        String accountNumber = "ES0000123456";
        double amount = -3.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.withdraw(accountNumber, amount, "Retirada de prueba");
        });

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("28. withdraw_Exceeds5k: Lanza excepcion si amount > 5000")
    void withdraw_Exceeds5kTest() {
        double amount = 6000.0;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.withdraw("ES12345", amount, "Retirada grande");
        });

        assertEquals("Amount exceeds maximum withdrawal limit", exception.getMessage());
    }

    @Test
    @DisplayName("29. withdraw_InsufficientFunds: Lanza excepcion si saldo < amount")
    void withdraw_InsufficientFundsTest() {
        Account account = new Account("ES123", Account.AccountType.CHECKING, 50);
        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(account));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.withdraw("ES123", 100, "Compra cara");
        });

        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    @DisplayName("30. withdraw_Success_Email: Retiro valido y notificacion EMAIL")
    void withdraw_Success_EmailTest() {
        User user = new User();
        user.setNotificationType(User.NotificationType.EMAIL);
        Account userAccount = new Account("ES129", Account.AccountType.CHECKING, 500);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES129")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        accountService.withdraw("ES129", 150, "Compra online");

        assertEquals(350, userAccount.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber("ES129");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verify(emailService, times(1)).sendNotification(
            eq(user),
            eq(Notification.NotificationType.WITHDRAWAL),
            anyString(),
            anyString()
        );
        verifyNoInteractions(smsService);
    }

    @Test
    @DisplayName("31. withdraw_Success_Sms: Retiro valido y notificacion SMS")
    void withdraw_Success_SmsTest() {
        User user = new User();
        user.setNotificationType(User.NotificationType.SMS);
        Account userAccount = new Account("ES128", Account.AccountType.CHECKING, 500);
        userAccount.setUser(user);

        when(accountRepository.findByAccountNumber("ES128")).thenReturn(Optional.of(userAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(userAccount);

        accountService.withdraw("ES128", 150, "Compra tienda");

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

    @Test
    @DisplayName("32. withdraw_Success_NoNotif: Retiro valido sin notificacion")
    void withdraw_Success_NoNotifTest() {
        User user = new User();
        user.setNotificationType(null);
        Account account = new Account("ES130", Account.AccountType.SAVINGS, 400);
        account.setUser(user);

        when(accountRepository.findByAccountNumber("ES130")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.withdraw("ES130", 100, "Retirada cajero");

        assertEquals(300, account.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber("ES130");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(account);
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("33. withdraw_ExactBalance: retirar saldo exacto deja cuenta a 0")
    void withdraw_ExactBalanceTest() {
        User user = new User();
        user.setNotificationType(null);
        Account account = new Account("ES141", Account.AccountType.SAVINGS, 400);
        account.setUser(user);

        when(accountRepository.findByAccountNumber("ES141")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.withdraw("ES141", 400, "Vaciar cuenta");

        assertEquals(0, account.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber("ES141");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(account);
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("33.1 withdraw_DailyLimitNotExceeded: Permite retiro si no supera limite diario")
    void withdraw_DailyLimitNotExceeded() {
        User user = new User();
        user.setNotificationType(null);
        Account account = new Account("ES999", Account.AccountType.CHECKING, 10000);
        account.setUser(user);

        Transaction t1 = new Transaction(account, Transaction.TransactionType.WITHDRAWAL, 3000, "Retiro previo");
        t1.setTimestamp(LocalDateTime.now().minusHours(2));
        account.setTransactions(List.of(t1));

        when(accountRepository.findByAccountNumber("ES999")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.withdraw("ES999", 1000, "Retiro valido");

        assertEquals(9000, account.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("33.2 withdraw_DailyLimitExceeded: Lanza excepcion si supera limite diario")
    void withdraw_DailyLimitExceeded() {
        Account account = new Account("ES999", Account.AccountType.CHECKING, 10000);
        Transaction t1 = new Transaction(account, Transaction.TransactionType.WITHDRAWAL, 4500, "retiro previo");
        t1.setTimestamp(LocalDateTime.now().minusHours(10));
        account.setTransactions(List.of(t1));

        when(accountRepository.findByAccountNumber("ES999")).thenReturn(Optional.of(account));

        assertThrows(DailyWithdrawalLimitExceededException.class, () -> {
            accountService.withdraw("ES999", 1000, "Retiro que excede");
        });
    }

    @Test
    @DisplayName("33.3 withdraw_DailyLimitWithOldTransactions: Retiros viejos no cuentan")
    void withdraw_DailyLimitWithOldTransactions() {
        User user = new User();
        user.setNotificationType(null);
        Account account = new Account("ES999", Account.AccountType.CHECKING, 10000);
        account.setUser(user);

        Transaction t1 = new Transaction(account, Transaction.TransactionType.WITHDRAWAL, 6000, "Retiro antiguo");
        t1.setTimestamp(LocalDateTime.now().minusHours(48));
        account.setTransactions(List.of(t1));

        when(accountRepository.findByAccountNumber("ES999")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.withdraw("ES999", 4000, "Retiro valido");

        assertEquals(6000, account.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("34. transfer_NegativeOrZero: Lanza excepcion si amount <= 0")
    void transfer_NegativeOrZeroTest() {
        String fromAccountNumber = "ES0000123456";
        String toAccountNumber = "ES0000654321";
        double amount = -3.0;
        Account source = new Account(fromAccountNumber, Account.AccountType.CHECKING, 500);
        source.setUser(adultUser(1L, null));
        when(accountRepository.findByAccountNumber(fromAccountNumber)).thenReturn(Optional.of(source));
        when(userService.isMinor(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer(fromAccountNumber, toAccountNumber, amount);
        });

        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("35. transfer_Exceeds20k: Lanza excepcion si amount > 20000")
    void transfer_Exceeds20kTest() {
        double amount = 25000.0;
        Account source = new Account("ES131", Account.AccountType.CHECKING, 500);
        source.setUser(adultUser(2L, null));
        when(accountRepository.findByAccountNumber("ES131")).thenReturn(Optional.of(source));
        when(userService.isMinor(2L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("ES131", "ES132", amount);
        });

        assertEquals("Amount exceeds maximum transfer limit", exception.getMessage());
    }

    @Test
    @DisplayName("36. transfer_SameAccount: Lanza excepcion si origen y destino son iguales")
    void transfer_SameAccountTest() {
        Account account = new Account("ES123", Account.AccountType.CHECKING, 500);
        account.setUser(adultUser(3L, null));
        when(accountRepository.findByAccountNumber("ES123")).thenReturn(Optional.of(account));
        when(userService.isMinor(3L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("ES123", "ES123", 50);
        });

        assertEquals("Cannot transfer to same account", exception.getMessage());
    }

    @Test
    @DisplayName("37. transfer_InsufficientFunds: Lanza excepcion si saldo origen < amount")
    void transfer_InsufficientFundsTest() {
        User user = adultUser(4L, null);
        Account source = new Account("ES133", Account.AccountType.CHECKING, 50);
        Account destination = new Account("ES134", Account.AccountType.SAVINGS, 100);
        source.setUser(user);
        destination.setUser(user);

        when(accountRepository.findByAccountNumber("ES133")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ES134")).thenReturn(Optional.of(destination));
        when(userService.isMinor(4L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("ES133", "ES134", 100);
        });

        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    @DisplayName("38. transfer_Success_Emails: Transferencia valida con EMAIL")
    void transfer_Success_EmailsTest() {
        User sourceUser = adultUser(5L, User.NotificationType.EMAIL);
        User destinationUser = adultUser(6L, User.NotificationType.EMAIL);
        Account source = new Account("ES135", Account.AccountType.CHECKING, 500);
        Account destination = new Account("ES136", Account.AccountType.SAVINGS, 100);
        source.setUser(sourceUser);
        destination.setUser(destinationUser);

        when(accountRepository.findByAccountNumber("ES135")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ES136")).thenReturn(Optional.of(destination));
        when(userService.isMinor(5L)).thenReturn(false);

        accountService.transfer("ES135", "ES136", 200);

        assertEquals(300, source.getBalance());
        assertEquals(300, destination.getBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(source);
        verify(accountRepository, times(1)).save(destination);
        verify(emailService, times(1)).sendNotification(
            eq(sourceUser),
            eq(Notification.NotificationType.TRANSFER),
            eq("Transfer Sent"),
            anyString()
        );
        verify(emailService, times(1)).sendNotification(
            eq(destinationUser),
            eq(Notification.NotificationType.TRANSFER),
            eq("Transfer Received"),
            anyString()
        );
        verifyNoInteractions(smsService);
    }

    @Test
    @DisplayName("39. transfer_Success_Sms: Transferencia valida con SMS")
    void transfer_Success_SmsTest() {
        User sourceUser = adultUser(7L, User.NotificationType.SMS);
        User destinationUser = adultUser(8L, User.NotificationType.SMS);
        Account source = new Account("ES137", Account.AccountType.CHECKING, 700);
        Account destination = new Account("ES138", Account.AccountType.SAVINGS, 200);
        source.setUser(sourceUser);
        destination.setUser(destinationUser);

        when(accountRepository.findByAccountNumber("ES137")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ES138")).thenReturn(Optional.of(destination));
        when(userService.isMinor(7L)).thenReturn(false);

        accountService.transfer("ES137", "ES138", 300);

        assertEquals(400, source.getBalance());
        assertEquals(500, destination.getBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(source);
        verify(accountRepository, times(1)).save(destination);
        verify(smsService, times(1)).sendNotification(
            eq(sourceUser),
            eq(Notification.NotificationType.TRANSFER),
            eq("Transfer Sent"),
            anyString()
        );
        verify(smsService, times(1)).sendNotification(
            eq(destinationUser),
            eq(Notification.NotificationType.TRANSFER),
            eq("Transfer Received"),
            anyString()
        );
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("40. transfer_Success_NoNotifs: Transferencia valida sin notificaciones")
    void transfer_Success_NoNotifsTest() {
        User user = adultUser(9L, null);
        Account origen = new Account("ES1", Account.AccountType.CHECKING, 500);
        Account destino = new Account("ES2", Account.AccountType.SAVINGS, 100);
        origen.setUser(user);
        destino.setUser(user);

        when(accountRepository.findByAccountNumber("ES1")).thenReturn(Optional.of(origen));
        when(accountRepository.findByAccountNumber("ES2")).thenReturn(Optional.of(destino));
        when(userService.isMinor(9L)).thenReturn(false);

        accountService.transfer("ES1", "ES2", 200);

        assertEquals(300, origen.getBalance());
        assertEquals(300, destino.getBalance());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("43. transfer_MinorUser: Lanza excepcion si usuario es menor")
    void transfer_MinorUserTest() {
        User user = minorUser(10L, null);
        Account source = new Account("ES200", Account.AccountType.CHECKING, 500);
        source.setUser(user);

        when(accountRepository.findByAccountNumber("ES200")).thenReturn(Optional.of(source));
        when(userService.isMinor(10L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("ES200", "ES201", 50);
        });

        assertEquals("Minors cannot make transfers", exception.getMessage());
        verifyNoInteractions(transactionRepository, emailService, smsService);
    }

    @Test
    @DisplayName("44. transfer_NullBirthDate: Lanza excepcion si no tiene fecha nacimiento")
    void transfer_NullBirthDateTest() {
        User user = new User();
        user.setId(12L);
        user.setBirthDate(null);
        Account source = new Account("ES210", Account.AccountType.CHECKING, 500);
        source.setUser(user);

        when(accountRepository.findByAccountNumber("ES210")).thenReturn(Optional.of(source));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("ES210", "ES211", 50);
        });

        assertEquals("Minors cannot make transfers", exception.getMessage());
        verifyNoInteractions(userService, transactionRepository, emailService, smsService);
    }

    @Test
    @DisplayName("45. banned_DefaultValue: isBanned devuelve false por defecto")
    void banned_DefaultValueTest() {
        User user = new User();
        assertFalse(user.isBanned());
    }

    @Test
    @DisplayName("46. banned_SetTrue: setBanned true hace que isBanned devuelva true")
    void banned_SetTrueTest() {
        User user = new User();
        user.setBanned(true);
        assertTrue(user.isBanned());
    }

    @Test
    @DisplayName("47. banned_SetFalse: setBanned false revierte el ban")
    void banned_SetFalseTest() {
        User user = new User();
        user.setBanned(true);
        assertTrue(user.isBanned());
        user.setBanned(false);
        assertFalse(user.isBanned());
    }

    @Test
    @DisplayName("48. deposit_BannedUser: Lanza excepcion si usuario baneado")
    void deposit_BannedUserTest() {
        User bannedUser = new User();
        bannedUser.setBanned(true);
        Account account = new Account("ES200", Account.AccountType.CHECKING, 100);
        account.setUser(bannedUser);
        when(accountRepository.findByAccountNumber("ES200")).thenReturn(Optional.of(account));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.deposit("ES200", 50, "Ingreso bloqueado");
        });

        assertEquals("El usuario está baneado y no puede depositar dinero.", exception.getMessage());
        verify(transactionRepository, times(0)).save(any(Transaction.class));
        verify(accountRepository, times(0)).save(any(Account.class));
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("49. withdraw_BannedUser: Lanza excepcion si usuario baneado")
    void withdraw_BannedUserTest() {
        User bannedUser = new User();
        bannedUser.setBanned(true);
        Account account = new Account("ES201", Account.AccountType.CHECKING, 500);
        account.setUser(bannedUser);
        when(accountRepository.findByAccountNumber("ES201")).thenReturn(Optional.of(account));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.withdraw("ES201", 100, "Retirada bloqueada");
        });

        assertEquals("El usuario está baneado y no puede retirar dinero.", exception.getMessage());
        verify(transactionRepository, times(0)).save(any(Transaction.class));
        verify(accountRepository, times(0)).save(any(Account.class));
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("50. transfer_BannedSender: Lanza excepcion si emisor baneado")
    void transfer_BannedSenderTest() {
        User bannedUser = adultUser(1L, User.NotificationType.EMAIL);
        bannedUser.setBanned(true);
        User normalUser = adultUser(2L, User.NotificationType.EMAIL);
        normalUser.setBanned(false);
        Account source = new Account("ES202", Account.AccountType.CHECKING, 500);
        Account destination = new Account("ES203", Account.AccountType.SAVINGS, 100);
        source.setUser(bannedUser);
        destination.setUser(normalUser);

        when(accountRepository.findByAccountNumber("ES202")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ES203")).thenReturn(Optional.of(destination));
        when(userService.isMinor(anyLong())).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.transfer("ES202", "ES203", 100);
        });

        assertEquals("Operación rechazada: El emisor o receptor se encuentra baneado.", exception.getMessage());
        verifyNoInteractions(transactionRepository, emailService, smsService);
    }

    @Test
    @DisplayName("51. transfer_BannedReceiver: Lanza excepcion si receptor baneado")
    void transfer_BannedReceiverTest() {
        User normalUser = adultUser(3L, User.NotificationType.EMAIL);
        normalUser.setBanned(false);
        User bannedUser = adultUser(4L, User.NotificationType.EMAIL);
        bannedUser.setBanned(true);
        Account source = new Account("ES204", Account.AccountType.CHECKING, 500);
        Account destination = new Account("ES205", Account.AccountType.SAVINGS, 100);
        source.setUser(normalUser);
        destination.setUser(bannedUser);

        when(accountRepository.findByAccountNumber("ES204")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("ES205")).thenReturn(Optional.of(destination));
        when(userService.isMinor(anyLong())).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.transfer("ES204", "ES205", 100);
        });

        assertEquals("Operación rechazada: El emisor o receptor se encuentra baneado.", exception.getMessage());
        verify(transactionRepository, times(0)).save(any(Transaction.class));
        verify(accountRepository, times(0)).save(any(Account.class));
        verifyNoInteractions(emailService, smsService);
    }
}
