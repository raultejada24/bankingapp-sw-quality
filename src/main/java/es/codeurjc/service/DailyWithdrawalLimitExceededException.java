package es.codeurjc.service;

/**
 * Exception thrown when the daily withdrawal limit is exceeded.
 */
public class DailyWithdrawalLimitExceededException extends RuntimeException {
    
    public DailyWithdrawalLimitExceededException(String message) {
        super(message);
    }
    
    public DailyWithdrawalLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
