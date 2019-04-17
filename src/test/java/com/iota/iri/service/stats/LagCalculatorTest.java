package com.iota.iri.service.stats;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionTrits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import com.iota.iri.utils.Converter;
import com.iota.iri.utils.dag.RecentTransactionsGetter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class LagCalculatorTest {

    private final TemporaryFolder dbFolder = new TemporaryFolder();
    private final TemporaryFolder logFolder = new TemporaryFolder();
    private Tangle tangle;
    private RecentTransactionsGetter recentTransactionsGetter;

    @After
    public void tearDown() throws Exception {
        tangle.shutdown();
        dbFolder.delete();
    }

    @Before
    public void setUp() throws Exception {
        tangle = new Tangle();
        dbFolder.create();
        logFolder.create();
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(dbFolder.getRoot().getAbsolutePath(),
                logFolder.getRoot().getAbsolutePath(), 1000));
        tangle.init();

        recentTransactionsGetter = Mockito.mock(RecentTransactionsGetter.class);
    }

    @Test
    public void returnsNegativeResultWhenThereAreZeroRecentTransactions() throws Exception {
        Collection<Hash> emptyList = new ArrayList<Hash>();
        Mockito.when(recentTransactionsGetter.getRecentTransactions(Mockito.anyInt()))
            .thenReturn(emptyList);
        LagCalculator lagCalculator = new LagCalculator(50, tangle, recentTransactionsGetter);
        
        long medianLag = lagCalculator.getMedianArrivalLag();

        Assert.assertTrue(medianLag < 0);
    }

    @Test
    public void returnsCorrectMedianForSingleTx() throws Exception {
        long arrivalTime = 6;
        long attachmentTime = 3;

        TransactionViewModel txVM = createRandomTransaction(arrivalTime, attachmentTime);
        txVM.store(tangle);

        Mockito.when(recentTransactionsGetter.getRecentTransactions(Mockito.anyInt()))
            .thenReturn(Arrays.asList(txVM.getHash()));

        LagCalculator lagCalculator = new LagCalculator(50, tangle, recentTransactionsGetter);
        
        long medianLag = lagCalculator.getMedianArrivalLag();

        Assert.assertEquals(3, medianLag);
    }

    @Test
    public void returnsCorrectMedianForThreeTransactions() throws Exception {
        long[] arrivalTimes    = {1, 2, 3};
        long[] attachmentTimes = {0, 2, 5};

        List<Hash> txs = new ArrayList<>();
        for(int i=0; i < arrivalTimes.length; i++) {
            TransactionViewModel txVM = createRandomTransaction(arrivalTimes[i], attachmentTimes[i]);
            txVM.store(tangle);
            txs.add(txVM.getHash());
        }

        Mockito.when(recentTransactionsGetter.getRecentTransactions(Mockito.anyInt()))
            .thenReturn(txs);

        LagCalculator lagCalculator = new LagCalculator(50, tangle, recentTransactionsGetter);
        
        long medianLag = lagCalculator.getMedianArrivalLag();

        Assert.assertEquals(1, medianLag);
    }

    @Test
    public void returnsCorrectMedianForFourTransactions() throws Exception {
        long[] arrivalTimes    = {2, 4, 6, 8};
        long[] attachmentTimes = {0, 0, 0, 0};

        List<Hash> txs = new ArrayList<>();
        for(int i=0; i < arrivalTimes.length; i++) {
            TransactionViewModel txVM = createRandomTransaction(arrivalTimes[i], attachmentTimes[i]);
            txVM.store(tangle);
            txs.add(txVM.getHash());
        }

        Mockito.when(recentTransactionsGetter.getRecentTransactions(Mockito.anyInt()))
            .thenReturn(txs);

        LagCalculator lagCalculator = new LagCalculator(50, tangle, recentTransactionsGetter);
        
        long medianLag = lagCalculator.getMedianArrivalLag();

        Assert.assertEquals(5, medianLag);
    }

    private TransactionViewModel createRandomTransaction(long arrivalTime, long attachmentTime) {
        byte[] trits = getRandomTransactionTrits();
        Converter.copyTrits(attachmentTime, trits,
            TransactionViewModel.ATTACHMENT_TIMESTAMP_TRINARY_OFFSET,
            TransactionViewModel.ATTACHMENT_TIMESTAMP_TRINARY_SIZE);
        TransactionViewModel txVM = new TransactionViewModel(trits, getRandomTransactionHash());
        txVM.setArrivalTime(arrivalTime);
        return txVM;
    }
}