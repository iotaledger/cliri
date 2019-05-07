package com.iota.iri.service.tipselection.impl;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import com.iota.iri.utils.dag.RecentTransactionsGetter;

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
    private RecentTransactionsGetter recentTransactionsGetter;

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
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(
                dbFolder.getRoot().getAbsolutePath(), logFolder.getRoot().getAbsolutePath(),1000,
                Tangle.COLUMN_FAMILIES, Tangle.METADATA_COLUMN_FAMILY));
        tangle.init();

        recentTransactionsGetter = Mockito.mock(RecentTransactionsGetter.class);
    }

    @Test
    public void selectTransactionFromBiggerComponentInTwoComponentStructure() throws Exception {
        int chainLength = 20;
        int max = chainLength * 10;

        List<Hash> bigChainTransactions = makeChain(chainLength, getRandomTransactionHash());
        List<Hash> bigChainTips = new ArrayList<>(Arrays.asList(bigChainTransactions.get(bigChainTransactions.size() - 1)));
        bigChainTransactions.addAll(makeChain(chainLength, bigChainTransactions.get(0)));
        bigChainTips.add(bigChainTransactions.get(bigChainTransactions.size() - 1));

        List<Hash> smallChainTransactions = makeChain(chainLength, getRandomTransactionHash());

        List<Hash> allTx = new ArrayList<>();
        allTx.addAll(bigChainTransactions);
        allTx.addAll(smallChainTransactions);

        Mockito.when(recentTransactionsGetter.getRecentTransactions(Mockito.anyInt())).thenReturn(allTx);

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator = 
            new ConnectedComponentsStartingTipSelector(tangle, max, recentTransactionsGetter);

        Hash selectedTx = connectedComponentsCalculator.getTip();

        Assert.assertTrue(bigChainTransactions.contains(selectedTx));
    }

    @Test
    public void selectTipOneConnectedComponentWithOneTip() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        Mockito.when(recentTransactionsGetter.getRecentTransactions(Mockito.anyInt())).thenReturn(Arrays.asList(transaction.getHash()));

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator = 
            new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, recentTransactionsGetter);

        Hash selectedTx = connectedComponentsCalculator.getTip();

        Assert.assertEquals(transaction.getHash(), selectedTx);
    }

    @Test
    public void selectTipFromLargestConnectedComponentWithOneTip() throws Exception {
        int amount = 10;
        List<Hash> chainTransactions = makeChain(amount, getRandomTransactionHash());

        // Create singleton detached transactions
        List<Hash> loneTransactions = new ArrayList<>();
        for (int i=0; i < amount; i++) {
            loneTransactions.addAll(makeStar(1, getRandomTransactionHash()));
        }

        List<Hash> allTxs = new ArrayList<>(loneTransactions);
        allTxs.addAll(chainTransactions);

        Mockito.when(recentTransactionsGetter.getRecentTransactions(Mockito.anyInt())).thenReturn(allTxs);

        ConnectedComponentsStartingTipSelector connectedComponentsCalculator =
            new ConnectedComponentsStartingTipSelector(tangle, maxTransactions, recentTransactionsGetter);

        Hash tx = connectedComponentsCalculator.getTip();

        Assert.assertTrue(chainTransactions.contains(tx));
    }

    private List<Hash> makeChain(int length, Hash tip) throws Exception {
        List<Hash> chain = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(tip, tip), getRandomTransactionHash());
            newTip.store(tangle);
            tip = newTip.getHash();
            chain.add(tip);
        }

        return chain;
    }

    private List<Hash> makeStar(int length, Hash tip) throws Exception {
        List<Hash> star = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(tip, tip), getRandomTransactionHash());
            newTip.store(tangle);
            star.add(newTip.getHash());
        }

        return star;
    }
}
