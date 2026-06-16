package com.atishaysoni.ledger.service;

import com.atishaysoni.ledger.exception.AccountNotFoundException;
import com.atishaysoni.ledger.exception.InsufficientFundsException;
import com.atishaysoni.ledger.model.Account;
import com.atishaysoni.ledger.model.Transaction;
import com.atishaysoni.ledger.model.TransactionStatus;
import com.atishaysoni.ledger.repository.AccountRepository;
import com.atishaysoni.ledger.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository,
                               TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction transfer(UUID fromId, UUID toId, BigDecimal amount, String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // always acquire locks in UUID order to prevent deadlocks
        // if A->B and B->A both run concurrently, both will try to lock
        // the lower UUID first — so one waits instead of deadlocking
        UUID firstId  = fromId.compareTo(toId) <= 0 ? fromId : toId;
        UUID secondId = firstId.equals(fromId) ? toId : fromId;

        Account first  = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account from = first.getId().equals(fromId) ? first : second;
        Account to   = first.getId().equals(toId)   ? first : second;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromId, from.getBalance(), amount);
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        return transactionRepository.save(
                new Transaction(fromId, toId, amount, TransactionStatus.PROCESSED, idempotencyKey)
        );
    }
}