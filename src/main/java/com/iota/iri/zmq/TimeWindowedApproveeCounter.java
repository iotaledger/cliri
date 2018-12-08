package com.iota.iri.zmq;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.dag.DAGHelper;
import com.iota.iri.utils.dag.TraversalException;

import java.util.HashSet;

/**
 * Utility class to count the number of distinct approvees that have an arrival time within the given time window.
 */
class TimeWindowedApproveeCounter {

    private final Tangle tangle;
    private final long minTransactionAgeSeconds;
    private final long maxTransactionAgeSeconds;

    TimeWindowedApproveeCounter(Tangle tangle, long minTransactionAgeSeconds, long maxTransactionAgeSeconds) {
        this.tangle = tangle;
        this.minTransactionAgeSeconds = minTransactionAgeSeconds;
        this.maxTransactionAgeSeconds = maxTransactionAgeSeconds;
    }

    private boolean continueTraversal(long now, TransactionViewModel transactionViewModel) {
        // stop the traversing as soon as the transaction is older than the max
        final long age = now - transactionViewModel.getArrivalTime();
        return age <= maxTransactionAgeSeconds;
    }

    /**
     * Checks whether the transaction is in the validity time window.
     * 
     * @param now         current time epoch in seconds
     * @param transaction transaction to check
     * @return true, if the arrival time is in the time window, false otherwise
     */
    boolean isInTimeWindow(long now, TransactionViewModel transaction) {

        final long age = now - transaction.getArrivalTime();
        return (age >= minTransactionAgeSeconds && age <= maxTransactionAgeSeconds);
    }

    /**
     * Counts the number of distinct approvees of the starting transaction, that are within the validity time window.
     * 
     * The approvee traversal is not continued for transactions that are older than the maximum age.
     *
     * @param now                     current time epoch in seconds
     * @param startingTransactionHash the starting point of the traversal
     * @param processedTransactions   a set of hashes that shall be considered as "processed" already and that will
     *                                consequently be ignored in the traversal
     * @throws TraversalException if anything goes wrong while traversing the graph and processing the transactions
     */
    long getCount(long now, Hash startingTransactionHash, HashSet<Hash> processedTransactions)
            throws TraversalException {

        DAGHelper helper = DAGHelper.get(tangle);

        ValidTransactionCounter transactionCounter = new ValidTransactionCounter(now);
        helper.traverseApprovees(startingTransactionHash, t -> continueTraversal(now, t), transactionCounter::consume,
                processedTransactions);

        return transactionCounter.getCount();
    }

    /**
     * Counts the consumed transactions that arrived in the valid interval.
     *
     * It is assumed that the same transaction is not consumed more than once.
     */
    private final class ValidTransactionCounter {

        final long now;

        long count = 0;

        private ValidTransactionCounter(long now) {
            this.now = now;
        }

        private void consume(TransactionViewModel transactionViewModel) {
            if (isInTimeWindow(now, transactionViewModel)) {
                count++;
            }
        }

        private long getCount() {
            return count;
        }
    }
}
