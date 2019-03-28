package com.iota.iri.utils.dag;

import java.util.Collection;

import com.iota.iri.model.Hash;

/**
 * Get N most recent transactions
 */
public interface RecentTransactionsGetter {
    Collection<Hash> getRecentTransactions(int count) throws Exception;
}