package com.iota.iri.service.snapshot.impl;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.snapshot.Snapshot;
import com.iota.iri.service.snapshot.SnapshotProvider;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class SnapshotMockUtils {
    private static final Hash DEFAULT_GENESIS_HASH = Hash.NULL_HASH;

    private static final Hash DEFAULT_GENESIS_ADDRESS = Hash.NULL_HASH;

    private static final long DEFAULT_GENESIS_TIMESTAMP = 1522146728;

    //region [mockSnapshotProvider] ////////////////////////////////////////////////////////////////////////////////////

    public static SnapshotProvider mockSnapshotProvider(SnapshotProvider snapshotProvider) {
        return mockSnapshotProvider(snapshotProvider, DEFAULT_GENESIS_HASH);
    }

    public static SnapshotProvider mockSnapshotProvider(SnapshotProvider snapshotProvider,
            Hash genesisHash) {

        return mockSnapshotProvider(snapshotProvider, genesisHash, DEFAULT_GENESIS_TIMESTAMP);
    }

    public static SnapshotProvider mockSnapshotProvider(SnapshotProvider snapshotProvider,
            Hash genesisHash, long genesisTimestamp) {

        Map<Hash, Long> balances = new HashMap<>();
        balances.put(DEFAULT_GENESIS_ADDRESS, TransactionViewModel.SUPPLY);

        return mockSnapshotProvider(snapshotProvider, genesisHash, genesisTimestamp, balances);
    }

    public static SnapshotProvider mockSnapshotProvider(SnapshotProvider snapshotProvider,
            Hash genesisHash, long genesisTimestamp, Map<Hash, Long> balances) {

        Snapshot initialSnapshot = new SnapshotImpl(
                new SnapshotStateImpl(balances),
                new SnapshotMetaDataImpl(genesisHash, genesisTimestamp)
        );
        Snapshot latestSnapshot = initialSnapshot.clone();

        Mockito.when(snapshotProvider.getInitialSnapshot()).thenReturn(initialSnapshot);
        Mockito.when(snapshotProvider.getLatestSnapshot()).thenReturn(latestSnapshot);

        return snapshotProvider;
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////
}
