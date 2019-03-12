package com.iota.iri.service.tipselection.impl;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionTrits;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.EntryPointSelector;
import com.iota.iri.service.tipselection.StartingTipSelector;
import com.iota.iri.service.tipselection.TailFinder;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class EntryPointSelectorCumulativeWeightThresholdTest {
    private TemporaryFolder dbFolder;
    private TemporaryFolder logFolder;
    private Tangle tangle;
    private StartingTipSelector startingTipSelector;
    private TailFinder tailFinder;

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

        startingTipSelector = Mockito.mock(StartingTipSelector.class);
        tailFinder = Mockito.mock(TailFinder.class);
        // Default tailFinder mock behavior is to return the same transaction
        Mockito.when(tailFinder.findTail(Mockito.any(Hash.class)))
           .thenAnswer(tx -> Optional.of(tx.getArguments()[0]));
    }

    @Test
    public void returnsGenesisInSingleTxTangle() throws Exception {
        TransactionViewModel transaction = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(
                Hash.NULL_HASH,
                Hash.NULL_HASH),
                getRandomTransactionHash());
        transaction.store(tangle);

        final int threshold = 50;
        Mockito.when(startingTipSelector.getTip()).thenReturn(transaction.getBundleHash());
        
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, threshold,
            startingTipSelector, tailFinder);
        Hash entryPoint = entryPointSelector.getEntryPoint();

        Assert.assertEquals(Hash.NULL_HASH, entryPoint);
    }

    @Test
    public void returnsCorrectTxInChain() throws Exception {
        final int threshold = 100;
        List<Hash> chain = makeChain(threshold * 5);

        // getTip returns genesis
        Mockito.when(startingTipSelector.getTip()).thenReturn(chain.get(chain.size() - 1));

        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, threshold, startingTipSelector, tailFinder);

        Hash entryPoint = entryPointSelector.getEntryPoint(); 

        Assert.assertEquals(chain.get(chain.size() - threshold), entryPoint);
    }

    @Test
    public void returnsCorrectTxInWheatStockShape() throws Exception {
        final int threshold = 15;
        final int stalkLevels = 15;
        final int txPerLevel = 5;
        final int expectedStalkLevel = stalkLevels - 3;
        
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

        Mockito.when(startingTipSelector.getTip()).thenReturn(mainStalk.get(mainStalk.size() - 1).getHash());
        
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, threshold,
            startingTipSelector, tailFinder);
        Hash entryPoint = entryPointSelector.getEntryPoint();

        Assert.assertNotEquals(Hash.NULL_HASH, entryPoint);
        Assert.assertEquals(mainStalk.get(expectedStalkLevel).getHash(), entryPoint);
    }

    @Test
    public void failWhenEntryPointSizeIsTooBig() throws Exception {
        // The scenario is two chains attached to the genesis: very long and very short.
        // The random tip function returns the short chain's tip.
        final int threshold = 10;
        final int longChainLength = EntryPointSelectorCumulativeWeightThreshold.MAX_SUBTANGLE_SIZE + 50;
        final int shortChainLength = 2;
        Hash longChainTip = Hash.NULL_HASH;
        Hash shortChainTip= Hash.NULL_HASH;

        for (int i = 0; i < longChainLength; i++) {
            TransactionViewModel newTip = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(longChainTip, longChainTip), getRandomTransactionHash());
            newTip.store(tangle);
            longChainTip = newTip.getHash();
        }

        for (int i = 0; i < shortChainLength; i++) {
            TransactionViewModel newTip = new TransactionViewModel(getRandomTransactionWithTrunkAndBranch(shortChainTip, shortChainTip), getRandomTransactionHash());
            newTip.store(tangle);
            shortChainTip = newTip.getHash();
        }

        Mockito.when(startingTipSelector.getTip()).thenReturn(shortChainTip);
        
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, threshold,
            startingTipSelector, tailFinder);

        exception.expect(IllegalStateException.class);
        entryPointSelector.getEntryPoint();
    }

    @Test
    public void succeedWhenEntryPointSizeIsJustRight() throws Exception {
        // Two chains
        final int threshold = 10;
        final int longChainLength = (int)(EntryPointSelectorCumulativeWeightThreshold.MAX_SUBTANGLE_SIZE / 2.5);
        final int shortChainLength = 2;
        
        makeChain(longChainLength);
        List<Hash> shortChain = makeChain(shortChainLength);

        // Start from the short chain, so we start the BFS from the genesis
        Mockito.when(startingTipSelector.getTip()).thenReturn(shortChain.get(shortChain.size() - 1));
        
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, threshold,
            startingTipSelector, tailFinder);

        Hash entryPoint = entryPointSelector.getEntryPoint();

        Assert.assertEquals(Hash.NULL_HASH, entryPoint);
    }

    @Test
    public void returnsTailRatherThanEndOfBacktrack() throws Exception {
        final int threshold = 10;
        final Hash tail = getRandomTransactionHash();

        // getTip returns genesis
        Mockito.when(startingTipSelector.getTip()).thenReturn(Hash.NULL_HASH);

        // findTail returns the "tail"
        Mockito.reset(tailFinder);
        Mockito.when(tailFinder.findTail(Mockito.any(Hash.class)))
            .thenReturn(Optional.of(tail));

        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, threshold, startingTipSelector, tailFinder);
        Hash entryPoint = entryPointSelector.getEntryPoint(); 

        Assert.assertEquals(tail, entryPoint);
    }

    @Test
    public void throwsExceptionIfTailFinderFails() throws Exception {
        final int threshold = 10;

        // getTip returns genesis
        Mockito.when(startingTipSelector.getTip()).thenReturn(Hash.NULL_HASH);

        // findTail returns Optional.empty()
        Mockito.reset(tailFinder);
        Mockito.when(tailFinder.findTail(Mockito.any(Hash.class)))
            .thenReturn(Optional.empty());

        exception.expect(NoSuchElementException.class);
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(tangle, threshold, startingTipSelector, tailFinder);
        entryPointSelector.getEntryPoint();
    }

    private List<Hash> makeChain(int length) throws Exception {
        List<Hash> chain = new ArrayList<>();

        // start from genesis
        Hash tip = Hash.NULL_HASH;

        for (int i = 0; i < length; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(tip, tip), getRandomTransactionHash());
            newTip.store(tangle);
            tip = newTip.getHash();
            chain.add(tip);
        }

        return chain;
    }
}
