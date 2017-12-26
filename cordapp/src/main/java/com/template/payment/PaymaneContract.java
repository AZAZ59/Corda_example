package com.template.payment;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Define your contract here.
 */
public class PaymaneContract implements Contract {
    /**
     * A transaction is considered valid if the verify() function of the contract of each of the transaction's input
     * and output states does not throw an exception.
     */
    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<PaymaneContract.Create> command = requireSingleCommand(tx.getCommands(), PaymaneContract.Create.class);

        requireThat(check -> {
            check.using("No Input", tx.getInputs().isEmpty());
            check.using("One output", tx.getOutputs().size() == 1);

            final PaymentState out = tx.outputsOfType(PaymentState.class).get(0);
            final Party lender = out.getLender();
            final Party borrower = out.getBorrower();

            check.using("value >100 ", out.getCountInCents() > 100);
            check.using("lender != borrower", lender != borrower);

            final List<PublicKey> signers = command.getSigners();
            check.using("2 signers ", signers.size()==2);
            check.using("lender and borrower in signers", signers.containsAll(ImmutableList.of(borrower.getOwningKey(),lender.getOwningKey())));

            return null;
        });

    }

    public static class Create implements CommandData {

    }

}