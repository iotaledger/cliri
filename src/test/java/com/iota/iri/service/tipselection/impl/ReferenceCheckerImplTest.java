package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.service.tipselection.ReferenceChecker;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.List;

import static com.iota.iri.controllers.TransactionViewModelTest.*;

public class ReferenceCheckerImplTest {
    private static final TemporaryFolder dbFolder = new TemporaryFolder();
    private static final TemporaryFolder logFolder = new TemporaryFolder();
    private static Tangle tangle;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

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
    public void testReferenceCheckerReturnsTrueForSameTransaction() throws Exception {
        TransactionViewModel transaction;
        transaction = new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash());

        transaction.store(tangle);

        ReferenceChecker referenceChecker = new ReferenceCheckerImpl(tangle);

        Assert.assertTrue(referenceChecker.doesReference(transaction.getHash(), transaction.getHash()));
    }

    @Test
    public void testReferenceCheckerReturnsFalseForTwoUnrelatedTxs() throws Exception {
        List<TransactionViewModel> transactions = new ArrayList<>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        ReferenceChecker referenceChecker = new ReferenceCheckerImpl(tangle);

        Assert.assertFalse(referenceChecker.doesReference(transactions.get(0).getHash(), transactions.get(1).getHash()));
        Assert.assertFalse(referenceChecker.doesReference(transactions.get(1).getHash(), transactions.get(0).getHash()));
    }

    @Test
    public void testReferenceCheckerReturnsTrueForDirectApprovers() throws Exception {
        List<TransactionViewModel> transactions = new ArrayList<>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        transactions.add(new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                transactions.get(0).getHash(),
                transactions.get(1).getHash()),
                getRandomTransactionHash()));

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        ReferenceChecker referenceChecker = new ReferenceCheckerImpl(tangle);

        Assert.assertTrue(referenceChecker.doesReference(transactions.get(2).getHash(), transactions.get(1).getHash()));
        Assert.assertTrue(referenceChecker.doesReference(transactions.get(2).getHash(), transactions.get(0).getHash()));
    }

    @Test
    public void testReferenceCheckerReturnsFalseForDirectApproversWhenReversingDirection() throws Exception {
        List<TransactionViewModel> transactions = new ArrayList<>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        transactions.add(new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                transactions.get(0).getHash(),
                transactions.get(1).getHash()),
                getRandomTransactionHash()));

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        ReferenceChecker referenceChecker = new ReferenceCheckerImpl(tangle);

        Assert.assertFalse(referenceChecker.doesReference(transactions.get(0).getHash(), transactions.get(2).getHash()));
        Assert.assertFalse(referenceChecker.doesReference(transactions.get(1).getHash(), transactions.get(2).getHash()));
    }

    @Test
    public void testReferenceCheckerThrowsExceptionWhenReferenceTransactionDoesNotExist() throws Exception {
        List<TransactionViewModel> transactions = new ArrayList<>();
        List<TransactionViewModel> nonExistentTransactions = new ArrayList<>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        nonExistentTransactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        exception.expect(Exception.class);
        new ReferenceCheckerImpl(tangle).doesReference(nonExistentTransactions.get(0).getHash(), transactions.get(0).getHash());
    }

    @Test
    public void testReferenceCheckerThrowsExceptionWhenTargetTransactionDoesNotExist() throws Exception {
        List<TransactionViewModel> transactions = new ArrayList<>();
        List<TransactionViewModel> nonExistentTransactions = new ArrayList<>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        nonExistentTransactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        nonExistentTransactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        exception.expect(Exception.class);
        new ReferenceCheckerImpl(tangle).doesReference(transactions.get(0).getHash(), nonExistentTransactions.get(0).getHash());
    }

    @Test
    public void testReferenceCheckerTrueForChainOfApprovers() throws Exception {
        List<TransactionViewModel> transactions = new ArrayList<>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        for (int i = 0; i < 10; i++) {
            TransactionViewModel lastTx = transactions.get(transactions.size() - 1);
            transactions.add(new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                    lastTx.getHash(),
                    lastTx.getHash()),
                    getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        ReferenceChecker referenceChecker = new ReferenceCheckerImpl(tangle);
        TransactionViewModel lastTx = transactions.get(transactions.size() - 1);
        TransactionViewModel firstTx = transactions.get(0);

        Assert.assertTrue(referenceChecker.doesReference(lastTx.getHash(), firstTx.getHash()));
    }
}