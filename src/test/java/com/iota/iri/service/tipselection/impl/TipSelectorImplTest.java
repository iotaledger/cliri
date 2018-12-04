package com.iota.iri.service.tipselection.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iota.iri.LedgerValidator;
import com.iota.iri.model.Hash;
import com.iota.iri.model.HashId;
import com.iota.iri.service.tipselection.EntryPointSelector;
import com.iota.iri.service.tipselection.RatingCalculator;
import com.iota.iri.service.tipselection.ReferenceChecker;
import com.iota.iri.service.tipselection.Walker;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import com.iota.iri.utils.collections.interfaces.UnIterableMap;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class TipSelectorImplTest {
    private static final TemporaryFolder dbFolder = new TemporaryFolder();
    private static final TemporaryFolder logFolder = new TemporaryFolder();
    private static Tangle tangle;
    private static Walker walker;
    private static LedgerValidator ledgerValidator;
    private static EntryPointSelector entryPointSelector;
    private static RatingCalculator ratingCalculator;
    private static ReferenceChecker referenceChecker;

    @AfterClass
    public static void tearDown() throws Exception {
        tangle.shutdown();
        dbFolder.delete();
    }

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void setUp() throws Exception {
        tangle = new Tangle();
        dbFolder.create();
        logFolder.create();
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(dbFolder.getRoot().getAbsolutePath(), logFolder
                .getRoot().getAbsolutePath(), 1000));
        tangle.init();

        walker = Mockito.mock(Walker.class);

        ledgerValidator = Mockito.mock(LedgerValidator.class);
        Mockito.when(ledgerValidator.checkConsistency(Mockito.anyListOf(Hash.class))).thenReturn(true);

        entryPointSelector = Mockito.mock(EntryPointSelector.class);
        Mockito.when(entryPointSelector.getEntryPoint()).thenReturn(Hash.NULL_HASH);

        ratingCalculator = Mockito.mock(RatingCalculator.class);
        Mockito.when(ratingCalculator.calculate(Mockito.any(Hash.class))).thenReturn((UnIterableMap<HashId, Integer>) Mockito.mock(UnIterableMap.class));

        referenceChecker = Mockito.mock(ReferenceChecker.class);
        Mockito.when(referenceChecker.doesReference(Mockito.any(Hash.class), Mockito.any(Hash.class))).thenReturn(true);
    }

    @Test
    public void testGetConfidencesReturnsEmptyListWhenGettingEmptyList() throws Exception {
        TipSelectorImpl tipSelector = new TipSelectorImpl(tangle, ledgerValidator, entryPointSelector, ratingCalculator, walker, referenceChecker);

        List<Hash> inputs = new ArrayList<>();

        List<Double> result = tipSelector.getConfidences(inputs);

        Assert.assertEquals(0, result.size());
    }
    
    @Test
    public void testGetConfidencesAllOneWhenReferenceAlwaysTrue() throws Exception {
        TipSelectorImpl tipSelector = new TipSelectorImpl(tangle, ledgerValidator, entryPointSelector, ratingCalculator, walker, referenceChecker);

        List<Hash> inputs = Collections.nCopies(3, Hash.NULL_HASH);

        List<Double> result = tipSelector.getConfidences(inputs);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(1d, result.get(0).doubleValue(), 0.0001d);
        Assert.assertEquals(1d, result.get(1).doubleValue(), 0.0001d);
        Assert.assertEquals(1d, result.get(2).doubleValue(), 0.0001d);
    }

    @Test
    public void testGetConfidencesOverHalfWhenOnlyThreeTipsDontReference() throws Exception {
        TipSelectorImpl tipSelector = new TipSelectorImpl(tangle, ledgerValidator, entryPointSelector, ratingCalculator, walker, referenceChecker);

        List<Hash> inputs = Collections.nCopies(1, Hash.NULL_HASH);
        // Returns false the first three times and then true for all consecutive calls
        Mockito.when(referenceChecker.doesReference(Mockito.any(Hash.class), Mockito.any(Hash.class)))
                .thenReturn(false, false, false, true);

        List<Double> result = tipSelector.getConfidences(inputs);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0) > 0.5d);
    }
}
