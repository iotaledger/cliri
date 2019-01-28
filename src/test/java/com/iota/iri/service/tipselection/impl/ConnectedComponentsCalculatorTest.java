package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;

public class ConnectedComponentsCalculatorTest {

    private TemporaryFolder dbFolder;
    private TemporaryFolder logFolder;
    private Tangle tangle;

    private int maxTransaction = 100;

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
    }

    

    @Test
    public void returnsGenesisInSingleTxTangle() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                Collections.singleton(transaction.getHash()));

        Assert.assertTrue(recentTransactions.contains(transaction.getHash()));
        Assert.assertTrue(recentTransactions.contains(Hash.NULL_HASH));
    }

    @Test
    public void returnsGenesisTxInChain() throws Exception {
        int chainLength = maxTransaction - 10;
        List<Hash> transactions = makeChain(chainLength, Hash.NULL_HASH, 1000);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                Collections.singleton(transactions.get(transactions.size() - 1)));
        Assert.assertTrue(recentTransactions.contains(Hash.NULL_HASH));
        Assert.assertEquals(chainLength + 1, recentTransactions.size());
    }

    @Test
    public void doesntReturnsGenesisTxInChain() throws Exception {
        List<Hash> transactions = makeChain(maxTransaction + 1, Hash.NULL_HASH, 1000);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                Collections.singleton(transactions.get(transactions.size() - 1)));
        Assert.assertFalse(recentTransactions.contains(Hash.NULL_HASH));
    }

    @Test
    public void allTipsAndGenesisReturnedForStarAroundGenesis() throws Exception {
        List<Hash> transactions = makeStar(maxTransaction - 1, Hash.NULL_HASH, 1000);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(transactions);
        Assert.assertTrue(recentTransactions.containsAll(transactions));
        Assert.assertTrue(recentTransactions.contains(Hash.NULL_HASH));
    }

    @Test
    public void oldTipsDroppedWhenStarAroundGenesisIsTooBig() throws Exception {
        List<Hash> transactions = makeStar(maxTransaction + 1, Hash.NULL_HASH, 1000);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(
                transactions);
        Assert.assertFalse(recentTransactions.contains(Hash.NULL_HASH));
        Assert.assertFalse(recentTransactions.containsAll(transactions)); // there exists one tip not in list
    }

    @Test
    public void mostRecentTipsChosenFromStarAroundGenesisAndChain() throws Exception {
        long incr = 1000;

        final int starAmount = maxTransaction  / 10;
        //star
        List<Hash> starTransactions = makeStar(starAmount, Hash.NULL_HASH, incr);

        //chain
        final int chainLength = maxTransaction  - 1;
        List<Hash> chainTransactions = makeChain(chainLength, Hash.NULL_HASH, incr + starAmount);

        List<Hash> allTips = new ArrayList<>(starTransactions);
        allTips.add(chainTransactions.get(chainLength - 1));

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Hash> recentTransactions = connectedComponentsCalculator.findNMostRecentTransactions(allTips);

        Assert.assertTrue(recentTransactions.containsAll(chainTransactions)); // main chain is present
        Assert.assertTrue(recentTransactions.contains(starTransactions.get(starAmount - 1))); // only the newest star tip appears
        Assert.assertFalse(recentTransactions.contains(starTransactions.get(starAmount - 2))); // only the newest star tip appears


        Assert.assertFalse(recentTransactions.contains(Hash.NULL_HASH));
    }

    private List<Hash> makeChain(int length, Hash tip, long startArrivalTime) throws Exception {
        List<Hash> chain = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(tip, tip), getRandomTransactionHash());
            newTip.setArrivalTime(startArrivalTime++);
            newTip.store(tangle);
            tip = newTip.getHash();
            chain.add(tip);
        }

        return chain;
    }

    private List<Hash> makeStar(int length, Hash tip, long startArrivalTime) throws Exception {
        List<Hash> star = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(tip, tip), getRandomTransactionHash());
            newTip.setArrivalTime(startArrivalTime++);
            newTip.store(tangle);
            star.add(newTip.getHash());
        }

        return star;
    }
}
