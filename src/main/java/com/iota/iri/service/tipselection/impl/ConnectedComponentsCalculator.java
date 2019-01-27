package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;

import java.util.*;

public class ConnectedComponentsCalculator {
    public final Tangle tangle;

    public static final int N = 5000;

    public ConnectedComponentsCalculator(Tangle tangle) {
        this.tangle = tangle;
    }

    //find N most recent transactions
    public Collection<Hash> findNMostRecentTransactions(Collection<Hash> tips) throws Exception {

        Collection<Hash> result = new HashSet<>(N);

        //using a max heap sorted by arrivalTime.
        Queue<TransactionViewModel> queue = new PriorityQueue<>(N,
                Comparator.comparingLong(a -> -1 * a.getArrivalTime()));

        //the heap is initialized with the tips.
        tips.stream().map(this::fromHash).forEach(queue::add);

        while (!queue.isEmpty() && result.size() < N) {
            TransactionViewModel current = queue.poll();
            result.add(current.getHash());

            //add children to priority queue, only if not already chosen.
            if (!result.contains(current.getTrunkTransactionHash())) {
                queue.add(current.getTrunkTransaction(tangle));
            }
            if (!result.contains(current.getBranchTransactionHash())
            && !current.getTrunkTransactionHash().equals(current.getBranchTransactionHash())) {
                queue.add(current.getBranchTransaction(tangle));
            }
        }

        return result;
    }

    //find connected components in `N` (step 2)

    //randomly select a tip from the largest CC (step 3)

    private TransactionViewModel fromHash(Hash hash) {
        try {
            return TransactionViewModel.fromHash(tangle, hash);
        } catch (Exception e) {
            //do nothing
        }
        return null;
    }
}
