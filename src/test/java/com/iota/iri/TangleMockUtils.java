package com.iota.iri;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.model.StateDiff;
import com.iota.iri.model.persistables.Transaction;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.Converter;
import com.iota.iri.utils.Pair;

import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Contains utilities that help to mock the retrieval of database entries from the tangle.<br />
 * <br />
 * Mocking the tangle allows us to write unit tests that perform much faster than spinning up a new database for every
 * test.<br />
 */
public class TangleMockUtils {
    public static List<TransactionViewModel> mockValidBundle(Tangle tangle,
                                                             BundleValidator bundleValidator,
                                                             int numOfNonValueTxs,
                                                             String... spendsInTrytes) throws Exception {
        List<TransactionViewModel> bundle = new ArrayList<>();
        String address = "ADDRESS99";
        TransactionViewModel tx = null;
        int lastIndex = spendsInTrytes.length + numOfNonValueTxs - 1;
        int currentIndex = lastIndex;
        for (int i = 0; i < spendsInTrytes.length; i++) {
            byte[] trits = new byte[TransactionViewModel.TRINARY_SIZE];
            Converter.trits(spendsInTrytes[i], trits, TransactionViewModel.VALUE_TRINARY_OFFSET);
            address = TransactionTestUtils.nextWord(address);
            Converter.trits(address, trits, TransactionViewModel.ADDRESS_TRINARY_OFFSET);
            if (tx != null) {
                TransactionTestUtils.getTransactionTritsWithTrunkAndBranch(trits, tx.getHash(), Hash.NULL_HASH);
            }
            TransactionTestUtils.setLastIndex(trits, lastIndex);
            TransactionTestUtils.setCurrentIndex(trits, currentIndex--);
            tx = TransactionTestUtils.createTransactionFromTrits(trits);
            tx.setMetadata();
            mockTransaction(tangle, tx);
            bundle.add(tx);
        }

        for (int i = 0; i < numOfNonValueTxs; i++) {
            if (tx == null) {
                tx = TransactionTestUtils.createBundleHead(currentIndex--);
            }
            else {
                tx = TransactionTestUtils.createTransactionWithTrunkBundleHash(tx, Hash.NULL_HASH);
            }
            tx.setMetadata();
            mockTransaction(tangle, tx);
            bundle.add(tx);
        }

        Collections.reverse(bundle);
        Mockito.when(bundleValidator.validate(Mockito.eq(tangle),
                Mockito.eq(bundle.iterator().next().getHash())))
                .thenReturn(Arrays.asList(bundle));

        return bundle;
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region [mockTransaction] /////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates an empty transaction, which is marked filled and parsed.
     * This transaction is returned when the hash is asked to load in the tangle object
     * 
     * @param tangle mocked tangle object
     * @param hash
     * @return The newly created (empty) transaction
     */
    public static Transaction mockTransaction(Tangle tangle, Hash hash) {
        Transaction transaction = new Transaction();
        transaction.bytes = new byte[0];
        transaction.type = TransactionViewModel.FILLED_SLOT;
        transaction.parsed = true;

        return mockTransaction(tangle, hash, transaction);
    }
    
    /**
     * Mocks the tangle object by checking for the hash and returning the transaction.
     * 
     * @param tangle mocked tangle object
     * @param hash transaction hash
     * @param transaction the transaction we send back
     * @return The transaction
     */
    public static Transaction mockTransaction(Tangle tangle, Hash hash, Transaction transaction) {
        try {
            Mockito.when(tangle.load(Transaction.class, hash)).thenReturn(transaction);
            Mockito.when(tangle.getLatest(Transaction.class, Hash.class)).thenReturn(new Pair<>(hash, transaction));
        } catch (Exception e) {
            // the exception can not be raised since we mock
        }

        return transaction;
    }

    /**
     * Mocks the tangle object by checking for the hash and returning the transaction.
     *
     * @param tangle mocked tangle object
     * @param tvm the transaction we mock in the tangle
     * @return The transaction
     */
    public static Transaction mockTransaction(Tangle tangle, TransactionViewModel tvm) throws NoSuchFieldException, IllegalAccessException {
        Transaction transaction;
        Field transactionField = tvm.getClass().getDeclaredField("transaction");
        transactionField.setAccessible(true);
        transaction = (Transaction) transactionField.get(tvm);
        return mockTransaction(tangle, tvm.getHash(), transaction);
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region [mockStateDiff] ///////////////////////////////////////////////////////////////////////////////////////////

    public static StateDiff mockStateDiff(Tangle tangle, Hash hash, Map<Hash, Long> balanceDiff) {
        StateDiff stateDiff = new StateDiff();
        stateDiff.state = balanceDiff;

        try {
            Mockito.when(tangle.load(StateDiff.class, hash)).thenReturn(stateDiff);
            Mockito.when(tangle.getLatest(StateDiff.class, Hash.class)).thenReturn(new Pair<>(hash, stateDiff));
        } catch (Exception e) {
            // the exception can not be raised since we mock
        }

        return stateDiff;
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////
}
