package com.iota.iri.service.tipselection.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.ReferenceChecker;
import com.iota.iri.storage.Tangle;

import org.apache.commons.collections4.CollectionUtils;

public class ReferenceCheckerImpl implements ReferenceChecker {
    private Tangle tangle;

    public ReferenceCheckerImpl(Tangle tangle) {
        this.tangle = tangle;
    }

    @Override
    public boolean doesReference(Hash referencer, Hash target) throws Exception {
        if (!TransactionViewModel.exists(tangle, referencer) ||
            !TransactionViewModel.exists(tangle, target)) {
                throw new IllegalStateException("Transactions not found");
        }

        HashSet<Hash> visitedHashes = new HashSet<>();
        Queue<Hash> queue = new LinkedList<>();

        // Run BFS scan to look for approvee
        queue.add(referencer);
        visitedHashes.add(referencer);

        while (CollectionUtils.isNotEmpty(queue)) {
            Hash currentHash = queue.remove();

            if (Objects.equals(currentHash, target)) {
                return true;
            }

            TransactionViewModel current = TransactionViewModel.fromHash(tangle, currentHash);
            List<Hash> approveeHashes = Arrays.asList(
                current.getTrunkTransactionHash(),
                current.getBranchTransactionHash());

            for (Hash approveeHash : approveeHashes) {
                if(approveeHash != null && !visitedHashes.contains(approveeHash)) {
                    queue.add(approveeHash);
                    visitedHashes.add(approveeHash);
                }
            }
        }

        return false;
    }
}
