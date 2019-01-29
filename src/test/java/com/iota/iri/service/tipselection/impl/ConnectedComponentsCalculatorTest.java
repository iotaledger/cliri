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

import java.util.*;

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

        final int starAmount = maxTransaction / 10;
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

    @Test
    public void selectTipOneConnectedComponentWithOneTip() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Set<Hash>> CC = new ArrayList<>(Collections.singleton(Collections.singleton(transaction.getHash())));

        Hash tip = connectedComponentsCalculator.randomlySelectTipFromLargestConnectedComponent(CC, Collections.singleton(transaction.getHash()));
        Assert.assertEquals(transaction.getHash(), tip);
    }

    @Test(expected = IllegalStateException.class)
    public void failOnEmptyConnectedComponentIntersection() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        TransactionViewModel falseTip = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        falseTip.store(tangle);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Set<Hash>> CC = new ArrayList<>(Collections.singleton(Collections.singleton(transaction.getHash())));

        //should throw IllegalStateException
        Hash tip = connectedComponentsCalculator.randomlySelectTipFromLargestConnectedComponent(CC,
                Collections.singleton(falseTip.getHash()));
    }

    @Test
    public void selectTipFromLargestConnectedComponentWithOneTip() throws Exception {
        int amount = 10;
        List<Hash> chainTransactions = makeChain(amount, Hash.NULL_HASH, 0);
        List<Hash> loneTransactions = makeStar(amount, Hash.NULL_HASH, 0);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Set<Hash>> CC = new ArrayList<>(Collections.singleton(new HashSet<>(chainTransactions)));
        loneTransactions.forEach(o -> CC.add(Collections.singleton(o)));

        Hash chainTip = chainTransactions.get(amount - 1);
        Hash tip = connectedComponentsCalculator.randomlySelectTipFromLargestConnectedComponent(CC, Collections.singleton(chainTip));
        Assert.assertEquals(chainTip, tip);
    }

    @Test
    public void selectTipFromLargestConnectedComponentWithMultipleTips() throws Exception {
        int amount = 10;
        List<Hash> chainTransactions = makeChain(amount, Hash.NULL_HASH, 0);
        List<Hash> loneTransactions = makeStar(amount, Hash.NULL_HASH, 0);

        Hash chainTip = chainTransactions.get(amount - 1);
        List<Hash> hairOnChainTransactions = makeStar(amount, chainTip, 0);
        chainTransactions.addAll(hairOnChainTransactions);

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Set<Hash>> CC = new ArrayList<>(Collections.singleton(new HashSet<>(chainTransactions)));
        loneTransactions.forEach(o -> CC.add(Collections.singleton(o)));

        Hash tip = connectedComponentsCalculator.randomlySelectTipFromLargestConnectedComponent(CC, hairOnChainTransactions);
        Assert.assertTrue(hairOnChainTransactions.contains(tip));
    }

    @Test
    public void getConnectedComponentsReturnsCorrectSetsForStar() throws Exception {
        final int amount = 10;
        Set<Hash> loneTransactions = new HashSet<Hash>(makeStar(amount, Hash.NULL_HASH, 0));

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Set<Hash>> components = connectedComponentsCalculator.getConnectedComponents(loneTransactions);

        Assert.assertEquals(amount, components.size());
        for (Set<Hash> component : components) {
            Assert.assertEquals(1, component.size());
            Assert.assertTrue(loneTransactions.contains(component.iterator().next()));
        }
    }

    @Test
    public void getConnectedComponentsReturnsCorrectSetsForStarsPlusGenesis() throws Exception {
        final int chainSize = 5;
        final int chainCount = 10;
        Set<Hash> transactions = new HashSet<>();

        transactions.add(Hash.NULL_HASH);

        for (int i = 0; i < chainCount; i++) {
            transactions.addAll(makeChain(chainSize, Hash.NULL_HASH, 0));
        }

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Set<Hash>> components = connectedComponentsCalculator.getConnectedComponents(transactions);

        Assert.assertEquals(1, components.size());
        Assert.assertEquals(chainSize * chainCount + 1, components.iterator().next().size());
    }

    @Test
    public void getConnectedComponentsReturnsCorrectSetsForManyChains() throws Exception {
        final int chainSize = 5;
        final int chainCount = 10;
        Set<Hash> transactions = new HashSet<>();

        for (int i = 0; i < chainCount; i++) {
            transactions.addAll(makeChain(chainSize, Hash.NULL_HASH, 0));
        }

        ConnectedComponentsCalculator connectedComponentsCalculator = new ConnectedComponentsCalculator(tangle, maxTransaction);
        Collection<Set<Hash>> components = connectedComponentsCalculator.getConnectedComponents(transactions);

        Assert.assertEquals(chainCount, components.size());
        for (Set<Hash> component : components) {
            Assert.assertEquals(chainSize, component.size());
        }
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
