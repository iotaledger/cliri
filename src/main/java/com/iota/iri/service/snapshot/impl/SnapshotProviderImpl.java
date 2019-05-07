package com.iota.iri.service.snapshot.impl;

import com.iota.iri.SignedFiles;
import com.iota.iri.conf.SnapshotConfig;
import com.iota.iri.model.Hash;
import com.iota.iri.model.HashFactory;
import com.iota.iri.service.snapshot.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a data provider for the two {@link Snapshot} instances that are relevant for the node.<br />
 * <br />
 * It provides access to the two relevant {@link Snapshot} instances:<br />
 * <ul>
 *     <li>
 *         the {@link #initialSnapshot} (the starting point of the ledger based on the last global or local Snapshot)
 *     </li>
 *     <li>
 *         the {@link #latestSnapshot} (the state of the ledger after applying all changes up till the latest confirmed
 *         milestone)
 *     </li>
 * </ul>
 */
public class SnapshotProviderImpl implements SnapshotProvider {
    /**
     * Public key that is used to verify the builtin snapshot signature.
     */
    private static final String SNAPSHOT_PUBKEY =
            "TTXJUGKTNPOOEXSTQVVACENJOQUROXYKDRCVK9LHUXILCLABLGJTIPNF9REWHOIMEUKWQLUOKD9CZUYAC";

    /**
     * Public key depth that is used to verify the builtin snapshot signature.
     */
    private static final int SNAPSHOT_PUBKEY_DEPTH = 6;

    /**
     * Snapshot index that is used to verify the builtin snapshot signature.
     */
    private static final int SNAPSHOT_INDEX = 12;

    /**
     * Holds a cached version of the builtin snapshot.
     *
     * Note: The builtin snapshot is embedded in the iri.jar and will not change. To speed up tests that need the
     *       snapshot multiple times while creating their own version of the LocalSnapshotManager, we cache the instance
     *       here so they don't have to rebuild it from the scratch every time (massively speeds up the unit tests).
     */
    private static SnapshotImpl builtinSnapshot = null;

    /**
     * Holds Snapshot related configuration parameters.
     */
    private SnapshotConfig config;

    /**
     * Internal property for the value returned by {@link SnapshotProvider#getInitialSnapshot()}.
     */
    private Snapshot initialSnapshot;

    /**
     * Internal property for the value returned by {@link SnapshotProvider#getLatestSnapshot()}.
     */
    private Snapshot latestSnapshot;

    /**
     * This method initializes the instance and registers its dependencies.<br />
     * <br />
     * It simply stores the passed in values in their corresponding private properties and loads the snapshots.<br />
     * <br />
     * Note: Instead of handing over the dependencies in the constructor, we register them lazy. This allows us to have
     *       circular dependencies because the instantiation is separated from the dependency injection. To reduce the
     *       amount of code that is necessary to correctly instantiate this class, we return the instance itself which
     *       allows us to still instantiate, initialize and assign in one line - see Example:<br />
     *       <br />
     *       {@code snapshotProvider = new SnapshotProviderImpl().init(...);}
     *
     * @param config Snapshot related configuration parameters
     * @throws SnapshotException if anything goes wrong while trying to read the snapshots
     * @return the initialized instance itself to allow chaining
     *
     */
    public SnapshotProviderImpl init(SnapshotConfig config) throws SnapshotException {
        this.config = config;

        loadSnapshots();

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Snapshot getInitialSnapshot() {
        return initialSnapshot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Snapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    /**
     * {@inheritDoc}<br />
     * <br />
     * It first writes two temporary files, then renames the current files by appending them with a ".bkp" extension and
     * finally renames the temporary files. This mechanism reduces the chances of the files getting corrupted if IRI
     * crashes during the snapshot creation and always leaves the node operator with a set of backup files that can be
     * renamed to resume node operation prior to the failed snapshot.<br />
     * <br />
     * Note: We create the temporary files in the same folder as the "real" files to allow the operating system to
     *       perform a "rename" instead of a "copy" operation.<br />
     */
    @Override
    public void writeSnapshotToDisk(Snapshot snapshot, String basePath) throws SnapshotException {
        snapshot.lockRead();

        try {
            // write new temp files
            writeSnapshotStateToDisk(snapshot, basePath + ".snapshot.state.tmp");
            writeSnapshotMetaDataToDisk(snapshot, basePath + ".snapshot.meta.tmp");

            // rename current files by appending ".bkp"
            if (new File(basePath + ".snapshot.state").exists()) {
                Files.move(Paths.get(basePath + ".snapshot.state"), Paths.get(basePath + ".snapshot.state.bkp"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            if (new File(basePath + ".snapshot.meta").exists()) {
                Files.move(Paths.get(basePath + ".snapshot.meta"), Paths.get(basePath + ".snapshot.meta.bkp"),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            // rename temp files to their final name
            Files.move(Paths.get(basePath + ".snapshot.state.tmp"), Paths.get(basePath + ".snapshot.state"));
            Files.move(Paths.get(basePath + ".snapshot.meta.tmp"), Paths.get(basePath + ".snapshot.meta"));
        } catch (IOException e) {
            throw new SnapshotException("failed to write snapshot files", e);
        } finally {
            snapshot.unlockRead();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        initialSnapshot = null;
        latestSnapshot = null;
    }

    //region SNAPSHOT RELATED UTILITY METHODS //////////////////////////////////////////////////////////////////////////

    /**
     * Loads the snapshots that are provided by this data provider.
     *
     * We first check if a valid local {@link Snapshot} exists by trying to load it. If we fail to load the local
     * {@link Snapshot}, we fall back to the builtin one.
     *
     * After the {@link #initialSnapshot} was successfully loaded we create a copy of it that will act as the "working
     * copy" that will keep track of the latest changes that get applied while the node operates and processes the new
     * confirmed transactions.
     *
     * @throws SnapshotException if anything goes wrong while loading the snapshots
     */
    private void loadSnapshots() throws SnapshotException {
        initialSnapshot = loadLocalSnapshot();
        if (initialSnapshot == null) {
            initialSnapshot = loadBuiltInSnapshot();
        }

        latestSnapshot = initialSnapshot.clone();
    }

    /**
     * Loads the last local snapshot from the disk.
     *
     * This method checks if local snapshot files are available on the hard disk of the node and tries to load them. If
     * no local snapshot files exist or local snapshots are not enabled we simply return null.
     *
     * @return local snapshot of the node
     * @throws SnapshotException if local snapshot files exist but are malformed
     */
    private Snapshot loadLocalSnapshot() throws SnapshotException {
        return null;
    }

    /**
     * Loads the builtin snapshot (last global snapshot) that is embedded in the jar (if a different path is provided it
     * can also load from the disk).
     *
     * We first verify the integrity of the snapshot files by checking the signature of the files and then construct
     * a {@link Snapshot} from the retrieved information.
     *
     * We add the NULL_HASH as the only solid entry point and an empty list of seen milestones.
     *
     * @return the builtin snapshot (last global snapshot) that is embedded in the jar
     * @throws SnapshotException if anything goes wrong while loading the builtin {@link Snapshot}
     */
    private Snapshot loadBuiltInSnapshot() throws SnapshotException {
        if (builtinSnapshot == null) {
            try {
                if (!config.isTestnet() && !SignedFiles.isFileSignatureValid(
                        config.getSnapshotFile(),
                        config.getSnapshotSignatureFile(),
                        SNAPSHOT_PUBKEY,
                        SNAPSHOT_PUBKEY_DEPTH,
                        SNAPSHOT_INDEX
                )) {
                    throw new SnapshotException("the snapshot signature is invalid");
                }
            } catch (IOException e) {
                throw new SnapshotException("failed to validate the signature of the builtin snapshot file", e);
            }

            SnapshotState snapshotState;
            try {
                snapshotState = readSnapshotStateFromJAR(config.getSnapshotFile());
            } catch (SnapshotException e) {
                snapshotState = readSnapshotStatefromFile(config.getSnapshotFile());
            }
            if (!snapshotState.hasCorrectSupply()) {
                throw new SnapshotException("the snapshot state file has an invalid supply");
            }
            if (!snapshotState.isConsistent()) {
                throw new SnapshotException("the snapshot state file is not consistent");
            }

            builtinSnapshot = new SnapshotImpl(
                    snapshotState,
                    new SnapshotMetaDataImpl(
                            Hash.NULL_HASH,
                            config.getSnapshotTime()
                    )
            );
        }

        return builtinSnapshot.clone();
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region SNAPSHOT STATE RELATED UTILITY METHODS ////////////////////////////////////////////////////////////////////

    /**
     * This method reads the balances from the given file on the disk and creates the corresponding SnapshotState.
     *
     * It simply creates the corresponding reader and for the file on the given location and passes it on to
     * {@link #readSnapshotState(BufferedReader)}.
     *
     * @param snapshotStateFilePath location of the snapshot state file
     * @return the unserialized version of the state file
     * @throws SnapshotException if anything goes wrong while reading the state file
     */
    private SnapshotState readSnapshotStatefromFile(String snapshotStateFilePath) throws SnapshotException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(snapshotStateFilePath))))) {
            return readSnapshotState(reader);
        } catch (IOException e) {
            throw new SnapshotException("failed to read the snapshot file at " + snapshotStateFilePath, e);
        }
    }

    /**
     * This method reads the balances from the given file in the JAR and creates the corresponding SnapshotState.
     *
     * It simply creates the corresponding reader and for the file on the given location in the JAR and passes it on to
     * {@link #readSnapshotState(BufferedReader)}.
     *
     * @param snapshotStateFilePath location of the snapshot state file
     * @return the unserialized version of the state file
     * @throws SnapshotException if anything goes wrong while reading the state file
     */
    private SnapshotState readSnapshotStateFromJAR(String snapshotStateFilePath) throws SnapshotException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(SnapshotProviderImpl.class.getResourceAsStream(snapshotStateFilePath))))) {
            return readSnapshotState(reader);
        } catch (IOException e) {
            throw new SnapshotException("failed to read the snapshot file from JAR at " + snapshotStateFilePath, e);
        }
    }
    /**
     * This method reads the balances from the given reader.
     *
     * The format of the input is pairs of "address;balance" separated by newlines. It simply reads the input line by
     * line, adding the corresponding values to the map.
     *
     * @param reader reader allowing us to retrieve the lines of the {@link SnapshotState} file
     * @return the unserialized version of the snapshot state state file
     * @throws IOException if something went wrong while trying to access the file
     * @throws SnapshotException if anything goes wrong while reading the state file
     */
    private SnapshotState readSnapshotState(BufferedReader reader) throws IOException, SnapshotException {
        Map<Hash, Long> state = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(";", 2);
            if (parts.length == 2) {
                state.put(HashFactory.ADDRESS.create(parts[0]), Long.valueOf(parts[1]));
            } else {
                throw new SnapshotException("malformed snapshot state file");
            }
        }

        return new SnapshotStateImpl(state);
    }

    /**
     * This method dumps the current state to a file.
     *
     * It is used by local snapshots to persist the in memory states and allow IRI to resume from the local snapshot.
     *
     * @param snapshotState state object that shall be written
     * @param snapshotPath location of the file that shall be written
     * @throws SnapshotException if anything goes wrong while writing the file
     */
    private void writeSnapshotStateToDisk(SnapshotState snapshotState, String snapshotPath) throws SnapshotException {
        try {
            Files.write(
                    Paths.get(snapshotPath),
                    () -> snapshotState.getBalances().entrySet()
                            .stream()
                            .filter(entry -> entry.getValue() != 0)
                            .<CharSequence>map(entry -> entry.getKey() + ";" + entry.getValue())
                            .sorted()
                            .iterator()
            );
        } catch (IOException e) {
            throw new SnapshotException("failed to write the snapshot state file at " + snapshotPath, e);
        }
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////

    //region SNAPSHOT METADATA RELATED UTILITY METHODS /////////////////////////////////////////////////////////////////

    /**
     * This method writes a file containing a serialized version of the metadata
     * object.
     *
     * It can be used to store the current values and read them on a later point in
     * time. It is used by the local snapshot manager to generate and maintain the
     * snapshot files.
     *
     * @param snapshotMetaData metadata object that shall be written
     * @param filePath         location of the file that shall be written
     * @throws SnapshotException if anything goes wrong while writing the file
     */
    private void writeSnapshotMetaDataToDisk(SnapshotMetaData snapshotMetaData, String filePath)
            throws SnapshotException {

        try {
            Files.write(
                    Paths.get(filePath),
                    (snapshotMetaData.getHash().toString() +
                        String.valueOf(snapshotMetaData.getTimestamp())).getBytes()
            );
        } catch (IOException e) {
            throw new SnapshotException("failed to write snapshot metadata file at " + filePath, e);
        }
    }

    //endregion ////////////////////////////////////////////////////////////////////////////////////////////////////////
}
