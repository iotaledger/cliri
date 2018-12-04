package com.iota.iri.service.tipselection;

import com.iota.iri.model.Hash;
import com.iota.iri.model.HashId;
import com.iota.iri.utils.collections.interfaces.UnIterableMap;

/**
 * Calculates the rating for a sub graph
 */
public interface RatingCalculator {

    /**
     * Rating calculator
     * <p>
     * Calculates the rating of all the transactions that reference
     * a given entry point.
     * </p>
     *
     * @param entryPoint  Transaction hash of a selected entry point.
     * @return  Map
     * @throws Exception If DB fails to retrieve transactions
     */

    UnIterableMap<HashId, Integer> calculate(Hash entryPoint) throws Exception;

    /**
     * Calculates rating for a single transactions
     *
     * @param transaction Transaction hash of transaction in question
     * @return  Map
     * @throws Exception If DB fails to retrieve transaction
     */
    Integer calculateSingle(Hash transaction) throws Exception;
}
