package com.atishaysoni.ledger.dto;

import com.atishaysoni.ledger.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionResponse {

    private final UUID id;
    private final UUID fromAccountId;
    private final UUID toAccountId;
    private final BigDecimal amount;
    private final TransactionStatus status;
    private final String idempotencyKey;
    private final LocalDateTime createdAt;
    private final LocalDateTime processedAt;

    public TransactionResponse(UUID id, UUID fromAccountId, UUID toAccountId, BigDecimal amount,
                                TransactionStatus status, String idempotencyKey,
                                LocalDateTime createdAt, LocalDateTime processedAt) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public UUID getId() { return id; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}