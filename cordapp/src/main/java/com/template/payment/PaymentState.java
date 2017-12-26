package com.template.payment;

import com.google.common.collect.ImmutableList;
import com.template.Type;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PaymentState implements ContractState {

    private final String accountNumber;
    private final Integer countInCents;
    private final LocalDate date;
    private final String idNumber;
    private final Type type;
    private final Party lender;  // -- отправитель
    private final Party borrower;// -- получатель
    public PaymentState(String accountNumber, Integer countInCents, LocalDate date, String idNumber, Type type, Party lender, Party borrower) {
        this.accountNumber = accountNumber;
        this.countInCents = countInCents;
        this.date = date;
        this.idNumber = idNumber;
        this.type = type;
        this.lender = lender;
        this.borrower = borrower;
    }


    public String getAccountNumber() {
        return accountNumber;
    }

    public Integer getCountInCents() {
        return countInCents;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public Type getType() {
        return type;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    /**
     * The public keys of the involved parties.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender,borrower);
    }
}