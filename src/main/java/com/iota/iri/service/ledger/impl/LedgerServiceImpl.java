package com.iota.iri.service.ledger.impl;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.ledger.LedgerException;
import com.iota.iri.service.ledger.LedgerService;
import com.iota.iri.service.snapshot.SnapshotProvider;
import com.iota.iri.service.snapshot.impl.SnapshotStateDiffImpl;
import com.iota.iri.storage.Tangle;

import java.util.*;

/**
 * Creates a service instance that allows us to perform ledger state specific operations.<br />
 * <br />
 * This class is stateless and does not hold any domain specific models.<br />
 */
public class LedgerServiceImpl implements LedgerService {
    /**
     * Holds the tangle object which acts as a database interface.<br />
     */
    private Tangle tangle;

    /**
     * Holds the snapshot provider which gives us access to the relevant snapshots.<br />
     */
    private SnapshotProvider snapshotProvider;

    /**
     * Initializes the instance and registers its dependencies.<br />
     * <br />
     * It simply stores the passed in values in their corresponding private properties.<br />
     * <br />
     * Note: Instead of handing over the dependencies in the constructor, we register them lazy. This allows us to have
     *       circular dependencies because the instantiation is separated from the dependency injection. To reduce the
     *       amount of code that is necessary to correctly instantiate this class, we return the instance itself which
     *       allows us to still instantiate, initialize and assign in one line - see Example:<br />
     *       <br />
     *       {@code ledgerService = new LedgerServiceImpl().init(...);}
     *
     * @param tangle Tangle object which acts as a database interface
     * @param snapshotProvider snapshot provider which gives us access to the relevant snapshots
     * @param snapshotService service instance of the snapshot package that gives us access to packages' business logic
     * @return the initialized instance itself to allow chaining
     */
    public LedgerServiceImpl init(Tangle tangle, SnapshotProvider snapshotProvider) {

        this.tangle = tangle;
        this.snapshotProvider = snapshotProvider;

        return this;
    }

    @Override
    public void restoreLedgerState() throws LedgerException {
        try {
        } catch (Exception e) {
            throw new LedgerException("unexpected error while restoring the ledger state", e);
        }
    }

    @Override
    public boolean tipsConsistent(List<Hash> tips) throws LedgerException {
        return true;
    }

    @Override
    public boolean isBalanceDiffConsistent(Set<Hash> approvedHashes, Map<Hash, Long> diff, Hash tip) throws
            LedgerException {

        try {
            if (!TransactionViewModel.fromHash(tangle, tip).isSolid()) {
                return false;
            }
        } catch (Exception e) {
            throw new LedgerException("failed to check the consistency of the balance changes", e);
        }

        if (approvedHashes.contains(tip)) {
            return true;
        }
        Set<Hash> visitedHashes = new HashSet<>(approvedHashes);
        Map<Hash, Long> currentState = generateBalanceDiff(visitedHashes, tip);
        if (currentState == null) {
            return false;
        }
        diff.forEach((key, value) -> {
            if (currentState.computeIfPresent(key, ((hash, aLong) -> value + aLong)) == null) {
                currentState.putIfAbsent(key, value);
            }
        });
        boolean isConsistent = snapshotProvider.getLatestSnapshot().patchedState(new SnapshotStateDiffImpl(
                currentState)).isConsistent();
        if (isConsistent) {
            diff.putAll(currentState);
            approvedHashes.addAll(visitedHashes);
        }
        return isConsistent;
    }

    @Override
    public Map<Hash, Long> generateBalanceDiff(Set<Hash> visitedTransactions, Hash startTransaction)
            throws LedgerException {
        throw new LedgerException("CLIRI ledger logic not implemented yet.");
    }
}
