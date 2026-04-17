package es.codeurjc.service;

import es.codeurjc.model.Account;
import es.codeurjc.model.User;
import es.codeurjc.model.Notification;
import es.codeurjc.model.Transaction;
import es.codeurjc.repository.AccountRepository;
import es.codeurjc.repository.TransactionRepository;
import es.codeurjc.service.notifications.EmailNotificationService;
import es.codeurjc.service.notifications.SmsNotificationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing bank accounts.
 */
@Service
public class AccountService {

    // Constants for business rules
    private static final double MAX_DEPOSIT_LIMIT = 10000.0;
    private static final double MAX_WITHDRAWAL_LIMIT = 5000.0;
    private static final double MAX_TRANSFER_LIMIT = 20000.0;
    private static final int MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS = 5;
    private static final int MAX_TRANSACTIONS_HISTORY_RESULTS = 100;
    private static final String DEPOSIT_CONFIRMATION_SUBJECT = "Deposit Confirmation";
    private static final String ERROR_AMOUNT_MUST_BE_POSITIVE = "Amount must be positive";
    private static final String ERROR_MAX_DEPOSIT_EXCEEDED = "Amount exceeds maximum deposit limit";
    private static final String ERROR_MAX_WITHDRAWAL_EXCEEDED = "Amount exceeds maximum withdrawal limit";
    private static final String ERROR_MAX_TRANSFER_EXCEEDED = "Amount exceeds maximum transfer limit";
    private static final String ERROR_INSUFFICIENT_FUNDS = "Insufficient funds";
    private static final String ERROR_SAME_ACCOUNT_TRANSFER = "Cannot transfer to same account";
    private static final String ERROR_ACCOUNT_NOT_FOUND = "Account not found";
    private static final String ERROR_NON_ZERO_BALANCE = "Cannot delete account with non-zero balance";
    private static final String ERROR_ACCOUNT_NUMBER_GENERATION_FAILED = "Could not generate unique account number";
    private static final String WITHDRAWAL_CONFIRMATION_SUBJECT = "Withdrawal Confirmation";
    private static final String TRANSFER_SENT_SUBJECT = "Transfer Sent";
    private static final String TRANSFER_RECEIVED_SUBJECT = "Transfer Received";
    private static final String TRANSFER_SENT_MESSAGE = "Transfer of %.2f EUR to %s. New balance: %.2f EUR";
    private static final String TRANSFER_RECEIVED_MESSAGE = "Transfer of %.2f EUR from %s. New balance: %.2f EUR";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final RandomService randomService;

    public AccountService(AccountRepository accountRepository,
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
     */
    public Account createAccount(User user, Account.AccountType accountType) {
        String accountNumber = generateUniqueAccountNumber();
        Account account = new Account(accountNumber, accountType, 0);
        account.setUser(user);
        return accountRepository.save(account);
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS; attempt++) {
            String accountNumber = generateAccountNumber();
            if (accountRepository.findByAccountNumber(accountNumber).isEmpty()) {
                return accountNumber;
            }
        }
        throw new AccountNumberGenerationException(ERROR_ACCOUNT_NUMBER_GENERATION_FAILED);
    }

    /**
     * Generate account number
     */
    private String generateAccountNumber() {
        return String.format("ES%010d", randomService.nextInt(1000000000));
    }

    /**
     * Get account by account number
     */
    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(ERROR_ACCOUNT_NOT_FOUND));
    }

    /**
     * Get all accounts for a user
     */
    public List<Account> getUserAccounts(User user) {
        return accountRepository.findByUser(user);
    }

    /**
     * Send notification
     */
    private User.NotificationType getNotificationPreference(Account account) {
        return account.getUser().getNotificationType();
    }

    private void sendNotification(Account account, Notification.NotificationType type, String subject, String message) {
        User user = account.getUser();

        // La cadena de llamadas queda oculta tras este método
        User.NotificationType notifType = getNotificationPreference(account);

        switch (notifType) {
            case EMAIL:
                emailService.sendNotification(user, type, subject, message);
                break;
            case SMS:
                smsService.sendNotification(user, type, subject, message);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported notification type: " + notifType);
        }

    }

    // Validation of amount
    private void validateMoneyPrecision(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new InvalidAmountException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        }
    }

    /**
     * Quick deposit without description
     */
    @Transactional
    public Account deposit(String accountNumber, double amount) {
        // Llama al método completo pasando una descripción por defecto ("Quick
        // deposit")
        return deposit(accountNumber, amount, "Quick deposit");
    }

    /**
     * Deposit money into account
     */
    @Transactional
    public Account deposit(String accountNumber, double amount, String description) {
        validateMoneyPrecision(amount);
        if (amount <= 0) {
            throw new InvalidAmountException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        }
        if (amount > MAX_DEPOSIT_LIMIT) {
            throw new LimitExceededException(ERROR_MAX_DEPOSIT_EXCEEDED);
        }

        Account account = getAccount(accountNumber);
        account.deposit(amount);

        // Record transaction
        Transaction transaction = new Transaction(account, Transaction.TransactionType.DEPOSIT,
                amount, description);
        transactionRepository.save(transaction);

        Account savedAccount = accountRepository.save(account);

        // Una única llamada al método sendNotification() en lugar de todo el bloque
        // if/else repetido
        sendNotification(
                account,
                Notification.NotificationType.DEPOSIT,
                DEPOSIT_CONFIRMATION_SUBJECT,
                String.format("Deposit of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance()));

        return savedAccount;
    }

    /**
     * Withdraw money from account
     */

    private void ensureSufficientBalance(Account account, double amount) {
        if (account.getBalance() < amount) {
            throw new InsufficientFundsException(ERROR_INSUFFICIENT_FUNDS);
        }
    }

    @Transactional
    public Account withdraw(String accountNumber, double amount, String description) {
        validateMoneyPrecision(amount);
        if (amount <= 0) {
            throw new InvalidAmountException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        }

        if (amount > MAX_WITHDRAWAL_LIMIT) {
            throw new LimitExceededException(ERROR_MAX_WITHDRAWAL_EXCEEDED);
        }

        Account account = getAccount(accountNumber);

        // Check balance
        ensureSufficientBalance(account, amount);

        account.withdraw(amount);

        // Record transaction
        Transaction transaction = new Transaction(account, Transaction.TransactionType.WITHDRAWAL,
                amount, description);
        transactionRepository.save(transaction);

        Account savedAccount = accountRepository.save(account);

        // Una única llamada al método sendNotification() en lugar de todo el bloque
        // if/else repetido
        sendNotification(
                account,
                Notification.NotificationType.WITHDRAWAL,
                WITHDRAWAL_CONFIRMATION_SUBJECT,
                String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance()));

        return savedAccount;
    }

    /**
     * Transfer money between accounts
     */
    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {

        Account sourceAccount = getAccount(fromAccountNumber);
        Account destinationAccount = getAccount(toAccountNumber);

        validateTransfer(sourceAccount, destinationAccount, amount);
        recordTransfer(sourceAccount, destinationAccount, fromAccountNumber, toAccountNumber, amount);
        performTransfer(sourceAccount, destinationAccount, amount);
        notifyTransfer(sourceAccount, destinationAccount, toAccountNumber, fromAccountNumber, amount);

    }

    private void validateTransfer(Account sourceAccount, Account destinationAccount, double amount) {
        validateMoneyPrecision(amount);
        if (amount <= 0)
            throw new InvalidAmountException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        if (amount > MAX_TRANSFER_LIMIT)
            throw new LimitExceededException(ERROR_MAX_TRANSFER_EXCEEDED);
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber()))
            throw new SameAccountTransferException(ERROR_SAME_ACCOUNT_TRANSFER);
        ensureSufficientBalance(sourceAccount, amount);
    }

    // Issue 17: Tell, Don't Ask - encapsulates balance check + withdrawal
    private void executeWithdrawal(Account account, double amount) {
        ensureSufficientBalance(account, amount);
        account.withdraw(amount);
    }

    private void performTransfer(Account sourceAccount, Account destinationAccount, double amount) {
        executeWithdrawal(sourceAccount, amount);
        destinationAccount.deposit(amount);
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
    }

    private void recordTransfer(Account sourceAccount, Account destinationAccount,
            String fromAccountNumber, String toAccountNumber, double amount) {
        Transaction sentTransaction = new Transaction(sourceAccount,
                Transaction.TransactionType.TRANSFER_SENT,
                amount,
                "Transfer to " + toAccountNumber);
        sentTransaction.setDestinationAccountNumber(toAccountNumber);
        transactionRepository.save(sentTransaction);

        Transaction receivedTransaction = new Transaction(destinationAccount,
                Transaction.TransactionType.TRANSFER_RECEIVED,
                amount,
                "Transfer from " + fromAccountNumber);
        receivedTransaction.setDestinationAccountNumber(fromAccountNumber);
        transactionRepository.save(receivedTransaction);
    }

    private void notifyTransfer(Account sourceAccount, Account destinationAccount,
            String toAccountNumber, String fromAccountNumber, double amount) {

        // Notificación al que ENVÍA la transferencia
        sendNotification(sourceAccount, Notification.NotificationType.TRANSFER, TRANSFER_SENT_SUBJECT,
                String.format(TRANSFER_SENT_MESSAGE, amount, toAccountNumber,
                        sourceAccount.getBalance()));

        // Notificación al que RECIBE la transferencia
        sendNotification(destinationAccount, Notification.NotificationType.TRANSFER, TRANSFER_RECEIVED_SUBJECT,
                String.format(TRANSFER_RECEIVED_MESSAGE, amount, fromAccountNumber,
                        destinationAccount.getBalance()));
    }

    /**
     * Delete account
     */
    public void deleteAccount(String accountNumber) {
        Account account = getAccount(accountNumber);

        if (account.getBalance() != 0) {
            throw new NonZeroBalanceException(ERROR_NON_ZERO_BALANCE);
        }

        accountRepository.delete(account);
    }

    @Deprecated
    public void rm(String accountNumber) {
        deleteAccount(accountNumber);
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
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(String accountNumber) {
        Account account = getAccount(accountNumber);
        return transactionRepository.findByAccountOrderByTimestampDesc(account)
                .stream()
                .limit(MAX_TRANSACTIONS_HISTORY_RESULTS)
                .collect(Collectors.toList());
    }

    public static class AccountNotFoundException extends IllegalArgumentException {
        public AccountNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidAmountException extends IllegalArgumentException {
        public InvalidAmountException(String message) {
            super(message);
        }
    }

    public static class LimitExceededException extends IllegalArgumentException {
        public LimitExceededException(String message) {
            super(message);
        }
    }

    public static class InsufficientFundsException extends IllegalArgumentException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    public static class SameAccountTransferException extends IllegalArgumentException {
        public SameAccountTransferException(String message) {
            super(message);
        }
    }

    public static class NonZeroBalanceException extends IllegalArgumentException {
        public NonZeroBalanceException(String message) {
            super(message);
        }
    }

    public static class AccountNumberGenerationException extends IllegalStateException {
        public AccountNumberGenerationException(String message) {
            super(message);
        }
    }
}
