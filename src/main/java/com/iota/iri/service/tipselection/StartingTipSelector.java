package com.iota.iri.service.tipselection;

import com.iota.iri.model.Hash;

/**
 * Functional interface for selecting a tip to start backtracking from,
 * used in EntryPointSelectorCumulativeWeightThreshold.
 */

public interface StartingTipSelector {
    Hash getTip() throws Exception;
}
