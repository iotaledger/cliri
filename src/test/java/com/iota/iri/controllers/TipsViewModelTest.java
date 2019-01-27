package com.iota.iri.controllers;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Created by paul on 5/2/17.
 */
public class TipsViewModelTest {
    private static final TemporaryFolder dbFolder = new TemporaryFolder();
    private static final TemporaryFolder logFolder = new TemporaryFolder();
    private static Tangle tangle;

    @Before
    public void setUp() throws Exception {
        tangle = new Tangle();
        dbFolder.create();
        logFolder.create();
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(dbFolder.getRoot().getAbsolutePath(), logFolder
                .getRoot().getAbsolutePath(), 1000));
        tangle.init();
    }

    @After
    public void tearDown() throws Exception {
        tangle.shutdown();
        dbFolder.delete();
        logFolder.delete();
    }

    @Test
    public void addTipHash() throws Exception {

    }

    @Test
    public void removeTipHash() throws Exception {

    }

    @Test
    public void setSolid() throws Exception {

    }

    @Test
    public void getTips() throws Exception {

    }

    @Test
    public void getRandomSolidTipHash() throws Exception {

    }

    @Test
    public void getRandomNonSolidTipHash() throws Exception {

    }

    @Test
    public void getRandomTipHash() throws Exception {

    }

    @Test
    public void nonSolidSize() throws Exception {

    }

    @Test
    public void size() throws Exception {

    }

    @Test
    public void loadTipHashes() throws Exception {

    }

    @Test
    public void nonsolidCapacityLimited() throws ExecutionException, InterruptedException {
        TipsViewModel tipsVM = new TipsViewModel(tangle);
        int capacity = TipsViewModel.MAX_TIPS;
        //fill tips list
        for (int i = 0; i < capacity * 2 ; i++) {
            Hash hash = TransactionViewModelTest.getRandomTransactionHash();
            tipsVM.addTipHash(hash);
        }
        //check that limit wasn't breached
        assertEquals(capacity, tipsVM.nonSolidSize());
    }

    @Test
    public void solidCapacityLimited() throws ExecutionException, InterruptedException {
        TipsViewModel tipsVM = new TipsViewModel(tangle);
        int capacity = TipsViewModel.MAX_TIPS;
        //fill tips list
        for (int i = 0; i < capacity * 2 ; i++) {
            Hash hash = TransactionViewModelTest.getRandomTransactionHash();
            tipsVM.addTipHash(hash);
            tipsVM.setSolid(hash);
        }
        //check that limit wasn't breached
        assertEquals(capacity, tipsVM.size());
    }

    @Test
    public void totalCapacityLimited() throws ExecutionException, InterruptedException {
        TipsViewModel tipsVM = new TipsViewModel(tangle);
        int capacity = TipsViewModel.MAX_TIPS;
        //fill tips list
        for (int i = 0; i <= capacity * 4; i++) {
            Hash hash = TransactionViewModelTest.getRandomTransactionHash();
            tipsVM.addTipHash(hash);
            if (i % 2 == 1) {
                tipsVM.setSolid(hash);
            }
        }
        //check that limit wasn't breached
        assertEquals(capacity * 2, tipsVM.size());
    }

    @Test
    public void tipsEmtpyAfterClear() throws Exception {
        TipsViewModel tipsVM = new TipsViewModel(tangle);

        int size = 60;

        //fill tips list
        for (int i = 0; i < size; i++) {
            Hash hash = TransactionViewModelTest.getRandomTransactionHash();
            tipsVM.addTipHash(hash);
        }

        assertEquals(size, tipsVM.size());

        tipsVM.clear();

        assertEquals(0, tipsVM.size());
    } 

    @Test
    public void solidTipsPopulatedWhenEmpty() throws Exception {
        final int tipCount = 10;
        Set<Hash> tips = new HashSet<>();

        for (int i = 0; i < tipCount; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());
            newTip.updateSolid(true);

            newTip.store(tangle);
            tips.add(newTip.getHash());
        }

        TipsViewModel tipsVM = new TipsViewModel(tangle);

        Hash tip = tipsVM.getRandomSolidTipHash();

        assertNotNull(tip);
        assertTrue(tips.contains(tip));
    }

    @Test
    public void nonSolidTipsAreIgnoredWhenPopulatingSolidTips() throws Exception {
        TransactionViewModel solidTip = new TransactionViewModel(
            getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());
        TransactionViewModel nonSolidTip = new TransactionViewModel(
            getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());

        solidTip.updateSolid(true);

        solidTip.store(tangle);
        nonSolidTip.store(tangle);

        TipsViewModel tipsVM = new TipsViewModel(tangle);

        Hash tip = tipsVM.getRandomSolidTipHash();

        assertEquals(solidTip.getHash(), tip);
    }

    @Test
    public void getLatestSolidTipsHappyFlow() throws Exception {
        final int total = 50;
        final int count = 10;

        TipsViewModel tipsVM = new TipsViewModel(tangle);

        for (int i = 0; i < total; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());
            
            tipsVM.addTipHash(newTip.getHash());
            tipsVM.setSolid(newTip.getHash());
        }


        List<Hash> tips = tipsVM.getLatestSolidTips(count);

        assertNotNull(tips);
        assertEquals(count, tips.size());
    }

}
