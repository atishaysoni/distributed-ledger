package com.atishaysoni.ledger.service;

import com.atishaysoni.ledger.exception.AccountNotFoundException;
import com.atishaysoni.ledger.model.Account;
import com.atishaysoni.ledger.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account create(String owner, BigDecimal initialBalance) {
        return accountRepository.save(new Account(owner, initialBalance));
    }

    public Account get(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}