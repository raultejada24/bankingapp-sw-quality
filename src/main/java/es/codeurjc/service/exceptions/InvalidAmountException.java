package es.codeurjc.service.exceptions;

/**
 * Exception thrown when an invalid amount is provided for a banking operation.
 * Issue 13: Use custom exceptions instead of generic IllegalArgumentException
 */
public class InvalidAmountException extends RuntimeException {
    private final double amount;

    public InvalidAmountException(double amount) {
        super("Amount must be positive, but got: " + amount);
        this.amount = amount;
    }

    public InvalidAmountException(String message, double amount) {
        super(message);
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
}
