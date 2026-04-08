package es.codeurjc.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object representing a monetary amount.
 * Issue 22: Replaces primitive double with Money Value Object
 * This ensures type safety and prevents mixing currencies or scales
 */
public class Money implements Serializable, Comparable<Money> {
    
    private static final String DEFAULT_CURRENCY = "EUR";
    private static final long serialVersionUID = 1L;
    
    private final double amount;
    private final String currency;
    
    /**
     * Create Money with default currency (EUR)
     */
    public Money(double amount) {
        this(amount, DEFAULT_CURRENCY);
    }
    
    /**
     * Create Money with specified currency
     */
    public Money(double amount, String currency) {
        if (amount < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        this.amount = Math.round(amount * 100.0) / 100.0; // Ensure 2 decimal places
        this.currency = currency.toUpperCase();
    }
    
    /**
     * Add money to this amount
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount + other.amount, this.currency);
    }
    
    /**
     * Subtract money from this amount
     */
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        double result = this.amount - other.amount;
        if (result < 0) {
            throw new IllegalArgumentException("Result would be negative");
        }
        return new Money(result, this.currency);
    }
    
    /**
     * Check if this money is sufficient for the given amount
     */
    public boolean isAtLeast(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount >= other.amount;
    }
    
    /**
     * Check if this money equals another
     */
    public boolean isSameAmount(Money other) {
        if (!this.currency.equals(other.currency)) {
            return false;
        }
        return Double.compare(this.amount, other.amount) == 0;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    @Override
    public int compareTo(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return Double.compare(this.amount, other.amount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Double.compare(money.amount, amount) == 0 &&
               Objects.equals(currency, money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return String.format("%.2f %s", amount, currency);
    }
}
