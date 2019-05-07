package com.iota.iri.service.stats;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;

public class TimeWindowedApproveeCounterTest {

    private static final Duration MIN_DURATION = Duration.ZERO;
    private static final Duration MAX_DURATION = Duration.ofSeconds(Long.MAX_VALUE);

    private static final TemporaryFolder dbFolder = new TemporaryFolder();
    private static final TemporaryFolder logFolder = new TemporaryFolder();
    private static Tangle tangle;

    @AfterClass
    public static void tearDown() throws Exception {
        tangle.shutdown();
        dbFolder.delete();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        tangle = new Tangle();
        dbFolder.create();
        logFolder.create();
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(
                dbFolder.getRoot().getAbsolutePath(), logFolder.getRoot().getAbsolutePath(),1000,
                Tangle.COLUMN_FAMILIES, Tangle.METADATA_COLUMN_FAMILY));
        tangle.init();
    }

    @Test
    public void testEmptyTangle() throws Exception {
        TimeWindowedApproveeCounter counter = new TimeWindowedApproveeCounter(tangle, MIN_DURATION, MAX_DURATION);

        final long count = counter.getCount(Instant.EPOCH, Hash.NULL_HASH, new HashSet<>());
        Assert.assertEquals(0, count);
    }

    private static List<TransactionViewModel> getTransactionChain(int count) {
        List<TransactionViewModel> transactions = new ArrayList<>();

        Hash lastTransactionHash = Hash.NULL_HASH;
        for (int i = 0; i < count; i++) {
            TransactionViewModel transactionViewModel = new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(lastTransactionHash, lastTransactionHash),
                    getRandomTransactionHash());
            transactions.add(transactionViewModel);

            lastTransactionHash = transactionViewModel.getHash();
        }

        return transactions;
    }

    @Test
    public void testSingleApproveeNotInTimeWindow() throws Exception {
        final Duration minTransactionAge = Duration.ofSeconds(2);
        final Duration maxTransactionAge = Duration.ofSeconds(5);

        TimeWindowedApproveeCounter counter = new TimeWindowedApproveeCounter(tangle, minTransactionAge,
                maxTransactionAge);

        List<TransactionViewModel> transactions = getTransactionChain(2);

        final Instant now = Instant.ofEpochSecond(10);
        transactions.get(0).setArrivalTime(0);
        transactions.get(1).setArrivalTime(now.getEpochSecond());

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        final long count = counter.getCount(now, transactions.get(1).getHash(), new HashSet<>());
        Assert.assertEquals(0, count);
    }

    @Test
    public void testTimeWindowTransactionsInChain() throws Exception {
        final Duration minTransactionAge = Duration.ofSeconds(2);
        final Duration maxTransactionAge = Duration.ofSeconds(5);

        TimeWindowedApproveeCounter counter = new TimeWindowedApproveeCounter(tangle, minTransactionAge,
                maxTransactionAge);

        List<TransactionViewModel> transactions = getTransactionChain(10);

        // give transactions ascending times
        long time = 0;
        for (TransactionViewModel transaction : transactions) {
            transaction.setArrivalTime(time++);
            transaction.store(tangle);
        }

        final long count = counter.getCount(Instant.ofEpochSecond(10),
                transactions.get(transactions.size() - 1).getHash(), new HashSet<>());
        Assert.assertEquals(4, count);
    }

    @Test
    public void testDoNotCountProcessedTransactions() throws Exception {
        TimeWindowedApproveeCounter counter = new TimeWindowedApproveeCounter(tangle, MIN_DURATION, MAX_DURATION);

        List<TransactionViewModel> transactions = getTransactionChain(10);

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        // add all transaction hashes to processed
        HashSet<Hash> processedTransactions = new HashSet<>();
        for (TransactionViewModel transaction : transactions) {
            processedTransactions.add(transaction.getHash());
        }

        final long count = counter.getCount(Instant.EPOCH, transactions.get(transactions.size() - 1).getHash(),
                processedTransactions);
        Assert.assertEquals(0, count);
    }
}