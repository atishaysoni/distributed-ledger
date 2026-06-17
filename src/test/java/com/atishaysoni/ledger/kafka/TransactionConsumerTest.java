package com.atishaysoni.ledger.kafka;

import com.atishaysoni.ledger.model.Account;
import com.atishaysoni.ledger.repository.AccountRepository;
import com.atishaysoni.ledger.repository.TransactionRepository;
import com.atishaysoni.ledger.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(
        partitions = 1,
        topics = {"transactions", "transactions.dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class TransactionConsumerTest {

    @Autowired
    private TransactionProducer producer;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void consume_processesTransfer() throws InterruptedException {
        Account alice = accountService.create("alice", new BigDecimal("500.00"));
        Account bob   = accountService.create("bob",   new BigDecimal("0.00"));

        producer.publish(new TransactionEvent(
                alice.getId(), bob.getId(), new BigDecimal("100.00"), "kafka-test-1"
        ));

        Thread.sleep(3000);

        assertThat(accountService.get(alice.getId()).getBalance())
                .isEqualByComparingTo("400.00");
        assertThat(accountService.get(bob.getId()).getBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void consume_insufficientFunds_sentToDlt() throws InterruptedException {
        Account alice = accountService.create("alice", new BigDecimal("10.00"));
        Account bob   = accountService.create("bob",   new BigDecimal("0.00"));

        producer.publish(new TransactionEvent(
                alice.getId(), bob.getId(), new BigDecimal("500.00"), "kafka-test-2"
        ));

        Thread.sleep(3000);

        assertThat(accountService.get(alice.getId()).getBalance())
                .isEqualByComparingTo("10.00");
    }
}