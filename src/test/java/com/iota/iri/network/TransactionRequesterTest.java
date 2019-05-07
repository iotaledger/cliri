package com.iota.iri.network;

import com.iota.iri.model.Hash;
import com.iota.iri.service.snapshot.SnapshotProvider;
import com.iota.iri.storage.Tangle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.iota.iri.TransactionTestUtils.getRandomTransactionHash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class TransactionRequesterTest {
    private static Tangle tangle = new Tangle();
    private static SnapshotProvider snapshotProvider;

    @Before
    public void setUp() throws Exception {
        snapshotProvider = Mockito.mock(SnapshotProvider.class);
    }

    @After
    public void tearDown() throws Exception {
        snapshotProvider.shutdown();
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
    public void popEldestTransactionToRequest() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle);
        // Add some Txs to the pool and see if the method pops the eldest one
        Hash eldest = getRandomTransactionHash();
        txReq.requestTransaction(eldest);
        txReq.requestTransaction(getRandomTransactionHash());
        txReq.requestTransaction(getRandomTransactionHash());
        txReq.requestTransaction(getRandomTransactionHash());

        txReq.popEldestTransactionToRequest();
        // Check that the transaction is there no more
        assertFalse(txReq.isTransactionRequested(eldest));
    }

    @Test
    public void transactionRequestedFreshness() throws Exception {
        // Add some Txs to the pool and see if the method pops the eldest one
        List<Hash> eldest = new ArrayList<Hash>(Arrays.asList(
                getRandomTransactionHash(),
                getRandomTransactionHash(),
                getRandomTransactionHash()
        ));
        TransactionRequester txReq = new TransactionRequester(tangle);
        int capacity = TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
        //fill tips list
        for (int i = 0; i < 3; i++) {
            txReq.requestTransaction(eldest.get(i));
        }
        for (int i = 0; i < capacity; i++) {
            Hash hash = getRandomTransactionHash();
            txReq.requestTransaction(hash);
        }

        //check that limit wasn't breached
        assertEquals("Queue capacity breached!!", capacity, txReq.numberOfTransactionsToRequest());
        // None of the eldest transactions should be in the pool
        for (int i = 0; i < 3; i++) {
            assertFalse("Old transaction has been requested", txReq.isTransactionRequested(eldest.get(i)));
        }
    }

    @Test
    public void nonMilestoneCapacityLimited() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle);
        int capacity = TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
        //fill tips list
        for (int i = 0; i < capacity * 2 ; i++) {
            Hash hash = getRandomTransactionHash();
            txReq.requestTransaction(hash);
        }
        //check that limit wasn't breached
        assertEquals(capacity, txReq.numberOfTransactionsToRequest());
    }
}
