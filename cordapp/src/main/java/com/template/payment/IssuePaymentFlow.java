package com.template.payment;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.template.Type;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

//import static net.corda.docs.java.tutorial.helloworld.TemplateContract.TEMPLATE_CONTRACT_ID;

@InitiatingFlow
@StartableByRPC
public class IssuePaymentFlow extends FlowLogic<Void> {
    private final String accountNumber;
    private final Integer countInCents;
    private final LocalDate date;
    private final String idNumber;
    private final Type type;
    private final Party otherParty;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public IssuePaymentFlow(PaymentState state) {
        this.accountNumber = state.getAccountNumber();
        this.countInCents = state.getCountInCents();
        this.date = state.getDate();
        this.idNumber = state.getIdNumber();
        this.type = state.getType();
        this.otherParty= state.getBorrower();
    }

    public IssuePaymentFlow(String accountNumber, Integer countInCents, LocalDate date, String idNumber, Type type, Party otherParty) {
        this.accountNumber = accountNumber;
        this.countInCents = countInCents;
        this.date = date;
        this.idNumber = idNumber;
        this.type = type;
        this.otherParty= otherParty;
    }


    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create the transaction components.
        PaymentState outputState = new PaymentState(accountNumber,countInCents,date,idNumber,type, getOurIdentity(), otherParty);
        String outContract = PaymaneContract.class.getName();
        StateAndContract outputStateAndContract=new StateAndContract(outputState,outContract);

        List<PublicKey> requiredSigners= ImmutableList.of(getOurIdentity().getOwningKey(),otherParty.getOwningKey());
        Command cmd = new Command<>(new PaymaneContract.Create(),requiredSigners);

        // We create a transaction builder and add the components.
        final TransactionBuilder txBuilder = new TransactionBuilder(notary);
        txBuilder.withItems(outputStateAndContract,cmd);
        txBuilder.verify(getServiceHub());

        // Signing the transaction.
        final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        FlowSession otherPartySession = initiateFlow(otherParty);

        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(signedTx,ImmutableList.of(otherPartySession),CollectSignaturesFlow.tracker())
        );

        // Finalising the transaction.
        subFlow(new FinalityFlow(fullySignedTx));

        return null;
    }
}

@InitiatedBy(IssuePaymentFlow.class)
class IssuePaymentFlowResponder extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    public IssuePaymentFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }


    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        class SignTxFlow extends SignTransactionFlow{
            public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                requireThat(requirements -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    requirements.using("must be IOU ",output instanceof PaymentState);
                    PaymentState iou = (PaymentState) output;
                    requirements.using("IUO <1000",iou.getCountInCents()<1000);
                    return null;
                });
            }
        }
        subFlow(new SignTxFlow(otherPartySession,SignTransactionFlow.Companion.tracker()));
        return null;
    }
}