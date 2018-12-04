package com.iota.iri;

import com.iota.iri.model.Hash;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LedgerValidatorImpl implements LedgerValidator {

    public boolean checkConsistency(List<Hash> hashes) {
        return true;
    }

    public boolean updateDiff(Set<Hash> approvedHashes, final Map<Hash, Long> diff, Hash tip) {
        return true;
    }
}
