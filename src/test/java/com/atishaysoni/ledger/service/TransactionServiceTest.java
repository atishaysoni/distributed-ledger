package com.atishaysoni.ledger.service;

import com.atishaysoni.ledger.exception.InsufficientFundsException;
import com.atishaysoni.ledger.model.Account;
import com.atishaysoni.ledger.repository.AccountRepository;
import com.atishaysoni.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

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
    void transfer_debitsAndCreditsCorrectly() {
        Account alice = accountService.create("alice", new BigDecimal("500.00"));
        Account bob   = accountService.create("bob",   new BigDecimal("100.00"));

        transactionService.transfer(alice.getId(), bob.getId(), new BigDecimal("200.00"), "key-1");

        assertThat(accountService.get(alice.getId()).getBalance())
                .isEqualByComparingTo("300.00");
        assertThat(accountService.get(bob.getId()).getBalance())
                .isEqualByComparingTo("300.00");
    }

    @Test
    void transfer_insufficientFunds_throws() {
        Account alice = accountService.create("alice", new BigDecimal("50.00"));
        Account bob   = accountService.create("bob",   new BigDecimal("0.00"));

        assertThrows(InsufficientFundsException.class, () ->
                transactionService.transfer(alice.getId(), bob.getId(),
                        new BigDecimal("100.00"), "key-2"));
    }

    @Test
    void transfer_sameIdempotencyKey_processedOnce() {
        Account alice = accountService.create("alice", new BigDecimal("500.00"));
        Account bob   = accountService.create("bob",   new BigDecimal("0.00"));

        transactionService.transfer(alice.getId(), bob.getId(), new BigDecimal("100.00"), "key-3");
        transactionService.transfer(alice.getId(), bob.getId(), new BigDecimal("100.00"), "key-3");

        assertThat(accountService.get(alice.getId()).getBalance())
                .isEqualByComparingTo("400.00");
    }

    @Test
    void transfer_concurrent_noDoubleSpend() throws InterruptedException {
        Account alice = accountService.create("alice", new BigDecimal("1000.00"));
        Account bob   = accountService.create("bob",   new BigDecimal("0.00"));

        int threads = 50;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);
        AtomicInteger succeeded = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final String key = "concurrent-" + i;
            new Thread(() -> {
                try {
                    start.await();
                    transactionService.transfer(alice.getId(), bob.getId(),
                            new BigDecimal("100.00"), key);
                    succeeded.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await(30, TimeUnit.SECONDS);

        BigDecimal total = accountService.get(alice.getId()).getBalance()
                .add(accountService.get(bob.getId()).getBalance());

        assertThat(total).isEqualByComparingTo("1000.00");
        assertThat(succeeded.get()).isEqualTo(10);
    }
}