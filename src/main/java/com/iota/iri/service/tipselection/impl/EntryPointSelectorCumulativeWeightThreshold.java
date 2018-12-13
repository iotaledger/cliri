package com.iota.iri.service.tipselection.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import com.iota.iri.controllers.ApproveeViewModel;
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

    public static int MAX_SUBTANGLE_SIZE = 4 * CumulativeWeightCalculator.MAX_FUTURE_SET_SIZE;

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
            // If there are no known tips, start walking from the genesis
            // and choose the heighest tip
            solidTip = getHeighestTip();
        }

        return solidTip;
    }

    /**
     * Perform BFS scan from the genesis to find all transactions'
     * heights, defined to be the distance from the genesis.
     * @return a map from transactions to heights, ordered in ascending height order
     */
    Hash getHeighestTip() throws Exception {
        HashSet<Hash> visited = new HashSet<>();
  
        // Create a queue for BFS
        Queue<Hash> queue = new LinkedList<>(); 
  
        // Mark the genesis as visited with distance 0, and enqueue it 
        visited.add(Hash.NULL_HASH);
        queue.add(Hash.NULL_HASH); 
  
        Hash currentHash = Hash.NULL_HASH;
        while (queue.size() != 0) 
        { 
            currentHash = queue.poll(); 
  
            // Get all approvers, add unvisited to queue and add them to the visited set
            for (Hash approver : ApproveeViewModel.load(tangle, currentHash).getHashes()) {
                if (!visited.contains(approver)) {
                    visited.add(approver);
                    queue.add(approver);
                }
            }
        } 

        return currentHash;
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
