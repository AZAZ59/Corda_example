package com.template;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.template.payment.PaymentState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.NodeInfo;
//import org.crsh.vfs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.template.payment.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
public class ExampleApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Controller", "Network Map Service");

    static private final Logger logger = LoggerFactory.getLogger(ExampleApi.class);

    public ExampleApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @GET
    @Path("insert")
    @Produces(MediaType.APPLICATION_JSON)
    public Response insert(
            @QueryParam(value="accountNumber") String accountNumber,
            @QueryParam(value="countInCents") Integer countInCents,
            @QueryParam(value="date") String dateS,
            @QueryParam(value="idNumber") String idNumber,
            @QueryParam(value="type") String typeS,
            @QueryParam(value="party") String party
                          ){
        logger.info(LocalDate.now().toString());
        Type type=Type.valueOf(typeS);
        LocalDate date = LocalDate.parse(dateS);

        Party me = rpcOps.nodeInfo().getLegalIdentities().get(0);
        Party lender=rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(party));

        for(String flow:rpcOps.registeredFlows()){
            logger.info(flow);
        }
        //new IssuePaymentFlow("5",5, LocalDate.now(),"qwe",Type.A,lender)
        try {
            PaymentState st = new PaymentState(accountNumber,countInCents,date,idNumber,type,lender,me);
//            rpcOps.startTrackedFlowDynamic(IssuePaymentFlow.class, st).getReturnValue().get();
            FlowHandle<Void> x = rpcOps.startFlowDynamic(IssuePaymentFlow.class, st);
            logger.info(String.valueOf(x));
            CordaFuture<Void> y = x.getReturnValue();
            logger.info(String.valueOf(y));
            Void z = y.get();
            return Response.status(Response.Status.CREATED).entity("QWEE").build();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

//    public void test(){
//        rpcOps.startFlowDynamic(new IssuePaymentFlow(),)
//        return ;
//    }
    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<PaymentState>> getIOUs() {
        return rpcOps.vaultQuery(PaymentState.class).getStates();
    }


}