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

/**
 * Service for managing bank accounts.
 */
@Service
public class AccountService {

    // Constants for business rules
    private static final double MAX_DEPOSIT_LIMIT = 10000.0;
    private static final double MAX_WITHDRAWAL_LIMIT = 5000.0;
    private static final double MAX_TRANSFER_LIMIT = 20000.0;
    private static final String DEPOSIT_CONFIRMATION_SUBJECT = "Deposit Confirmation";
    private static final String ERROR_AMOUNT_MUST_BE_POSITIVE = "Amount must be positive";
    private static final String ERROR_MAX_DEPOSIT_EXCEEDED = "Amount exceeds maximum deposit limit";
    private static final String ERROR_MAX_WITHDRAWAL_EXCEEDED = "Amount exceeds maximum withdrawal limit";
    private static final String ERROR_MAX_TRANSFER_EXCEEDED = "Amount exceeds maximum transfer limit";
    private static final String ERROR_INSUFFICIENT_FUNDS = "Insufficient funds";
    private static final String ERROR_SAME_ACCOUNT_TRANSFER = "Cannot transfer to same account";

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
        String accountNumber = generateAccountNumber();
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
     */
    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    /**
     * Get all accounts for a user
     */
    public List<Account> getUserAccounts(User user) {
        return accountRepository.findByUser(user);
    }

    /**
     * Quick deposit without description
     */
    @Transactional
    public Account deposit(String accountNumber, double amount) {
        // Llama al método completo pasando una descripción por defecto ("Quick deposit")
        return deposit(accountNumber, amount, "Quick deposit");
    }

    /**
     * Deposit money into account
     */
    @Transactional
    public Account deposit(String accountNumber, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        }
        if (amount > MAX_DEPOSIT_LIMIT) {
            throw new IllegalArgumentException(ERROR_MAX_DEPOSIT_EXCEEDED);
        }

        Account account = getAccount(accountNumber);
        account.deposit(amount);

        // Record transaction
        Transaction transaction = new Transaction(account, Transaction.TransactionType.DEPOSIT,
                amount, description);
        transactionRepository.save(transaction);

        Account savedAccount = accountRepository.save(account);

        // Send notification
        User.NotificationType notifType = account.getUser().getNotificationType();
        if (notifType == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    account.getUser(),
                    Notification.NotificationType.DEPOSIT,
                    DEPOSIT_CONFIRMATION_SUBJECT,
                    String.format("Deposit of %.2f EUR. New balance: %.2f EUR",
                            amount, account.getBalance()));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    account.getUser(),
                    Notification.NotificationType.DEPOSIT,
                    DEPOSIT_CONFIRMATION_SUBJECT,
                    String.format("Deposit: %.2f EUR. Balance: %.2f EUR",
                            amount, account.getBalance()));
        }

        return savedAccount;
    }

    /**
     * Withdraw money from account
     */
    @Transactional
    public Account withdraw(String accountNumber, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        }

        if (amount > MAX_WITHDRAWAL_LIMIT) {
            throw new IllegalArgumentException(ERROR_MAX_WITHDRAWAL_EXCEEDED);
        }

        Account account = getAccount(accountNumber);

        // Check balance
        if (account.getBalance() < amount) {
            throw new IllegalArgumentException(ERROR_INSUFFICIENT_FUNDS);
        }

        account.withdraw(amount);

        // Record transaction
        Transaction transaction = new Transaction(account, Transaction.TransactionType.WITHDRAWAL,
                amount, description);
        transactionRepository.save(transaction);

        Account savedAccount = accountRepository.save(account);

        User.NotificationType notifType = account.getUser().getNotificationType();
        if (notifType == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    account.getUser(),
                    Notification.NotificationType.WITHDRAWAL,
                    "Withdrawal Confirmation",
                    String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance()));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    account.getUser(),
                    Notification.NotificationType.WITHDRAWAL,
                    "Withdrawal",
                    String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance()));
        }

        return savedAccount;
    }

    /**
     * Transfer money between accounts
     */
    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(ERROR_AMOUNT_MUST_BE_POSITIVE);
        }
        if (amount > MAX_TRANSFER_LIMIT) {
            throw new IllegalArgumentException(ERROR_MAX_TRANSFER_EXCEEDED);
        }

        Account sourceAccount = getAccount(fromAccountNumber);
        Account destinationAccount = getAccount(toAccountNumber);

        // Validate same account
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            throw new IllegalArgumentException(ERROR_SAME_ACCOUNT_TRANSFER);
        }

        // Check balance
        if (sourceAccount.getBalance() < amount) {
            throw new IllegalArgumentException(ERROR_INSUFFICIENT_FUNDS);
        }

        // Perform transfer
        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);

        // Record transactions
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

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        User.NotificationType notifType = sourceAccount.getUser().getNotificationType();
        if (notifType == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    sourceAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    sourceAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        }

        User.NotificationType notifTypeTo = destinationAccount.getUser().getNotificationType();
        if (notifTypeTo == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    destinationAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Received",
                    String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR",
                        amount, fromAccountNumber, destinationAccount.getBalance()));
        } else if (notifTypeTo == User.NotificationType.SMS) {
            smsService.sendNotification(
                destinationAccount.getUser(),
                Notification.NotificationType.TRANSFER,
                "Transfer Received",
                String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR", amount, fromAccountNumber, destinationAccount.getBalance()));
        }
    }

    /**
     * Delete account
     */
    public void rm(String accountNumber) {
        Account account = getAccount(accountNumber);

        if (account.getBalance() != 0) {
            throw new IllegalArgumentException("Cannot delete account with non-zero balance");
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
}
