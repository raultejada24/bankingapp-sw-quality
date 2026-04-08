package es.codeurjc.service.exceptions;

/**
 * Exception thrown when an account is not found.
 * Issue 13: Use custom exceptions instead of generic IllegalArgumentException
 */
public class AccountNotFoundException extends RuntimeException {
    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
