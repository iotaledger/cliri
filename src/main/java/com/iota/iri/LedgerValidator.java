package com.iota.iri;

import com.iota.iri.model.Hash;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LedgerValidator {

    public LedgerValidator() {
    }

    /**
     * Initializes the LedgerValidator. This updates the latest milestone and solid subtangle milestone, and then
     * builds up the confirmed until it reaches the latest consistent confirmed. If any inconsistencies are detected,
     * perhaps by database corruption, it will delete the milestone confirmed and all that follow.
     * It then starts at the earliest consistent milestone index with a confirmed, and analyzes the tangle until it
     * either reaches the latest solid subtangle milestone, or until it reaches an inconsistent milestone.
     * @throws Exception
     */
    protected void init() throws Exception {
    }

    public boolean checkConsistency(List<Hash> hashes) throws Exception {
        return true;
    }

    public boolean updateDiff(Set<Hash> approvedHashes, final Map<Hash, Long> diff, Hash tip) throws Exception {
        return true;
    }
}
