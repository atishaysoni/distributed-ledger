package com.atishaysoni.ledger.service;

import com.atishaysoni.ledger.model.Account;
import com.atishaysoni.ledger.repository.AccountRepository;
import com.atishaysoni.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransactionServiceStressTest {

    @Autowired private TransactionService transactionService;
    @Autowired private AccountService accountService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void heavyConcurrentTrafficOnTwoAccountsOnly() throws InterruptedException {
        Account a = accountService.create("a", new BigDecimal("100000.00"));
        Account b = accountService.create("b", new BigDecimal("100000.00"));

        BigDecimal startingTotal = a.getBalance().add(b.getBalance());

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(2000);
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < 2000; i++) {
            final boolean aToB = i % 2 == 0;
            pool.submit(() -> {
                try {
                    UUID from = aToB ? a.getId() : b.getId();
                    UUID to   = aToB ? b.getId() : a.getId();
                    transactionService.transfer(from, to, new BigDecimal("10.00"), UUID.randomUUID().toString());
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        BigDecimal endingTotal = accountService.get(a.getId()).getBalance()
                .add(accountService.get(b.getId()).getBalance());

        System.out.println("starting: " + startingTotal + " ending: " + endingTotal + " errors: " + errors.get());
        assertThat(endingTotal).isEqualByComparingTo(startingTotal);
    }
}