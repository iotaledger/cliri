package com.iota.iri.service.tipselection.impl;

import com.iota.iri.controllers.ApproveeViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.StartingTipSelector;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.dag.RecentTransactionsGetter;

import java.security.SecureRandom;
import java.util.*;

public class ConnectedComponentsStartingTipSelector implements StartingTipSelector {
    public final Tangle tangle;

    private final int maxTransactions;
    private final Random random;
    private RecentTransactionsGetter recentTransactionsGetter;

    public ConnectedComponentsStartingTipSelector(Tangle tangle, int maxTransactions, RecentTransactionsGetter recentTransactionsGetter) {
        this.tangle = tangle;
        this.maxTransactions = maxTransactions;
        this.random = new SecureRandom();
        this.recentTransactionsGetter = recentTransactionsGetter;
    }

    @Override
    public Hash getTip() throws Exception {
        Collection<Hash> latestTransactions = this.recentTransactionsGetter.getRecentTransactions(this.maxTransactions);
        Collection<Set<Hash>> components = this.getConnectedComponents(latestTransactions);
        return this.randomlySelectTransactionFromLargestConnectedComponent(components);
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

    private Hash randomlySelectTransactionFromLargestConnectedComponent(Collection<Set<Hash>> connectedComponents) {
        Collection<Hash> largestComponent = connectedComponents.stream()
                .max(Comparator.comparing(Collection::size))
                .orElse(Collections.emptySet());

        return largestComponent.stream()
            .skip(random.nextInt(largestComponent.size()))
            .findFirst()
            .orElse(Hash.NULL_HASH);
    }

}
