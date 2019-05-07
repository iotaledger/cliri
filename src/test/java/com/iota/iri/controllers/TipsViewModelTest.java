package com.iota.iri.controllers;

import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;
import static org.junit.Assert.*;

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
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(
                dbFolder.getRoot().getAbsolutePath(), logFolder.getRoot().getAbsolutePath(),1000,
                Tangle.COLUMN_FAMILIES, Tangle.METADATA_COLUMN_FAMILY));
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
    public void solidTipsPopulatedWithGenesisForEmptyTangle() throws Exception {
        TipsViewModel tipsVM = new TipsViewModel(tangle);

        List<Hash> solidTips = tipsVM.getLatestSolidTips(1);
        TransactionViewModel genesis = new TransactionViewModel(
            getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), Hash.NULL_HASH);

        genesis.updateSolid(true);
        genesis.store(tangle);

        assertNotNull(solidTips);
        assertEquals(Hash.NULL_HASH, solidTips.get(0));
    }

    @Test
    public void solidTipsPopulatedWhenSetIsEmpty() throws Exception {
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

        List<Hash> solidTips = tipsVM.getLatestSolidTips(1);

        assertNotNull(solidTips);
        assertTrue(tips.contains(solidTips.get(0)));
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

        List<Hash> solidTips = tipsVM.getLatestSolidTips(1);

        assertEquals(solidTip.getHash(), solidTips.get(0));
    }

    @Test
    public void getRandomSolidTipReturnsGenesisWhenThereAreNoSolidTips() throws Exception {
        final int totalUnsolid = 50;

        TipsViewModel tipsVM = new TipsViewModel(tangle);

        for (int i = 0; i < totalUnsolid; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());
            
            // Adding non-solid tip
            newTip.updateSolid(false);
            newTip.store(tangle);
            tipsVM.addTipHash(newTip.getHash());
        }

        Hash tip = tipsVM.getRandomSolidTipHash();

        assertNotNull(tip);
        assertEquals(Hash.NULL_HASH, tip);
    }

    @Test
    public void getLatestSolidTipsHappyFlow() throws Exception {
        final int total = 50;
        final int count = 10;
        List<Hash> originalTips = new ArrayList<>();

        TipsViewModel tipsVM = new TipsViewModel(tangle);

        for (int i = 0; i < total; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());
            
            originalTips.add(newTip.getHash());

            newTip.store(tangle);
            tipsVM.addTipHash(newTip.getHash());
            tipsVM.setSolid(newTip.getHash());
        }

        List<Hash> expectedTips = originalTips.subList(originalTips.size() - count, originalTips.size());

        List<Hash> tips = tipsVM.getLatestSolidTips(count);

        assertNotNull(tips);
        assertEquals(count, tips.size());

        for (Hash tip : tips) {
            assertTrue(expectedTips.contains(tip));
        }
    }

    @Test
    public void getLatestSolidTipsIgnoresNonSolidTips() throws Exception {
        final int total = 50;
        final int count = 10;
        List<Hash> solidTips = new ArrayList<>();
        List<Hash> nonSolidTips = new ArrayList<>();

        TipsViewModel tipsVM = new TipsViewModel(tangle);

        for (int i = 0; i < total; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());
            
            solidTips.add(newTip.getHash());

            newTip.store(tangle);
            tipsVM.addTipHash(newTip.getHash());
            tipsVM.setSolid(newTip.getHash());
        }

        for (int i = 0; i < total; i++) {
            TransactionViewModel newTip = new TransactionViewModel(
                getRandomTransactionWithTrunkAndBranch(Hash.NULL_HASH, Hash.NULL_HASH), getRandomTransactionHash());
            
            nonSolidTips.add(newTip.getHash());

            newTip.store(tangle);
            tipsVM.addTipHash(newTip.getHash());
        }

        List<Hash> tips = tipsVM.getLatestSolidTips(count);

        assertNotNull(tips);
        assertEquals(count, tips.size());

        for (Hash tip : tips) {
            assertTrue(solidTips.contains(tip));
        }
    }

}
