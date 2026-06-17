package com.atishaysoni.ledger.controller;

import com.atishaysoni.ledger.dto.ErrorResponse;
import com.atishaysoni.ledger.dto.TransactionResponse;
import com.atishaysoni.ledger.dto.TransferAcceptedResponse;
import com.atishaysoni.ledger.dto.TransferRequest;
import com.atishaysoni.ledger.kafka.TransactionEvent;
import com.atishaysoni.ledger.kafka.TransactionProducer;
import com.atishaysoni.ledger.model.Transaction;
import com.atishaysoni.ledger.repository.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionProducer producer;
    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionProducer producer,
                                  TransactionRepository transactionRepository) {
        this.producer = producer;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    public ResponseEntity<TransferAcceptedResponse> submit(@Valid @RequestBody TransferRequest request) {
        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        producer.publish(new TransactionEvent(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                idempotencyKey
        ));

        return ResponseEntity.accepted().body(new TransferAcceptedResponse(idempotencyKey, "PENDING"));
    }

    @GetMapping("/{idempotencyKey}")
    public ResponseEntity<Object> get(@PathVariable String idempotencyKey) {
        Optional<Transaction> transaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (transaction.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                    "not_found", "no transaction found for this key yet — it may still be processing"));
        }

        return ResponseEntity.ok(toResponse(transaction.get()));
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getStatus(), t.getIdempotencyKey(), t.getCreatedAt(), t.getProcessedAt());
    }
}