package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;

import java.util.*;

public class ConnectedComponentsCalculator {
    public final Tangle tangle;

    private final int maxTransactions;

    public ConnectedComponentsCalculator(Tangle tangle, int maxTransactions) {
        this.maxTransactions = maxTransactions;
        this.tangle = tangle;
    }

    public Collection<Hash> findNMostRecentTransactions(Collection<Hash> tips) throws Exception {

        Collection<Hash> result = new HashSet<>(maxTransactions);

        //using a max heap sorted by arrivalTime.
        Queue<TransactionViewModel> queue = new PriorityQueue<>(maxTransactions,
                Comparator.comparingLong(a -> (-1) * a.getArrivalTime()));

        //the heap is initialized with the tips.
        tips.stream().map(this::fromHash).forEach(queue::add);

        while (!queue.isEmpty() && result.size() < maxTransactions) {
            TransactionViewModel current = queue.poll();
            result.add(current.getHash());

            //add children to priority queue, only if not already chosen.
            Hash trunkHash = current.getTrunkTransactionHash();
            Hash branchHash = current.getBranchTransactionHash();

            if (!result.contains(trunkHash)) {
                queue.add(current.getTrunkTransaction(tangle));
            }
            if (!trunkHash.equals(branchHash) && !result.contains(branchHash)) {
                queue.add(current.getBranchTransaction(tangle));
            }
        }

        return result;
    }

    private TransactionViewModel fromHash(Hash hash) {
        try {
            return TransactionViewModel.fromHash(tangle, hash);
        } catch (Exception e) {
            throw new RuntimeException("failed to load transaction");
        }
    }
}
