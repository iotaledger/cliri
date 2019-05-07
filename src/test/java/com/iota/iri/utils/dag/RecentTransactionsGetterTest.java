package com.iota.iri.utils.dag;

import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionHash;
import static com.iota.iri.controllers.TransactionViewModelTest.getRandomTransactionWithTrunkAndBranch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import com.iota.iri.utils.dag.impl.RecentTransactionsGetterImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class RecentTransactionsGetterTest {
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
        tangle.addPersistenceProvider(new RocksDBPersistenceProvider(
                dbFolder.getRoot().getAbsolutePath(), logFolder.getRoot().getAbsolutePath(),1000,
                Tangle.COLUMN_FAMILIES, Tangle.METADATA_COLUMN_FAMILY));
        tangle.init();

        tipsViewModel = Mockito.mock(TipsViewModel.class);
    }

    @Test
    public void correctNewestSetForStarShapeTangle() throws Exception {
        List<Hash> transactions = makeStar(maxTransactions * 100, Hash.NULL_HASH, 1000);
        List<Hash> newestTransactions = transactions.subList(transactions.size() - maxTransactions, transactions.size());

        RecentTransactionsGetter recentTransactionsGetter = new RecentTransactionsGetterImpl(tipsViewModel, tangle);
        Mockito.when(tipsViewModel.getLatestSolidTips(Mockito.anyInt())).thenReturn(transactions);

        // Hash selectedTip = connectedComponentsCalculator.getTip();
        Collection<Hash> recentTxs = recentTransactionsGetter.getRecentTransactions(maxTransactions);

        Assert.assertEquals(maxTransactions, recentTxs.size());
        for (Hash tx : recentTxs) {
            Assert.assertTrue(newestTransactions.contains(tx));
        }
        for (Hash tx : newestTransactions) {
            Assert.assertTrue(recentTxs.contains(tx));
        }
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
