package com.iota.iri.service.tipselection;

import com.iota.iri.model.Hash;

/**
 * Selects an {@code entryPoint} for tip selection.
 * <p>
 * this point is used as the starting point for {@link Walker#walk(Hash, UnIterableMap, WalkValidator)}
 * </p>
 */

public interface EntryPointSelector {
    /**
     *get an entryPoint for tip selection
     *
     * @return  Entry point for walk method
     * @throws Exception If DB fails to retrieve transactions
     */
    Hash getEntryPoint() throws Exception;
}
