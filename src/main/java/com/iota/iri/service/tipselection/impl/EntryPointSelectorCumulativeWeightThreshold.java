package com.iota.iri.service.tipselection.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Objects;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.EntryPointSelector;
import com.iota.iri.service.tipselection.StartingTipSelector;
import com.iota.iri.service.tipselection.TailFinder;
import com.iota.iri.storage.Tangle;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Implementation of <tt>EntryPointSelector</tt> that backtracks via trunkTransaction 
 * until reaching a minimum Cumulative Weight or the genesis.
 */
public class EntryPointSelectorCumulativeWeightThreshold implements EntryPointSelector {
    public final Tangle tangle;
    private final CumulativeWeightCalculator cumulativeWeightCalculator;
    private final StartingTipSelector startingTipSelector;
    private final int threshold;
    private final TailFinder tailFinder;
    private final SecureRandom random = new SecureRandom();

    public static int MAX_SUBTANGLE_SIZE = 15 * CumulativeWeightCalculator.MAX_FUTURE_SET_SIZE;

    public EntryPointSelectorCumulativeWeightThreshold(Tangle tangle, int threshold,
            StartingTipSelector startingTipSelector, TailFinder tailFinder) {
        this.tangle = tangle;
        this.cumulativeWeightCalculator = new CumulativeWeightCalculator(tangle);
        this.threshold = threshold;
        this.startingTipSelector = startingTipSelector;
        this.tailFinder = tailFinder;
    }

    @Override
    public Hash getEntryPoint(int depth) {
        throw new NotImplementedException("Not supported");
    }

    @Override
    public Hash getEntryPoint() throws Exception {
        Hash entryBundle = backtrack(startingTipSelector.getTip(), threshold);
        Hash entryPoint = tailFinder.findTail(entryBundle).get();

        int subtangleWeight = cumulativeWeightCalculator.calculateSingle(entryPoint);
        if (subtangleWeight > MAX_SUBTANGLE_SIZE) {
            throw new IllegalStateException(String.format(
                "The selected entry point's subtangle size is too big. EntryPoint Hash: %s Subtangle size: %d",
                entryPoint, subtangleWeight));
        }

        return entryPoint;
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

        ArrayList<Hash> path = new ArrayList<>();
        path.add(currentHash);

        // Backtrack as long as the genesis hasn't been reached and the threshold has not been crossed
        while (currentWeight < threshold && !isGenesis(currentHash)) {
            path = nStepsBack(currentHash, stepSize);
            currentHash = path.get(path.size() - 1);
            currentWeight = cumulativeWeightCalculator.calculateSingle(currentHash);
            stepSize *= 2;
        }

        return binarySearch(path, threshold);
    }
    
    private Hash binarySearch(ArrayList<Hash> path, int threshold) throws Exception {
        int left = 0;
        int right = path.size() - 1;
        while (left < right) {
            int middle = (right + left) / 2;
            int weight = cumulativeWeightCalculator.calculateSingle(path.get(middle));

            if (weight < threshold) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }

        return path.get(right);
    }

    private ArrayList<Hash> nStepsBack(Hash from, int steps) throws Exception {
        Hash currentHash = from;
        int i = 0;

        ArrayList<Hash> path = new ArrayList<>();
        path.add(currentHash);

        while (i < steps && !isGenesis(currentHash)) {
            currentHash = random.nextInt() % 2 == 0 ?
                TransactionViewModel.fromHash(tangle, currentHash).getTrunkTransactionHash() :
                TransactionViewModel.fromHash(tangle, currentHash).getBranchTransactionHash();
            
            path.add(currentHash);
            i++;
        }

        return path;
    }

    private static boolean isGenesis(Hash transaction) {
        return Objects.equals(transaction, Hash.NULL_HASH);
    }
}
