package com.atishaysoni.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID accountId, BigDecimal balance, BigDecimal requested) {
        super(String.format("account %s has %.4f, requested %.4f", accountId, balance, requested));
    }
}