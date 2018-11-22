package com.iota.iri.conf;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TestnetConfig extends BaseIotaConfig {

    protected int mwm = Defaults.MWM;
    protected int transactionPacketSize = Defaults.PACKET_SIZE;
    protected int requestHashSize = Defaults.REQUEST_HASH_SIZE;

    public TestnetConfig() {
        super();
        dbPath = Defaults.DB_PATH;
        dbLogPath = Defaults.DB_LOG_PATH;
    }

    @Override
    public boolean isTestnet() {
        return true;
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
        int REQUEST_HASH_SIZE = 49;
        int MWM = 9;
        int PACKET_SIZE = 1653;
        String DB_PATH = "testnetdb";
        String DB_LOG_PATH = "testnetdb.log";
    }
}
