package com.payledger.exception;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String sourceCurrency, String targetCurrency) {
        super("Currency mismatch: " + sourceCurrency + " vs " + targetCurrency);
    }
}
