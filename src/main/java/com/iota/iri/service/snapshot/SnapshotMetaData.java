package com.iota.iri.service.snapshot;

import com.iota.iri.model.Hash;

/**
 * Represents the meta data of a snapshot.
 *
 */
public interface SnapshotMetaData {
    /**
     * Getter of the hash of the transaction that the snapshot was "derived" from.
     *
     * In case of the "latest" {@link Snapshot} this value will be the hash of the "initial" {@link Snapshot}.
     *
     * Note: a snapshot can be modified over time, as we apply the balance changes caused by consecutive transactions,
     *       so this value differs from the value returned by {@link #getHash()}.
     *
     * @return hash of the transaction that the snapshot was "derived" from
     */
    Hash getInitialHash();

    /**
     * Setter of the hash of the transaction that the snapshot was "derived" from.
     *
     * Note: After creating a new local {@link Snapshot} we update this value in the latest {@link Snapshot} to
     *       correctly reflect the new "origin" of the latest {@link Snapshot}.
     *
     * @param initialHash hash of the transaction that the snapshot was "derived" from
     */
    void setInitialHash(Hash initialHash);
    /**
     *  Getter of the timestamp of the transaction that the snapshot was "derived" from.
     *
     *  In case of the "latest" {@link Snapshot} this value will be the timestamp of the "initial" {@link Snapshot}.
     *
     * Note: a snapshot can be modified over time, as we apply the balance changes caused by consecutive transactions,
     *       so this value differs from the value returned by {@link #getTimestamp()}.
     *
     * @return timestamp of the transaction that the snapshot was "derived" from
     */
    long getInitialTimestamp();

    /**
     * Setter of the timestamp of the transaction that the snapshot was "derived" from.
     *
     * Note: After creating a new local {@link Snapshot} we update this value in the latest {@link Snapshot} to
     *       correctly reflect the new "origin" of the latest {@link Snapshot}.
     *
     * @param initialTimestamp timestamp of the transaction that the snapshot was "derived" from
     */
    void setInitialTimestamp(long initialTimestamp);

    /**
     * Getter of the hash of the transaction that the snapshot currently belongs to.
     *
     * @return hash of the transaction that the snapshot currently belongs to
     */
    Hash getHash();

    /**
     * Setter of the hash of the transaction that the snapshot currently belongs to.
     *
     * @param hash hash of the transaction that the snapshot currently belongs to
     */
    void setHash(Hash hash);

    /**
     * Getter of the timestamp of the transaction that the snapshot currently belongs to.
     *
     * @return timestamp of the transaction that the snapshot currently belongs to
     */
    long getTimestamp();

    /**
     * Setter of the timestamp of the transaction that the snapshot currently belongs to.
     *
     * @param timestamp timestamp of the transaction that the snapshot currently belongs to
     */
    void setTimestamp(long timestamp);

    /**
     * Replaces the meta data values of this instance with the values of another meta data object.
     *
     * This can for example be used to "reset" the meta data after a failed modification attempt (while being able to
     * keep the same instance).
     *
     * @param newMetaData the new meta data that shall overwrite the current one
     */
    void update(SnapshotMetaData newMetaData);
}
