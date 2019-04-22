package com.iota.iri.model.persistables;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.utils.Converter;

import org.junit.Test;

public class TransactionTest {
    private static final Random seed = new Random();

    @Test
    public void metadataParsedCorrectlyInHappyFlow() throws Exception {
        Transaction tx = getRandomTransaction();

        Transaction fromMetaData = new Transaction();
        byte[] bytes = tx.metadata();
        fromMetaData.readMetadata(bytes);

        assertEquals(tx.address, fromMetaData.address);
        assertEquals(tx.bundle, fromMetaData.bundle);
        assertEquals(tx.trunk, fromMetaData.trunk);
        assertEquals(tx.branch, fromMetaData.branch);
        assertEquals(tx.obsoleteTag, fromMetaData.obsoleteTag);
        assertEquals(tx.value, fromMetaData.value);
        assertEquals(tx.currentIndex, fromMetaData.currentIndex);
        assertEquals(tx.lastIndex, fromMetaData.lastIndex);
        assertEquals(tx.timestamp, fromMetaData.timestamp);

        assertEquals(tx.tag, fromMetaData.tag);
        assertEquals(tx.attachmentTimestamp, fromMetaData.attachmentTimestamp);
        assertEquals(tx.attachmentTimestampLowerBound, fromMetaData.attachmentTimestampLowerBound);
        assertEquals(tx.attachmentTimestampUpperBound, fromMetaData.attachmentTimestampUpperBound);

        assertEquals(tx.validity, fromMetaData.validity);
        assertEquals(tx.type, fromMetaData.type);
        assertEquals(tx.arrivalTime, fromMetaData.arrivalTime);
        assertEquals(tx.height, fromMetaData.height);
        assertEquals(tx.solidificationTime, fromMetaData.solidificationTime);

        assertEquals(tx.solid, fromMetaData.solid);
    }

    private Transaction getRandomTransaction() {
        Transaction transaction = new Transaction();

        byte[] trits = new byte[TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE];
        for(int i = 0; i < trits.length; i++) {
            trits[i] = (byte) (seed.nextInt(3) - 1);
        }

        transaction.bytes = Converter.allocateBytesForTrits(trits.length);
        Converter.bytes(trits, 0, transaction.bytes, 0, trits.length);
        transaction.readMetadata(transaction.bytes);
        return transaction;
    }
}