package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.EntryPointSelector;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static com.iota.iri.controllers.TransactionViewModelTest.*;

public class EntryPointSelectorCumulativeWeightThresholdTest {
    private static final TemporaryFolder dbFolder = new TemporaryFolder();
    private static final TemporaryFolder logFolder = new TemporaryFolder();
    private static Tangle tangle;
    private static TipsViewModel tipsViewModel;

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
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(dbFolder.getRoot().getAbsolutePath(),
                logFolder.getRoot().getAbsolutePath(), 1000));
        tangle.init();

        tipsViewModel = Mockito.mock(TipsViewModel.class);
    }

    @Test
    public void returnsGenesisInSingleTxTangle() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        final int threshold = 50;
        Mockito.when(tipsViewModel.getRandomSolidTipHash()).thenReturn(transaction.getBundleHash());
        
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, tipsViewModel, threshold);
        Hash entryPoint = entryPointSelector.getEntryPoint();

        Assert.assertEquals(Hash.NULL_HASH, entryPoint);
    }

    @Test
    public void returnsCorrectTxInChain() throws Exception {
        final int threshold = 5;
        final int chainLength = 30;
        final int expectedEntrypoint = chainLength - 7;
        
        List<TransactionViewModel> transactions = new ArrayList<TransactionViewModel>();

        transactions.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));

        for (int i = 0; i < chainLength; i++) {
            Hash prevTxHash = transactions.get(transactions.size() - 1).getHash();
            transactions.add(new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(prevTxHash, prevTxHash), getRandomTransactionHash()));
        }

        for (TransactionViewModel transaction : transactions) {
            transaction.store(tangle);
        }

        Mockito.when(tipsViewModel.getRandomSolidTipHash()).thenReturn(transactions.get(transactions.size() - 1).getHash());
        
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, tipsViewModel, threshold);
        Hash entryPoint = entryPointSelector.getEntryPoint();

        Assert.assertNotEquals(Hash.NULL_HASH, entryPoint);
        Assert.assertEquals(transactions.get(expectedEntrypoint).getHash(), entryPoint);
    }

    @Test
    public void returnsCorrectTxInWheatStockShape() throws Exception {
        final int threshold = 15;
        final int stalkLevels = 15;
        final int txPerLevel = 5;
        final int expectedStalkLevel = stalkLevels - 4;
        
        List<TransactionViewModel> mainStalk = new ArrayList<TransactionViewModel>();

        mainStalk.add(new TransactionViewModel(getRandomTransactionTrits(), getRandomTransactionHash()));
        mainStalk.get(0).store(tangle);

        for (int i = 0; i < stalkLevels - 1; i++) {
            Hash prevTxHash = mainStalk.get(mainStalk.size() - 1).getHash();
            TransactionViewModel mainStalkTx = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(prevTxHash, prevTxHash), getRandomTransactionHash());

            mainStalk.add(mainStalkTx);
            mainStalkTx.store(tangle);
            
            for (int j = 0; j < txPerLevel; j++) {
                new TransactionViewModel(
                    getRandomTransactionWithTrunkAndBranch(mainStalkTx.getHash(), mainStalkTx.getHash()), getRandomTransactionHash())
                    .store(tangle);
            }
        }

        Mockito.when(tipsViewModel.getRandomSolidTipHash()).thenReturn(mainStalk.get(mainStalk.size() - 1).getHash());
        
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, tipsViewModel, threshold);
        Hash entryPoint = entryPointSelector.getEntryPoint();

        Assert.assertNotEquals(Hash.NULL_HASH, entryPoint);
        Assert.assertEquals(mainStalk.get(expectedStalkLevel).getHash(), entryPoint);
    }
}
