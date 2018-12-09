package com.iota.iri.service.tipselection.impl;

import java.util.Objects;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.EntryPointSelector;
import com.iota.iri.storage.Tangle;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Implementation of <tt>EntryPointSelector</tt> that backtracks via trunkTransaction
 * until reaching a minimum Cumulative Weight or the genesis.
 */
public class EntryPointSelectorCumulativeWeightThreshold implements EntryPointSelector {
    public final Tangle tangle;
    private final CumulativeWeightCalculator cumulativeWeightCalculator;
    private final TipsViewModel tipsViewModel;
    private final int threshold;

    public EntryPointSelectorCumulativeWeightThreshold(Tangle tangle, TipsViewModel tipsViewModel, int threshold) {
        this.tangle = tangle;
        this.cumulativeWeightCalculator = new CumulativeWeightCalculator(tangle);
        this.tipsViewModel = tipsViewModel;
        this.threshold = threshold;
    }

    @Override
    public Hash getEntryPoint(int depth) {
        throw new NotImplementedException("Not supported");
    }

    @Override
    public Hash getEntryPoint() throws Exception {
        return backtrack(tipsViewModel.getRandomSolidTipHash(), threshold);
    }

    /**
     * Backtracks from tip until it reaches a transaction with a cumulative weight
     * of at least threshold.
     * 
     * Uses an exponential search to avoid running the expensive CW computations
     * more than necessary.
     */
    public Hash backtrack(Hash tip, int threshold) throws Exception {
        Hash currentHash = tip;
        Integer currentWeight = 1;
        int stepSize = 1;

        // Backtrack as long as the genesis hasn't been reached and the tresholed has not been crossed
        while (currentWeight < threshold && !isGenesis(currentHash)) {
            currentHash = nStepsBack(currentHash, stepSize);
            currentWeight = cumulativeWeightCalculator.calculateSingle(currentHash);
            stepSize *= 2;
        }

        return currentHash;
    }

    private Hash nStepsBack(Hash from, int steps) throws Exception {
        Hash currentHash = from;
        int i = 0;

        while (i < steps && !isGenesis(currentHash)) {
            currentHash = TransactionViewModel.fromHash(tangle, currentHash).getTrunkTransactionHash();
            i++;
        }

        return currentHash;
    }

    private boolean isGenesis(Hash transaction) {
        return Objects.equals(transaction, Hash.NULL_HASH);
    }
}
