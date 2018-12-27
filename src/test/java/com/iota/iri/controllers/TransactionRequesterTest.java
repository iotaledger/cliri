package com.iota.iri.controllers;

import com.iota.iri.model.Hash;
import com.iota.iri.network.TransactionRequester;
import com.iota.iri.storage.Tangle;
import com.iota.iri.zmq.MessageQ;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by paul on 5/2/17.
 */
public class TransactionRequesterTest {
    private static Tangle tangle = new Tangle();
    private MessageQ mq;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void init() throws Exception {

    }

    @Test
    public void rescanTransactionsToRequest() throws Exception {

    }

    @Test
    public void getRequestedTransactions() throws Exception {

    }

    @Test
    public void numberOfTransactionsToRequest() throws Exception {

    }

    @Test
    public void clearTransactionRequest() throws Exception {

    }

    @Test
    public void requestTransaction() throws Exception {

    }

    @Test
    public void transactionToRequest() throws Exception {

    }

    @Test
    public void checkSolidity() throws Exception {

    }

    @Test
    public void instance() throws Exception {

    }

    @Test
    public void capacityLimited() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle, mq);
        int capacity = TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
        //fill tips list
        for (int i = 0; i < capacity * 2 ; i++) {
            Hash hash = TransactionViewModelTest.getRandomTransactionHash();
            txReq.requestTransaction(hash);
        }
        //check that limit wasn't breached
        assertEquals(capacity, txReq.numberOfTransactionsToRequest());
    }

    @Test
    public void queueIsEmptyAfterCallingClearQueue() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle, mq);

        //fill tips list
        for (int i = 0; i < 60; i++) {
            Hash hash = TransactionViewModelTest.getRandomTransactionHash();
            txReq.requestTransaction(hash);
        }

        assertEquals(60, txReq.numberOfTransactionsToRequest());

        txReq.clearQueue();

        assertEquals(0, txReq.numberOfTransactionsToRequest());
    }
}