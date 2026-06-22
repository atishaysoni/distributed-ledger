package com.atishaysoni.ledger.kafka;

import com.atishaysoni.ledger.exception.AccountNotFoundException;
import com.atishaysoni.ledger.exception.InsufficientFundsException;
import com.atishaysoni.ledger.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);
    private static final String DLT = "transactions.dlt";

    private final TransactionService transactionService;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionConsumer(TransactionService transactionService,
                                KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.transactionService = transactionService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "transactions", groupId = "ledger-consumer", concurrency = "3")
    public void consume(TransactionEvent event) {
        try {
            transactionService.transfer(
                    event.getFromAccountId(),
                    event.getToAccountId(),
                    event.getAmount(),
                    event.getIdempotencyKey()
            );
        } catch (InsufficientFundsException | AccountNotFoundException e) {
            log.warn("sending to dlt: {} — {}", event.getIdempotencyKey(), e.getMessage());
            kafkaTemplate.send(DLT, event.getIdempotencyKey(), event);
        }
    }
}