package com.atishaysoni.ledger.controller;

import com.atishaysoni.ledger.dto.AccountResponse;
import com.atishaysoni.ledger.dto.CreateAccountRequest;
import com.atishaysoni.ledger.model.Account;
import com.atishaysoni.ledger.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request.getOwner(), request.getInitialBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(accountService.get(id)));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getOwner(),
                account.getBalance(), account.getCreatedAt());
    }
}