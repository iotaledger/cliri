package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.iota.iri.controllers.TransactionViewModelTest.*;

public class ConnectedComponentsCalculatorTest {

    private TemporaryFolder dbFolder;
    private TemporaryFolder logFolder;
    private Tangle tangle;
    private TipsViewModel tipsViewModel;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void tearDown() throws Exception {
        tangle.shutdown();
        dbFolder.delete();
    }

    @Before
    public void setUp() throws Exception {
        dbFolder = new TemporaryFolder();
        logFolder = new TemporaryFolder();
        tangle = new Tangle();
        dbFolder.create();
        logFolder.create();
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(dbFolder.getRoot().getAbsolutePath(),
                logFolder.getRoot().getAbsolutePath(), 1000));
        tangle.init();

        tipsViewModel = Mockito.mock(TipsViewModel.class);
    }

    //hairy genesis
    //triangle

    @Test
    public void returnsGenesisInSingleTxTangle() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        Mockito.when(tipsViewModel.getRandomSolidTipHash()).thenReturn(transaction.getHash());

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                Collections.singleton(tipsViewModel.getRandomSolidTipHash()));

        Assert.assertTrue(recentTransactions.contains(transaction.getHash()));
        Assert.assertTrue(recentTransactions.contains(Hash.NULL_HASH));

    }

    @Test
    public void returnsGenesisTxInChain() throws Exception {

        long incr = 1000;
        final int chainLength = ConnectedComponentsCalculator.N - 10;
        List<TransactionViewModel> transactions = new ArrayList<TransactionViewModel>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        for (int i = 0; i < chainLength; i++) {
            Hash prevTxHash = transactions.get(transactions.size() - 1).getHash();
            transactions.add(new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(prevTxHash, prevTxHash), getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactions) {
            transaction.setArrivalTime(incr++);
            transaction.store(tangle);
        }

        Mockito.when(tipsViewModel.getRandomSolidTipHash()).thenReturn(transactions.get(transactions.size() - 1).getHash());

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                Collections.singleton(tipsViewModel.getRandomSolidTipHash()));
        Assert.assertTrue(recentTransactions.contains(Hash.NULL_HASH));
    }

    @Test
    public void doesntReturnsGenesisTxInChain() throws Exception {
        long incr = 1000;

        final int chainLength = ConnectedComponentsCalculator.N + 1;
        List<TransactionViewModel> transactions = new ArrayList<TransactionViewModel>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        for (int i = 0; i < chainLength; i++) {
            Hash prevTxHash = transactions.get(transactions.size() - 1).getHash();
            transactions.add(new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(prevTxHash, prevTxHash), getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactions) {
            transaction.setArrivalTime(incr++);
            transaction.store(tangle);
        }

        Mockito.when(tipsViewModel.getRandomSolidTipHash()).thenReturn(transactions.get(transactions.size() - 1).getHash());

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                Collections.singleton(tipsViewModel.getRandomSolidTipHash()));
        Assert.assertFalse(recentTransactions.contains(Hash.NULL_HASH));
    }

    @Test
    public void starAroundGenesis() throws Exception {
        long incr = 1000;

        final int amount = ConnectedComponentsCalculator.N - 1;
        List<TransactionViewModel> transactions = new ArrayList<TransactionViewModel>();

        for (int i = 0; i < amount; i++) {
            transactions.add(new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                    Hash.NULL_HASH,
                    Hash.NULL_HASH),
                    getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactions) {
            transaction.setArrivalTime(incr++);
            transaction.store(tangle);
        }

        Collection<Hash> transactionHashes = transactions.stream().map(TransactionViewModel::getHash).collect(Collectors.toList());

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                transactionHashes);
        Assert.assertTrue(recentTransactions.containsAll(transactionHashes));
        Assert.assertTrue(recentTransactions.contains(Hash.NULL_HASH));

    }

    @Test
    public void starAroundGenesisTooBig() throws Exception {
        long incr = 1000;

        final int amount = ConnectedComponentsCalculator.N + 1;
        List<TransactionViewModel> transactions = new ArrayList<TransactionViewModel>();

        for (int i = 0; i < amount; i++) {
            transactions.add(new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                    Hash.NULL_HASH,
                    Hash.NULL_HASH),
                    getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactions) {
            transaction.setArrivalTime(incr++);
            transaction.store(tangle);
        }

        Collection<Hash> transactionHashes = transactions.stream().map(TransactionViewModel::getHash).collect(Collectors.toList());

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                transactionHashes);
        Assert.assertFalse(recentTransactions.contains(Hash.NULL_HASH));
        Assert.assertFalse(recentTransactions.containsAll(transactionHashes)); // there exists one tip not in list

    }

    @Test
    public void starAroundGenesisAndChain() throws Exception {
        long incr = 1000;

        final int amount = ConnectedComponentsCalculator.N / 10;
        //star
        List<TransactionViewModel> transactions = new ArrayList<TransactionViewModel>();

        for (int i = 0; i < amount; i++) {
            transactions.add(new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                    Hash.NULL_HASH,
                    Hash.NULL_HASH),
                    getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactions) {
            transaction.setArrivalTime(incr++);
            transaction.store(tangle);
        }

        //chain
        final int chainLength = ConnectedComponentsCalculator.N - 1;
        List<TransactionViewModel> transactionChain = new ArrayList<TransactionViewModel>();

        transactionChain.add(new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash()));

        for (int i = 0; i < chainLength; i++) {
            Hash prevTxHash = transactionChain.get(transactionChain.size() - 1).getHash();
            transactionChain.add(new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(prevTxHash, prevTxHash), getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactionChain) {
            transaction.setArrivalTime(incr++);
            transaction.store(tangle);
        }

        List<Hash> transactionHashes = transactions.stream().map(TransactionViewModel::getHash).collect(Collectors.toList());
        transactionHashes.add(transactionChain.get(transactionChain.size() - 1).getHash());

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                transactionHashes);

        Assert.assertTrue(recentTransactions.containsAll(
                transactionChain.stream().map(TransactionViewModel::getHash).collect(Collectors.toList()))); // there exists one tip not in list
        Assert.assertFalse(recentTransactions.containsAll(transactionHashes)); // there exists one tip not in list
        Assert.assertFalse(recentTransactions.contains(Hash.NULL_HASH));

    }
}
