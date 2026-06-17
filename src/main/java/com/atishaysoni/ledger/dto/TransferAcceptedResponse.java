package com.atishaysoni.ledger.dto;

public class TransferAcceptedResponse {

    private final String idempotencyKey;
    private final String status;

    public TransferAcceptedResponse(String idempotencyKey, String status) {
        this.idempotencyKey = idempotencyKey;
        this.status = status;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
}