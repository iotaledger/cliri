package com.iota.iri.service.snapshot.impl;

import com.iota.iri.model.Hash;
import com.iota.iri.service.snapshot.Snapshot;
import com.iota.iri.service.snapshot.SnapshotException;
import com.iota.iri.service.snapshot.SnapshotMetaData;
import com.iota.iri.service.snapshot.SnapshotState;
import com.iota.iri.service.snapshot.SnapshotStateDiff;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements the basic contract of the {@link Snapshot} interface.
 */
public class SnapshotImpl implements Snapshot {
    /**
     * Holds a reference to the state of this snapshot.
     */
    private final SnapshotState state;

    /**
     * Holds a reference to the metadata of this snapshot.
     */
    private final SnapshotMetaData metaData;

    /**
     * Lock object allowing to block access to this object from different threads.
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
    /**
     * Creates a snapshot object with the given information.
     *
     * It simply stores the passed in parameters in the internal properties.
     *
     * @param state state of the snapshot with the balances of all addresses
     * @param metaData meta data of the snapshot with the additional information of this snapshot
     */
    public SnapshotImpl(SnapshotState state, SnapshotMetaData metaData) {
        this.state = state;
        this.metaData = metaData;
    }

    /**
     * Creates a deep clone of the passed in snapshot.
     *
     * @param snapshot object that shall be cloned
     */
    private SnapshotImpl(SnapshotImpl snapshot) {
        this(
            new SnapshotStateImpl(snapshot.state),
            new SnapshotMetaDataImpl(snapshot.metaData)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lockRead() {
        readWriteLock.readLock().lock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlockRead() {
        readWriteLock.readLock().unlock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lockWrite() {
        readWriteLock.writeLock().lock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlockWrite() {
        readWriteLock.writeLock().unlock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Snapshot snapshot) {
        lockWrite();

        try {
            state.update(((SnapshotImpl) snapshot).state);
            metaData.update(((SnapshotImpl) snapshot).metaData);
        } finally {
            unlockWrite();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), state.hashCode(), metaData.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        return Objects.equals(state, ((SnapshotImpl) obj).state) &&
               Objects.equals(metaData, ((SnapshotImpl) obj).metaData);
    }

    @Override
    public SnapshotImpl clone() {
        return new SnapshotImpl(this);
    }

    //region [THREAD-SAFE METADATA METHODS] ////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public Hash getInitialHash() {
        lockRead();

        try {
            return metaData.getInitialHash();
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public void setInitialHash(Hash initialHash) {
        lockWrite();

        try {
            metaData.setInitialHash(initialHash);
        } finally {
            unlockWrite();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public long getInitialTimestamp() {
        lockRead();

        try {
            return metaData.getInitialTimestamp();
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public void setInitialTimestamp(long initialTimestamp) {
        lockWrite();

        try {
            metaData.setInitialTimestamp(initialTimestamp);
        } finally {
            unlockWrite();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public Hash getHash() {
        lockRead();

        try {
            return this.metaData.getHash();
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public void setHash(Hash hash) {
        lockWrite();

        try {
            metaData.setHash(hash);
        } finally {
            unlockWrite();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public long getTimestamp() {
        lockRead();

        try {
            return metaData.getTimestamp();
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public void setTimestamp(long timestamp) {
        lockWrite();

        try {
            metaData.setTimestamp(timestamp);
        } finally {
            unlockWrite();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotMetaData} method.
     */
    @Override
    public void update(SnapshotMetaData newMetaData) {
        lockWrite();

        try {
            metaData.update(newMetaData);
        } finally {
            unlockWrite();
        }
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region [THREAD-SAFE STATE METHODS] ///////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotState} method.
     */
    @Override
    public Long getBalance(Hash address) {
        lockRead();

        try {
            return state.getBalance(address);
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotState} method.
     */
    @Override
    public Map<Hash, Long> getBalances() {
        lockRead();

        try {
            return state.getBalances();
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotState} method.
     */
    @Override
    public boolean isConsistent() {
        lockRead();

        try {
            return state.isConsistent();
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotState} method.
     */
    @Override
    public boolean hasCorrectSupply() {
        lockRead();

        try {
            return state.hasCorrectSupply();
        } finally {
            unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotState} method.
     */
    @Override
    public void update(SnapshotState newState) {
        lockWrite();

        try {
            state.update(newState);
        } finally {
            unlockWrite();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotState} method.
     */
    @Override
    public void applyStateDiff(SnapshotStateDiff diff) throws SnapshotException {
        lockWrite();

        try {
            state.applyStateDiff(diff);
        } finally {
            unlockWrite();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This is a thread-safe wrapper for the underlying {@link SnapshotState} method.
     */
    @Override
    public SnapshotState patchedState(SnapshotStateDiff snapshotStateDiff) {
        lockRead();

        try {
            return state.patchedState(snapshotStateDiff);
        } finally {
            unlockRead();
        }
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////
}
