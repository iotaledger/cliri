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

    public Hash backtrack(Hash tip, int threshold) throws Exception {
        Hash currentHash = tip;
        Integer currentWeight = 0;

        // Stop when reaching the genesis or the cumulative weight threshold
        while (currentWeight < threshold && !Objects.equals(currentHash, Hash.NULL_HASH)) {
            currentHash = TransactionViewModel.fromHash(tangle, currentHash).getTrunkTransactionHash();
            currentWeight = cumulativeWeightCalculator.calculate(currentHash).get(currentHash);
        }

        return currentHash;
    }
}
