package com.iota.iri.service.stats;

import java.util.Arrays;
import java.util.Collection;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.dag.RecentTransactionsGetter;

/**
 * Computes the node lag using various metrics
 */
public class LagCalculator {
    private int transactionCount;
    private RecentTransactionsGetter recentTransactionsGetter;
    private Tangle tangle;

    public LagCalculator(int transactionCount, Tangle tangle, RecentTransactionsGetter recentTransactionsGetter) {
        this.transactionCount = transactionCount;
        this.tangle = tangle;
        this.recentTransactionsGetter = recentTransactionsGetter;
    }

    public long getMedianArrivalLag() throws Exception {
          Collection<Hash> recentTransactions = recentTransactionsGetter.getRecentTransactions(transactionCount);

          if (recentTransactions.isEmpty()) {
            return -1;
          }

          Long[] lags = recentTransactions.stream()
              .map(this::fromHash)
              .map(tx -> new Long(Math.abs(tx.getAttachmentTimestamp() - tx.getArrivalTime())))
              .toArray(Long[]::new);
          
          Arrays.sort(lags);

          int middle = lags.length/2;
          long median = lags.length % 2 == 1 ?
              lags[middle] :
              (lags[middle-1] + lags[middle]) / 2;

          return median;
    }

    private TransactionViewModel fromHash(Hash hash) {
        try {
            return TransactionViewModel.fromHash(tangle, hash);
        } catch (Exception e) {
            throw new RuntimeException("failed to load transaction");
        }
    }
}
