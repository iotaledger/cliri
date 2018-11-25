package com.iota.iri.network;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.zmq.MessageQ;
import com.iota.iri.storage.Tangle;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;

/**
 * Created by paul on 3/27/17.
 */
public class TransactionRequester {

    private static final Logger log = LoggerFactory.getLogger(TransactionRequester.class);
    private final MessageQ messageQ;
    private final Set<Hash> transactionsToRequest = new LinkedHashSet<>();

    public static final int MAX_TX_REQ_QUEUE_SIZE = 10000;

    private static double P_REMOVE_REQUEST;
    private static boolean initialized = false;
    private final SecureRandom random = new SecureRandom();

    private final Object syncObj = new Object();
    private final Tangle tangle;

    public TransactionRequester(Tangle tangle, MessageQ messageQ) {
        this.tangle = tangle;
        this.messageQ = messageQ;
    }

    public void init(double pRemoveRequest) {
        if(!initialized) {
            initialized = true;
            P_REMOVE_REQUEST = pRemoveRequest;
        }
    }

    public Hash[] getRequestedTransactions() {
        synchronized (syncObj) {
            return ArrayUtils.addAll(transactionsToRequest.toArray(new Hash[0]));
        }
    }

    public int numberOfTransactionsToRequest() {
        return transactionsToRequest.size();
    }

    public boolean clearTransactionRequest(Hash hash) {
        synchronized (syncObj) {
            return transactionsToRequest.remove(hash);
        }
    }

    public void requestTransaction(Hash hash) throws Exception {
        if (!hash.equals(Hash.NULL_HASH) && !TransactionViewModel.exists(tangle, hash)) {
            synchronized (syncObj) {
                if(!transactionsToRequestIsFull()) {
                        transactionsToRequest.add(hash);
                }
            }
        }
    }

    /**
     * This method allows to check if a transaction was requested by the TransactionRequester.
     *
     * It can for example be used to determine if a transaction that was received by the node was actively requested
     * while i.e. solidifying transactions or if a transaction arrived due to the gossip protocol.
     *
     * @param transactionHash hash of the transaction to check
     * @return true if the transaction is in the set of transactions to be requested and false otherwise
     */
    public boolean isTransactionRequested(Hash transactionHash) {
        return transactionsToRequest.contains(transactionHash);
    }

    private boolean transactionsToRequestIsFull() {
        return transactionsToRequest.size() >= TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
    }


    public Hash transactionToRequest() throws Exception {
        // determine the first hash in our set that needs to be processed
        Hash hash = null;
        synchronized (syncObj) {
            // repeat while we have transactions that shall be requested
            while (transactionsToRequest.size() != 0) {
                // remove the first item in our set for further examination
                Iterator<Hash> iterator = transactionsToRequest.iterator();
                hash = iterator.next();
                iterator.remove();

                // if we have received the transaction in the mean time ....
                if (TransactionViewModel.exists(tangle, hash)) {
                    // ... dump a log message ...
                    log.info("Removed existing tx from request list: " + hash);
                    messageQ.publish("rtl %s", hash);

                    // ... and continue to the next element in the set
                    continue;
                }

                // ... otherwise -> re-add it at the end of the set ...
                //
                // Note: we always have enough space since we removed the element before
                transactionsToRequest.add(hash);

                // ... and abort our loop to continue processing with the element we found
                break;
            }
        }

        // randomly drop transactions so we don't keep on asking for non-existent transactions forever
        if(random.nextDouble() < P_REMOVE_REQUEST) {
            synchronized (syncObj) {
                transactionsToRequest.remove(hash);
            }
        }

        // return our result
        return hash;
    }

}
