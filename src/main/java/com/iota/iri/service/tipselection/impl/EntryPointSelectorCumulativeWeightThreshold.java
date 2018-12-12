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
        Hash solidTip = tipsViewModel.getRandomSolidTipHash();
        Objects.requireNonNull(solidTip, "Failed to get random tip, most likely a bootstrapping issue.");

        return backtrack(solidTip, threshold);
    }

    public Hash backtrack(Hash tip, int threshold) throws Exception {
        Hash currentHash = tip;
        Integer currentWeight = 0;

        // Backtrack as long as the genesis hasn't been reached and the tresholed has not been crossed
        while (currentWeight < threshold && !Objects.equals(currentHash, Hash.NULL_HASH)) {
            currentHash = TransactionViewModel.fromHash(tangle, currentHash).getTrunkTransactionHash();
            currentWeight = cumulativeWeightCalculator.calculateSingle(currentHash);
        }

        return currentHash;
    }
}