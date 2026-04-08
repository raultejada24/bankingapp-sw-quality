package es.codeurjc.service.exceptions;

/**
 * Exception thrown when a transaction limit is exceeded.
 * Issue 13: Use custom exceptions instead of generic IllegalArgumentException
 */
public class LimitExceededException extends RuntimeException {
    private final double limit;
    private final double amount;
    private final String operationType;

    public LimitExceededException(String operationType, double amount, double limit) {
        super(operationType + " amount (" + amount + ") exceeds maximum limit (" + limit + ")");
        this.operationType = operationType;
        this.amount = amount;
        this.limit = limit;
    }

    public double getLimit() {
        return limit;
    }

    public double getAmount() {
        return amount;
    }

    public String getOperationType() {
        return operationType;
    }
}
