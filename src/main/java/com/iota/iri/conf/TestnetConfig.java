package com.iota.iri.conf;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iota.iri.crypto.SpongeFactory;
import com.iota.iri.model.Hash;
import com.iota.iri.model.HashFactory;

import java.util.Objects;

public class TestnetConfig extends BaseIotaConfig {

    protected String snapshotFile = Defaults.SNAPSHOT_FILE;
    protected String snapshotSignatureFile = Defaults.SNAPSHOT_SIG;
    protected long snapshotTime = Defaults.SNAPSHOT_TIME;
    protected int mwm = Defaults.MWM;
    protected int transactionPacketSize = Defaults.PACKET_SIZE;
    protected int requestHashSize = Defaults.REQUEST_HASH_SIZE;

    public TestnetConfig() {
        super();
        dbPath = Defaults.DB_PATH;
        dbLogPath = Defaults.DB_LOG_PATH;
        localSnapshotsBasePath = Defaults.LOCAL_SNAPSHOTS_BASE_PATH;
    }

    @Override
    public boolean isTestnet() {
        return true;
    }

    @Override
    public String getSnapshotFile() {
        return snapshotFile;
    }

    @JsonProperty
    @Parameter(names = "--snapshot", description = SnapshotConfig.Descriptions.SNAPSHOT_FILE)
    protected void setSnapshotFile(String snapshotFile) {
        this.snapshotFile = snapshotFile;
    }

    @Override
    public String getSnapshotSignatureFile() {
        return snapshotSignatureFile;
    }

    @JsonProperty
    @Parameter(names = "--snapshot-sig", description = SnapshotConfig.Descriptions.SNAPSHOT_SIGNATURE_FILE)
    protected void setSnapshotSignatureFile(String snapshotSignatureFile) {
        this.snapshotSignatureFile = snapshotSignatureFile;
    }

    @Override
    public long getSnapshotTime() {
        return snapshotTime;
    }

    @JsonProperty
    @Parameter(names = "--snapshot-timestamp", description = SnapshotConfig.Descriptions.SNAPSHOT_TIME)
    protected void setSnapshotTime(long snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    @Override
    public int getMwm() {
        return mwm;
    }

    @JsonProperty
    @Parameter(names = "--mwm", description = ProtocolConfig.Descriptions.MWM)
    protected void setMwm(int mwm) {
        this.mwm = mwm;
    }

    @Override
    public int getTransactionPacketSize() {
        return transactionPacketSize;
    }

    @JsonProperty
    @Parameter(names = {"--packet-size"}, description = ProtocolConfig.Descriptions.TRANSACTION_PACKET_SIZE)
    protected void setTransactionPacketSize(int transactionPacketSize) {
        this.transactionPacketSize = transactionPacketSize;
    }

    @Override
    public int getRequestHashSize() {
        return requestHashSize;
    }

    @JsonProperty
    @Parameter(names = {"--request-hash-size"}, description = ProtocolConfig.Descriptions.REQUEST_HASH_SIZE)
    public void setRequestHashSize(int requestHashSize) {
        this.requestHashSize = requestHashSize;
    }

    @JsonProperty
    @Override
    public void setDbPath(String dbPath) {
        if (Objects.equals(MainnetConfig.Defaults.DB_PATH, dbPath)) {
            throw new ParameterException("Testnet Db folder cannot be configured to mainnet's db folder");
        }
            super.setDbPath(dbPath);
    }

    @JsonProperty
    @Override
    public void setDbLogPath(String dbLogPath) {
        if (Objects.equals(MainnetConfig.Defaults.DB_LOG_PATH, dbLogPath)) {
            throw new ParameterException("Testnet Db log folder cannot be configured to mainnet's db log folder");
        }
            super.setDbLogPath(dbLogPath);
    }

    public interface Defaults {
        String LOCAL_SNAPSHOTS_BASE_PATH = "testnet";
        String SNAPSHOT_FILE = "/snapshotTestnet.txt";
        int REQUEST_HASH_SIZE = 49;
        String SNAPSHOT_SIG = "/snapshotTestnet.sig";
        int SNAPSHOT_TIME = 1522306500;
        int MWM = 9;
        int PACKET_SIZE = 1653;
        String DB_PATH = "testnetdb";
        String DB_LOG_PATH = "testnetdb.log";

    }
}
