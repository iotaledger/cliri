package com.iota.iri.service.tipselection;

import com.iota.iri.model.Hash;

/**
 * Checks if a given transaction references another
 */
public interface ReferenceChecker {
    boolean doesReference(Hash referencer, Hash target) throws Exception;
}
