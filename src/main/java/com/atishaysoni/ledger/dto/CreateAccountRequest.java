package com.atishaysoni.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotBlank
    private String owner;

    @NotNull
    @PositiveOrZero
    private BigDecimal initialBalance;

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}