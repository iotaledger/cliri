package com.iota.iri;

import com.iota.iri.controllers.*;
import com.iota.iri.model.Hash;
import com.iota.iri.network.TransactionRequester;
import com.iota.iri.zmq.MessageQ;
import com.iota.iri.storage.Tangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LedgerValidator {

    private final Logger log = LoggerFactory.getLogger(LedgerValidator.class);
    private final Tangle tangle;
    private final TransactionRequester transactionRequester;
    private final MessageQ messageQ;
    private volatile int numberOfConfirmedTransactions;

    public LedgerValidator(Tangle tangle, TransactionRequester transactionRequester, MessageQ messageQ) {
        this.tangle = tangle;
        this.transactionRequester = transactionRequester;
        this.messageQ = messageQ;
    }

    /**
     * Returns a Map of Address and change in balance that can be used to build a new Snapshot state.
     * Under certain conditions, it will return null:
     *  - While descending through transactions, if a transaction is marked as {PREFILLED_SLOT}, then its hash has been
     *    referenced by some transaction, but the transaction data is not found in the database. It notifies
     *    TransactionRequester to increase the probability this transaction will be present the next time this is checked.
     *  - When a transaction marked as a tail transaction (if the current index is 0), but it is not the first transaction
     *    in any of the BundleValidator's transaction lists, then the bundle is marked as invalid, deleted, and re-requested.
     *  - When the bundle is not internally consistent (the sum of all transactions in the bundle must be zero)
     * As transactions are being traversed, it will come upon bundles, and will add the transaction value to {state}.
     * If {milestone} is true, it will search, through trunk and branch, all transactions, starting from {tip},
     * until it reaches a transaction that is marked as a "confirmed" transaction.
     * If {milestone} is false, it will search up until it reaches a confirmed transaction, or until it finds a hash that has been
     * marked as consistent since the previous milestone.
     * @param visitedNonMilestoneSubtangleHashes hashes that have been visited and considered as approved
     * @param tip                                the hash of a transaction to start the search from
     * @param latestSnapshotIndex                index of the latest snapshot to traverse to
     * @param milestone                          marker to indicate whether to stop only at confirmed transactions
     * @return {state}                           the addresses that have a balance changed since the last diff check
     * @throws Exception
     */
    public Map<Hash,Long> getLatestDiff(final Set<Hash> visitedNonMilestoneSubtangleHashes, Hash tip, int latestSnapshotIndex, boolean milestone) throws Exception {
        //TODO CLIRI: disabled diff
        return new HashMap<>();
    }

    /**
     * Initializes the LedgerValidator. This updates the latest milestone and solid subtangle milestone, and then
     * builds up the confirmed until it reaches the latest consistent confirmed. If any inconsistencies are detected,
     * perhaps by database corruption, it will delete the milestone confirmed and all that follow.
     * It then starts at the earliest consistent milestone index with a confirmed, and analyzes the tangle until it
     * either reaches the latest solid subtangle milestone, or until it reaches an inconsistent milestone.
     * @throws Exception
     */
    protected void init() throws Exception {
    }

    public boolean checkConsistency(List<Hash> hashes) throws Exception {
        //TODO CLIRI: all transactions will validate.
        return true;
    }

    public boolean updateDiff(Set<Hash> approvedHashes, final Map<Hash, Long> diff, Hash tip) throws Exception {
        //TODO CLIRI: all transactions will validate.
        return true;
    }
}
