package com.atishaysoni.ledger.controller;

import com.atishaysoni.ledger.dto.TransferAcceptedResponse;
import com.atishaysoni.ledger.dto.TransferRequest;
import com.atishaysoni.ledger.repository.AccountRepository;
import com.atishaysoni.ledger.repository.TransactionRepository;
import com.atishaysoni.ledger.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TransactionControllerTest {

    @Autowired
    private TestRestTemplate rest;

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
    void submit_returnsAccepted() {
        var alice = accountService.create("alice", new BigDecimal("500.00"));
        var bob = accountService.create("bob", new BigDecimal("0.00"));

        TransferRequest request = new TransferRequest();
        request.setFromAccountId(alice.getId());
        request.setToAccountId(bob.getId());
        request.setAmount(new BigDecimal("100.00"));

        ResponseEntity<TransferAcceptedResponse> response =
                rest.postForEntity("/api/v1/transactions", request, TransferAcceptedResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getStatus()).isEqualTo("PENDING");
    }
}