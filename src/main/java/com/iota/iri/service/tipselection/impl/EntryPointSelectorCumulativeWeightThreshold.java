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

    public static int MAX_SUBTANGLE_SIZE = CumulativeWeightCalculator.MAX_FUTURE_SET_SIZE;

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
        Hash entryPoint = backtrack(getTip(), threshold);

        int subtangleWeight = cumulativeWeightCalculator.calculateSingle(entryPoint);
        if (subtangleWeight > MAX_SUBTANGLE_SIZE) {
            throw new IllegalStateException(String.format(
                "The selected entry point's subtangle size is too big. EntryPoint Hash: %s Subtangle size: %d",
                entryPoint, subtangleWeight));
        }

        return entryPoint;
    }

    private Hash getTip() throws Exception {
        Hash solidTip = tipsViewModel.getRandomSolidTipHash();

        if (solidTip == null) {
            solidTip = unbiasedWalk();
        }

        return solidTip;
    }

    private Hash unbiasedWalk() throws Exception {
        // Start at the genesis
        Hash genesis = Hash.NULL_HASH;

        UnIterableMap<HashId, Integer> ratings = new RatingOne(tangle).calculate(genesis);
        
        return walker.walk(genesis, ratings, walkValidator);
    }

    /**
     * Backtracks from tip until it reaches a transaction with a cumulative weight
     * of at least {@code threshold}.
     * 
     * Uses an exponential search to avoid running the expensive CW computations
     * more than necessary.
     */
    private Hash backtrack(Hash tip, int threshold) throws Exception {
        Hash currentHash = tip;
        int currentWeight = 1;
        int stepSize = 1;

        // Backtrack as long as the genesis hasn't been reached and the threshold has not been crossed
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

    private static boolean isGenesis(Hash transaction) {
        return Objects.equals(transaction, Hash.NULL_HASH);
    }
}
