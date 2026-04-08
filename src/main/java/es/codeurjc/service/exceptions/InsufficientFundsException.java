package es.codeurjc.service.exceptions;

/**
 * Exception thrown when an account has insufficient funds for a transaction.
 * Issue 13: Use custom exceptions instead of generic IllegalArgumentException
 */
public class InsufficientFundsException extends RuntimeException {
    private final double balance;
    private final double requestedAmount;

    public InsufficientFundsException(double balance, double requestedAmount) {
        super("Insufficient funds. Current balance: " + balance + ", Requested: " + requestedAmount);
        this.balance = balance;
        this.requestedAmount = requestedAmount;
    }

    public double getBalance() {
        return balance;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }
}
