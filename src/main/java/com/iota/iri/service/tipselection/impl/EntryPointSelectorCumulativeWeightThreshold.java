package com.iota.iri.service.tipselection.impl;

import java.util.Objects;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.model.HashId;
import com.iota.iri.service.tipselection.EntryPointSelector;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.collections.interfaces.UnIterableMap;
import com.iota.iri.service.tipselection.WalkValidator;
import com.iota.iri.service.tipselection.Walker;

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
    private final Walker walker;
    private final WalkValidator walkValidator;

    public EntryPointSelectorCumulativeWeightThreshold(Tangle tangle, TipsViewModel tipsViewModel, int threshold, Walker walker, WalkValidator walkValidator) {
        this.tangle = tangle;
        this.cumulativeWeightCalculator = new CumulativeWeightCalculator(tangle);
        this.tipsViewModel = tipsViewModel;
        this.threshold = threshold;
        this.walker = walker;
        this.walkValidator = walkValidator;
    }

    @Override
    public Hash getEntryPoint(int depth) {
        throw new NotImplementedException("Not supported");
    }

    @Override
    public Hash getEntryPoint() throws Exception {
        return backtrack(getTip(), threshold);
    }

    private Hash getTip() throws Exception {
        Hash solidTip = tipsViewModel.getRandomSolidTipHash();

        solidTip = solidTip == null ? unbiasedWalk() : solidTip;

        return solidTip;
    }

    private Hash unbiasedWalk() throws Exception {
        // Start at the genesis
        Hash genesis = Hash.NULL_HASH;

        UnIterableMap<HashId, Integer> ratings = new RatingOne(tangle).calculate(genesis);
        
        return walker.walk(genesis, ratings, walkValidator);
    }

    
    private Hash backtrack(Hash tip, int threshold) throws Exception {
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
