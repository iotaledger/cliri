package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.ApproveeViewModel;
import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.StartingTipSelector;
import com.iota.iri.storage.Tangle;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class ConnectedComponentsStartingTipSelector implements StartingTipSelector {
    public final Tangle tangle;

    private final int maxTransactions;
    private final int maxInitialTips;
    private final Random random;
    private final TipsViewModel tipsViewModel;

    public ConnectedComponentsStartingTipSelector(Tangle tangle, int maxTransactions, TipsViewModel tipsViewModel) {
        this.tangle = tangle;
        this.maxTransactions = maxTransactions;
        this.maxInitialTips = maxTransactions / 4;
        this.tipsViewModel = tipsViewModel;
        this.random = new SecureRandom();
    }

    @Override
    public Hash getTip() throws Exception {
        List<Hash> latestTips = tipsViewModel.getLatestSolidTips(maxInitialTips);
        Collection<Hash> latestTransactions = this.findNMostRecentTransactions(latestTips);
        Collection<Set<Hash>> components = this.getConnectedComponents(latestTransactions);
        return this.randomlySelectTipFromLargestConnectedComponent(components, latestTips);
    }

    private Collection<Set<Hash>> getConnectedComponents(Collection<Hash> transactions) throws Exception {
        Set<Hash> unvisited = new HashSet<>(transactions);
        List<Set<Hash>> result = new ArrayList<>();

        // Outer loop iterates once for each component
        while (!unvisited.isEmpty()) {
            Set<Hash> currentComponent = new HashSet<>();

            // Perform DFS scan to find all elements in this component
            Deque<Hash> stack = new ArrayDeque<>();
            stack.push(unvisited.iterator().next());
            while (!stack.isEmpty()) {
                Hash currentHash = stack.pop();

                if (!currentComponent.contains(currentHash)) {
                    unvisited.remove(currentHash);
                    currentComponent.add(currentHash);

                    getAdjacent(currentHash).stream()
                        .filter(unvisited::contains)
                        .forEach(stack::push);
                }
            }

            result.add(currentComponent);
        }

        return result;
    }

    private Collection<Hash> getAdjacent(Hash hash) throws Exception {
        Collection<Hash> result = new HashSet<>();

        TransactionViewModel transaction = TransactionViewModel.fromHash(tangle, hash);
        result.addAll(ApproveeViewModel.load(tangle, hash).getHashes());
        result.add(transaction.getBranchTransactionHash());
        result.add(transaction.getTrunkTransactionHash());

        return result;
    }

    private Collection<Hash> findNMostRecentTransactions(Collection<Hash> tips) throws Exception {

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

    private Hash randomlySelectTipFromLargestConnectedComponent(Collection<Set<Hash>> connectedComponents,
                                                               Collection<Hash> tips) {
        Collection<Hash> largestComponent = connectedComponents.stream()
                .max(Comparator.comparing(Collection::size))
                .orElse(Collections.emptySet());

        List<Hash> tipsInLargestComponent = tips.stream()
                .filter(largestComponent::contains)
                .collect(Collectors.toList());

        if (tipsInLargestComponent.isEmpty()) {
            throw new IllegalStateException("no tips found in largest connected component.");
        }

        return tipsInLargestComponent.get(random.nextInt(tipsInLargestComponent.size()));
    }

    private TransactionViewModel fromHash(Hash hash) {
        try {
            return TransactionViewModel.fromHash(tangle, hash);
        } catch (Exception e) {
            throw new RuntimeException("failed to load transaction");
        }
    }
}
