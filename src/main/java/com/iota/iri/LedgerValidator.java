package com.iota.iri;

import com.iota.iri.model.Hash;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LedgerValidator {

    public LedgerValidator() {
    }

    /**
     * Initializes the LedgerValidator.
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
