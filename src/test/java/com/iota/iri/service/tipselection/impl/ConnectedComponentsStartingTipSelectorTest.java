package com.iota.iri.service.tipselection.impl;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class ConnectedComponentsStartingTipSelectorTest {

    private TemporaryFolder dbFolder;
    private TemporaryFolder logFolder;
    private Tangle tangle;
    private TipsViewModel tipsViewModel;

    private int maxTransactions = 50;

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

    @Test
    public void oldTipIgnoredInTwoComponentStructure() throws Exception {
        List<Hash> oldTransactions = makeChain(maxTransactions, Hash.NULL_HASH, 1000);
        List<Hash> newTransactions = makeChain(maxTransactions, Hash.NULL_HASH, 2000 + maxTransactions);
        Hash oldTip = oldTransactions.get(oldTransactions.size() - 1);
        Hash newTip = newTransactions.get(newTransactions.size() - 1);
        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt())).thenReturn(Arrays.asList(oldTip, newTip));
        ConnectedComponentsStartingTipSelector connectedComponentsCalculator = new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, tipsViewModel);

        Hash selectedTip = connectedComponentsCalculator.getTip();

        Assert.assertEquals(newTip, selectedTip);
    }

    @Test
    public void selectTipFromBiggerComponentInTwoComponentStructure() throws Exception {
        int chainLength = 20;
        int max = chainLength * 10;

        List<Hash> bigChainTransactions = makeChain(chainLength, getRandomTransactionHash(), 1000);
        List<Hash> bigChainTips = new ArrayList<>(Arrays.asList(bigChainTransactions.get(bigChainTransactions.size() - 1)));
        bigChainTransactions.addAll(makeChain(chainLength, bigChainTransactions.get(0), 1000));
        bigChainTips.add(bigChainTransactions.get(bigChainTransactions.size() - 1));

        List<Hash> smallChainTransactions = makeChain(chainLength, getRandomTransactionHash(), 1000);
        Hash smallChainTip = smallChainTransactions.get(smallChainTransactions.size() - 1);

        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt()))
            .thenReturn(Arrays.asList(bigChainTips.get(0), bigChainTips.get(1), smallChainTip));

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator = 
            new ConnectedComponentsStartingTipSelector(tangle, max, tipsViewModel);

        Hash selectedTip = connectedComponentsCalculator.getTip();

        Assert.assertTrue(bigChainTips.contains(selectedTip));
        Assert.assertNotEquals(smallChainTip, selectedTip);
    }

    @Test
    public void oldTipsDroppedWhenStarAroundGenesisIsTooBig() throws Exception {
        List<Hash> transactions = makeStar(maxTransactions * 100, Hash.NULL_HASH, 1000);
        List<Hash> newestTransactions = transactions.subList(transactions.size() - maxTransactions, transactions.size());

        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt())).thenReturn(transactions);

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator = 
            new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, tipsViewModel);

        Hash selectedTip = connectedComponentsCalculator.getTip();

        Assert.assertTrue(newestTransactions.contains(selectedTip));
    }

    @Test
    public void selectTipOneConnectedComponentWithOneTip() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt())).thenReturn(Arrays.asList(transaction.getHash()));

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator = 
            new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, tipsViewModel);

        Hash tip = connectedComponentsCalculator.getTip();

        Assert.assertEquals(transaction.getHash(), tip);
    }

    @Test(expected = IllegalStateException.class)
    public void failOnEmptyConnectedComponentIntersection() throws Exception {
        ConnectedComponentsStartingTipSelector connectedComponentsCalculator =
            new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, tipsViewModel);

        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt())).thenReturn(Arrays.asList());

        //should throw IllegalStateException
        connectedComponentsCalculator.getTip();
    }

    @Test
    public void selectTipFromLargestConnectedComponentWithOneTip() throws Exception {
        int amount = 10;
        List<Hash> chainTransactions = makeChain(amount, getRandomTransactionHash(), 0);
        Hash chainTip = chainTransactions.get(amount - 1);

        // Create singleton detached transactions
        List<Hash> loneTransactions = new ArrayList<>();
        for (int i=0; i < amount; i++) {
            loneTransactions = makeStar(1, getRandomTransactionHash(), 0);
        }

        List<Hash> allTips = new ArrayList<>(loneTransactions);
        allTips.add(chainTip);

        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt())).thenReturn(allTips);

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator =
            new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, tipsViewModel);

        Hash tip = connectedComponentsCalculator.getTip();

        Assert.assertEquals(chainTip, tip);
    }

    @Test
    public void selectTipFromLargestConnectedComponentWithMultipleTips() throws Exception {
        int amount = 10;
        List<Hash> chainTransactions = makeChain(amount, getRandomTransactionHash(), 0);

        // Create singleton detached transactions
        List<Hash> loneTransactions = new ArrayList<>();
        for (int i=0; i < amount; i++) {
            loneTransactions = makeStar(1, getRandomTransactionHash(), 0);
        }

        Hash chainTip = chainTransactions.get(amount - 1);
        List<Hash> hairOnChainTransactions = makeStar(amount, chainTip, 0);
        chainTransactions.addAll(hairOnChainTransactions);

        List<Hash> allTips = new ArrayList<>(loneTransactions);
        allTips.addAll(hairOnChainTransactions);

        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt())).thenReturn(allTips);

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator =
            new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, tipsViewModel);

        Hash selectedTip = connectedComponentsCalculator.getTip();

        Assert.assertTrue(hairOnChainTransactions.contains(selectedTip));
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
