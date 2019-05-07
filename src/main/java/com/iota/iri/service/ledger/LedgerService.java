package com.iota.iri.service.ledger;

import com.iota.iri.model.Hash;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the service that contains all the relevant business logic for modifying and calculating the ledger
 * state.<br />
 * <br />
 * This class is stateless and does not hold any domain specific models.<br />
 */
public interface LedgerService {
    /**
     * Restores the ledger state after a restart of IRI, which allows us to fast forward to the point where we
     * stopped before the restart.<br />
     * <br />
     *
     * @throws LedgerException if anything unexpected happens while trying to restore the ledger state
     */
    void restoreLedgerState() throws LedgerException;

    /**
     * Checks the consistency of the combined balance changes of the given tips.<br />
     * <br />
     * It simply calculates the balance changes of the tips and then combines them to verify that they are leading to a
     * consistent ledger state (which means that they are not containing any double-spends or spends of non-existent
     * IOTA).<br />
     *
     * @param hashes a list of hashes that reference the chosen tips
     * @return {@code true} if the tips are consistent and {@code false} otherwise
     * @throws LedgerException if anything unexpected happens while checking the consistency of the tips
     */
    boolean tipsConsistent(List<Hash> hashes) throws LedgerException;

    /**
     * Checks if the balance changes of the transactions that are referenced by the given tip are consistent.<br />
     * <br />
     * It first calculates the balance changes, then adds them to the given {@code diff} and finally checks their
     * consistency. If we are only interested in the changes that are referenced by the given {@code tip} we need to
     * pass in an empty map for the {@code diff} parameter.<br />
     * <br />
     * The {@code diff} as well as the {@code approvedHashes} parameters are modified, so they will contain the new
     * balance changes and the approved transactions after this method terminates.<br />
     *
     * @param approvedHashes a set of transaction hashes that shall be considered to be approved already (and that
     *                       consequently shall be excluded from the calculation)
     * @param diff a map of balances associated to their address that shall be used as a basis for the balance
     * @param tip the tip that will have its approvees checked
     * @return {@code true} if the balance changes are consistent and {@code false} otherwise
     * @throws LedgerException if anything unexpected happens while determining the consistency
     */
    boolean isBalanceDiffConsistent(Set<Hash> approvedHashes, Map<Hash, Long> diff, Hash tip) throws LedgerException;

    /**
     * Generates the accumulated balance changes of the transactions that are directly or indirectly referenced by the
     * given transaction.
     *
     * @param visitedTransactions a set of transaction hashes that shall be considered to be visited already
     * @param startTransaction the transaction that marks the start of the dag traversal and that has its approovees
     *                        examined
     * @return a map of the balance changes (addresses associated to their balance) or {@code null} if the balance could
     *         not be generated due to inconsistencies
     * @throws LedgerException if anything unexpected happens while generating the balance changes
     */
    Map<Hash, Long> generateBalanceDiff(Set<Hash> visitedTransactions, Hash startTransaction)
            throws LedgerException;
}
