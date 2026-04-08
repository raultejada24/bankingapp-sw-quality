package es.codeurjc.service;

import es.codeurjc.model.Account;
import es.codeurjc.model.User;
import es.codeurjc.model.Notification;
import es.codeurjc.model.Transaction;
import es.codeurjc.repository.AccountRepository;
import es.codeurjc.repository.TransactionRepository;
import es.codeurjc.service.notifications.EmailNotificationService;
import es.codeurjc.service.notifications.SmsNotificationService;
import es.codeurjc.service.exceptions.InvalidAmountException;
import es.codeurjc.service.exceptions.LimitExceededException;
import es.codeurjc.service.exceptions.InsufficientFundsException;
import es.codeurjc.service.exceptions.AccountNotFoundException;
import es.codeurjc.service.exceptions.InvalidOperationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing bank accounts (REFACTORED VERSION)
 * Implements clean code principles: SOLID, DRY, KISS
 */
@Service
public class AccountServiceRefactored {

    // Issue 8: Magic Numbers extracted as constants
    private static final double MAX_DEPOSIT_LIMIT = 10000.0;
    private static final double MAX_WITHDRAWAL_LIMIT = 5000.0;
    private static final double MAX_TRANSFER_LIMIT = 20000.0;

    // Issue 1: Duplicated literals extracted as constants
    private static final String DEPOSIT_CONFIRMATION_SUBJECT = "Deposit Confirmation";
    private static final String WITHDRAWAL_CONFIRMATION_SUBJECT = "Withdrawal Confirmation";
    private static final String TRANSFER_SENT_SUBJECT = "Transfer Sent";
    private static final String TRANSFER_RECEIVED_SUBJECT = "Transfer Received";

    // Issue 12: Error messages centralized in constants
    // Note: Only ERROR_CANNOT_DELETE_WITH_BALANCE and ERROR_SAME_ACCOUNT_TRANSFER are used
    // Other error messages are encapsulated in custom exceptions (Issue 13)
    private static final String ERROR_SAME_ACCOUNT_TRANSFER = "Cannot transfer to same account";
    private static final String ERROR_CANNOT_DELETE_WITH_BALANCE = "Cannot delete account with non-zero balance";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final RandomService randomService;

    public AccountServiceRefactored(AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            EmailNotificationService emailService,
            SmsNotificationService smsService,
            RandomService randomService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.randomService = randomService;
    }

    /**
     * Create a new account
     * Issue 21: Validate that account number doesn't already exist (uniqueness)
     */
    public Account createAccount(User user, Account.AccountType accountType) {
        String accountNumber = generateAccountNumber();
        
        // Issue 21: Check if account number already exists in database
        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new InvalidOperationException("Account number already exists: " + accountNumber);
        }
        
        Account account = new Account(accountNumber, accountType, 0);
        account.setUser(user);
        return accountRepository.save(account);
    }

    /**
     * Generate account number
     */
    private String generateAccountNumber() {
        return String.format("ES%010d", randomService.nextInt(1000000000));
    }

    /**
     * Get account by account number
     * Issue 13: Throw specific AccountNotFoundException instead of generic IllegalArgumentException
     */
    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    /**
     * Get all accounts for a user
     */
    public List<Account> getUserAccounts(User user) {
        return accountRepository.findByUser(user);
    }

    /**
     * Issue 6: Refactored deposit methods using method overloading
     * Quick deposit without description - delegates to main method
     */
    @Transactional
    public Account deposit(String accountNumber, double amount) {
        return deposit(accountNumber, amount, "Deposit");
    }

    /**
     * Issue 6: Main deposit method with description
     * Issue 9: Single condition for amount validation (amount <= 0)
     * Issue 10: Centralized notification sending
     * Issue 13: Throw specific exceptions instead of generic IllegalArgumentException
     */
    @Transactional
    public Account deposit(String accountNumber, double amount, String description) {
        // Issue 9: Consolidated validation (was separate if for == 0 and < 0)
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        // Issue 5: Removed unreachable condition (amount > 50000)
        if (amount > MAX_DEPOSIT_LIMIT) {
            throw new LimitExceededException("Deposit", amount, MAX_DEPOSIT_LIMIT);
        }

        Account account = getAccount(accountNumber);
        account.deposit(amount);

        // Record transaction
        Transaction transaction = new Transaction(account,
                Transaction.TransactionType.DEPOSIT,
                amount,
                description);
        transactionRepository.save(transaction);
        accountRepository.save(account);

        // Issue 10: Use centralized sendNotification method
        sendNotification(account, Notification.NotificationType.DEPOSIT,
                DEPOSIT_CONFIRMATION_SUBJECT,
                String.format("Deposit of %.2f EUR. New balance: %.2f EUR",
                        amount, account.getBalance()));

        return account;
    }

    /**
     * Withdraw money from account
     * Issue 9: Single condition for amount validation
     * Issue 13: Throw specific exceptions instead of generic IllegalArgumentException
     */
    @Transactional
    public Account withdraw(String accountNumber, double amount, String description) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }

        if (amount > MAX_WITHDRAWAL_LIMIT) {
            throw new LimitExceededException("Withdrawal", amount, MAX_WITHDRAWAL_LIMIT);
        }

        Account account = getAccount(accountNumber);

        // Check balance using Account's method (Issue 15: centralized validation)
        if (!account.hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(account.getBalance(), amount);
        }

        account.withdraw(amount);

        // Record transaction
        Transaction transaction = new Transaction(account,
                Transaction.TransactionType.WITHDRAWAL,
                amount,
                description);
        transactionRepository.save(transaction);

        Account savedAccount = accountRepository.save(account);

        // Issue 10: Use centralized sendNotification method
        sendNotification(account, Notification.NotificationType.WITHDRAWAL,
                WITHDRAWAL_CONFIRMATION_SUBJECT,
                String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR",
                        amount, account.getBalance()));

        return savedAccount;
    }

    /**
     * Transfer money between accounts
     * Issue 4: Renamed m and o to sourceAccount and destinationAccount
     * Issue 3: Use .equals() instead of == for String comparison
     * Issue 10: Use centralized sendNotification
     * Issue 11: Refactored into smaller helper methods for better readability
     */
    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        // Issue 4: Descriptive variable names instead of m and o
        Account sourceAccount = getAccount(fromAccountNumber);
        Account destinationAccount = getAccount(toAccountNumber);

        // Issue 11: Validate preconditions using helper method
        validateTransfer(sourceAccount, destinationAccount, amount);

        // Issue 11: Perform transfer and record transactions using helper method
        performTransfer(sourceAccount, destinationAccount, fromAccountNumber, toAccountNumber, amount);

        // Issue 11: Send notifications using helper method
        notifyTransfer(sourceAccount, destinationAccount, toAccountNumber, fromAccountNumber, amount);
    }

    /**
     * Issue 7: Renamed from rm() to deleteAccount()
     * Validates account has zero balance before deletion
     * Issue 13: Throw specific InvalidOperationException
     */
    @Transactional
    public void deleteAccount(String accountNumber) {
        Account account = getAccount(accountNumber);

        // Validation: cannot delete if balance != 0
        if (account.getBalance() > 0) {
            throw new InvalidOperationException(ERROR_CANNOT_DELETE_WITH_BALANCE);
        }

        accountRepository.delete(account);
    }

    /**
     * Get account balance
     */
    public double getBalance(String accountNumber) {
        Account account = getAccount(accountNumber);
        return account.getBalance();
    }

    /**
     * Get account transactions
     */
    public List<Transaction> getTransactions(String accountNumber) {
        Account account = getAccount(accountNumber);
        return transactionRepository.findByAccountOrderByTimestampDesc(account);
    }

    /**
     * Issue 10: Centralized notification sending method
     * Issue 14: Use getPreferredNotificationType() delegate method (Law of Demeter)
     * Issue 20: Complete control flow with default case
     * Extracts duplicated notification logic used in deposit, withdraw, transfer
     */
    private void sendNotification(Account account, Notification.NotificationType type, String subject, String message) {
        User user = account.getUser();
        // Issue 14: Use delegated method from Account to respect Law of Demeter
        User.NotificationType notifType = account.getPreferredNotificationType();

        // Issue 20: Switch with default case to prevent silent failures
        switch (notifType) {
            case EMAIL:
                emailService.sendNotification(user, type, subject, message);
                break;
            case SMS:
                smsService.sendNotification(user, type, subject, message);
                break;
            default:
                throw new UnsupportedOperationException(
                    "Unsupported notification type: " + notifType);
        }
    }

    /**
     * Issue 11: Extract helper method to reduce transfer() complexity (Long Method refactoring)
     * Validates transfer preconditions
     * Issue 13: Throw specific exceptions instead of generic IllegalArgumentException
     */
    private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (amount > MAX_TRANSFER_LIMIT) {
            throw new LimitExceededException("Transfer", amount, MAX_TRANSFER_LIMIT);
        }
        // Issue 3: Using .equals() instead of == for String comparison
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            throw new InvalidOperationException(ERROR_SAME_ACCOUNT_TRANSFER);
        }
        // Issue 15: Use Account's hasSufficientBalance method
        if (!sourceAccount.hasSufficientBalance(amount)) {
            throw new InsufficientFundsException(sourceAccount.getBalance(), amount);
        }
    }

    /**
     * Issue 11: Extract helper method for transfer operations
     * Performs the money movement and records both transactions
     */
    private void performTransfer(Account sourceAccount, Account destinationAccount, String fromAccountNumber, String toAccountNumber, double amount) {
        // Perform transfer
        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);

        // Record outgoing transaction
        Transaction sentTransaction = new Transaction(sourceAccount,
                Transaction.TransactionType.TRANSFER_SENT,
                amount,
                "Transfer to " + toAccountNumber);
        sentTransaction.setDestinationAccountNumber(toAccountNumber);
        transactionRepository.save(sentTransaction);

        // Record incoming transaction
        Transaction receivedTransaction = new Transaction(destinationAccount,
                Transaction.TransactionType.TRANSFER_RECEIVED,
                amount,
                "Transfer from " + fromAccountNumber);
        receivedTransaction.setDestinationAccountNumber(fromAccountNumber);
        transactionRepository.save(receivedTransaction);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
    }

    /**
     * Issue 11: Extract helper method for transfer notifications
     */
    private void notifyTransfer(Account sourceAccount, Account destinationAccount, String toAccountNumber, String fromAccountNumber, double amount) {
        // Notify sender
        sendNotification(sourceAccount, Notification.NotificationType.TRANSFER,
                TRANSFER_SENT_SUBJECT,
                String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR",
                        amount, toAccountNumber, sourceAccount.getBalance()));

        // Notify receiver
        sendNotification(destinationAccount, Notification.NotificationType.TRANSFER,
                TRANSFER_RECEIVED_SUBJECT,
                String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR",
                        amount, fromAccountNumber, destinationAccount.getBalance()));
    }
}
