package com.atishaysoni.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountResponse {

    private final UUID id;
    private final String owner;
    private final BigDecimal balance;
    private final LocalDateTime createdAt;

    public AccountResponse(UUID id, String owner, BigDecimal balance, LocalDateTime createdAt) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}