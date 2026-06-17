package com.atishaysoni.ledger.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionEvent {

    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private String idempotencyKey;

    public TransactionEvent() {}

    public TransactionEvent(UUID fromAccountId, UUID toAccountId,
                             BigDecimal amount, String idempotencyKey) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId()   { return toAccountId; }
    public BigDecimal getAmount()  { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
}