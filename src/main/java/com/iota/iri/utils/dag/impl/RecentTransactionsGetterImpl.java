package com.iota.iri.utils.dag.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.dag.RecentTransactionsGetter;

/**
 * Get N most recent transactions
 */
public class RecentTransactionsGetterImpl implements RecentTransactionsGetter {
    private TipsViewModel tipsViewModel;
    private Tangle tangle;

    public RecentTransactionsGetterImpl(TipsViewModel tipsViewModel, Tangle tangle) {
        this.tipsViewModel = tipsViewModel;
        this.tangle = tangle;
    }

    @Override
    public Collection<Hash> getRecentTransactions(int count) throws Exception {
        List<Hash> latestTips = tipsViewModel.getLatestSolidTips(count);
        return findNMostRecentTransactions(latestTips, count);
    }

    private Collection<Hash> findNMostRecentTransactions(Collection<Hash> tips, int count) throws Exception {
        int maxTransactions = tips.size() + count * 2;
        Comparator<TransactionViewModel> comparator = Comparator.comparingLong(a -> (-1) * a.getArrivalTime());

        Queue<TransactionViewModel> result = new PriorityQueue<>(maxTransactions, comparator);

        //using a max heap sorted by arrivalTime.
        Queue<TransactionViewModel> queue = new PriorityQueue<>(maxTransactions, comparator);
        Set<Hash> visited = new HashSet<>(maxTransactions);

        //the heap is initialized with the tips.
        tips.stream().map(this::fromHash).forEach(queue::add);
        visited.addAll(tips);

        while (!queue.isEmpty() && result.size() < maxTransactions) {
            TransactionViewModel current = queue.poll();
            result.add(current);

            //add children to priority queue, only if not already chosen.
            Hash trunkHash = current.getTrunkTransactionHash();
            Hash branchHash = current.getBranchTransactionHash();

            List<Hash> approvees = Arrays.asList(trunkHash, branchHash);
            for (Hash approvee: approvees) {
                if (!visited.contains(approvee)) {
                    queue.add(TransactionViewModel.fromHash(tangle, approvee));
                    visited.add(approvee);
                }
            }
        }

        // Return the first count elements
        return result.stream()
            .limit(count)
            .map(t -> t.getHash())
            .collect(Collectors.toList());
    }

    private TransactionViewModel fromHash(Hash hash) {
        try {
            return TransactionViewModel.fromHash(tangle, hash);
        } catch (Exception e) {
            throw new RuntimeException("failed to load transaction");
        }
    }
}