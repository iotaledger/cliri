package com.iota.iri;

import com.iota.iri.model.Hash;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LedgerValidator {

    boolean checkConsistency(List<Hash> hashes) throws Exception;

    boolean updateDiff(Set<Hash> approvedHashes, final Map<Hash, Long> diff, Hash tip) throws Exception;

}
