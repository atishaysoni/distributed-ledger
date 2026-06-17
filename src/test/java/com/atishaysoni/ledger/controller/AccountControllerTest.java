package com.atishaysoni.ledger.controller;

import com.atishaysoni.ledger.dto.AccountResponse;
import com.atishaysoni.ledger.dto.CreateAccountRequest;
import com.atishaysoni.ledger.dto.ErrorResponse;
import com.atishaysoni.ledger.repository.AccountRepository;
import com.atishaysoni.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AccountControllerTest {

    @Autowired
    private TestRestTemplate rest;

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
    void create_returnsCreatedAccount() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setOwner("alice");
        request.setInitialBalance(new BigDecimal("250.00"));

        ResponseEntity<AccountResponse> response =
                rest.postForEntity("/api/v1/accounts", request, AccountResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getOwner()).isEqualTo("alice");
        assertThat(response.getBody().getBalance()).isEqualByComparingTo("250.00");
    }

    @Test
    void create_blankOwner_returnsBadRequest() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setOwner("");
        request.setInitialBalance(new BigDecimal("100.00"));

        ResponseEntity<ErrorResponse> response =
                rest.postForEntity("/api/v1/accounts", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void get_unknownAccount_returnsNotFound() {
        ResponseEntity<ErrorResponse> response =
                rest.getForEntity("/api/v1/accounts/" + UUID.randomUUID(), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}