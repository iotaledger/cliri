package com.iota.iri.service.tipselection.impl;

import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.EntryPointSelector;

/**
 * Implementation of <tt>EntryPointSelector</tt> that always returns the Genesis transaction:
 * {@link Hash#NULL_HASH}
 * Used to as a starting point for the random walk.
 */
public class EntryPointSelectorGenesisImpl implements EntryPointSelector {

    public EntryPointSelectorGenesisImpl() {
    }

    @Override
    public Hash getEntryPoint() {
        return Hash.NULL_HASH;
    }
}
