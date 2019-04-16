package com.iota.iri;

import com.iota.iri.conf.IotaConfig;
import com.iota.iri.conf.TipSelConfig;
import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.network.Node;
import com.iota.iri.network.TransactionRequester;
import com.iota.iri.network.UDPReceiver;
import com.iota.iri.network.replicator.Replicator;
import com.iota.iri.service.TipsSolidifier;
import com.iota.iri.service.stats.LagCalculator;
import com.iota.iri.service.stats.TransactionStatsPublisher;
import com.iota.iri.service.DatabaseRecycler;
import com.iota.iri.service.tipselection.*;
import com.iota.iri.service.tipselection.impl.*;
import com.iota.iri.storage.*;
import com.iota.iri.storage.rocksDB.RocksDBPersistenceProvider;
import com.iota.iri.utils.Pair;
import com.iota.iri.utils.dag.RecentTransactionsGetter;
import com.iota.iri.utils.dag.impl.RecentTransactionsGetterImpl;
import com.iota.iri.zmq.MessageQ;

import java.security.SecureRandom;
import java.util.List;
import java.util.Date;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * The main class of CLIRI. This will propagate transactions into and throughout the network.
 * This data is stored as a {@link Tangle}, a form of a Directed acyclic graph.
 * All incoming data will be stored in one or more implementations of {@link PersistenceProvider}.
 * 
 * <p>
 *     During initialization, all the Providers can be set to rescan or revalidate their transactions.
 *     After initialization, an asynchronous process has started which will process inbound and outbound transactions.
 *     Each full node should be peered with 7-9 other full nodes (neighbors) to function optimally.
 * </p>
 * <p>
 *     If this node has no Neighbors defined, no data is transferred. 
 *     However, if the node has Neighbors, but no Internet connection, 
 *     synchronization will continue after Internet connection is established.
 *     Any transactions sent to this node in its local network will then be processed.
 *     This makes CLIRI able to run partially offline if an already existing database exists on this node.
 * </p>
 * <p>
 *     Validation of a transaction is the process by which other devices choose the transaction.
 *     This is done via a {@link TipSelector} algorithm, after which the transaction performs 
 *     the necessary proof-of-work in order to cast their vote of confirmation/approval upon those tips. <br/>
 *     
 *     As many other transactions repeat this process on top of each other, 
 *     validation of the transaction in question slowly builds up enough verifications.
 *     Eventually this will reach a minimum acceptable verification threshold.
 *     This threshold is determined by the recipient of the transaction. 
 *     When this minimum threshold is reached, the transaction is "confirmed".
 * </p>
 *
 */
public class Iota {
    private static final Logger log = LoggerFactory.getLogger(Iota.class);

    public final LedgerValidator ledgerValidator;
    public final Tangle tangle;
    public final TransactionValidator transactionValidator;
    public final TipsSolidifier tipsSolidifier;
    public final TransactionStatsPublisher transactionStatsPublisher;
    public final TransactionRequester transactionRequester;
    public final Node node;
    public final UDPReceiver udpReceiver;
    public final Replicator replicator;
    public final IotaConfig configuration;
    public final TipsViewModel tipsViewModel;
    public final MessageQ messageQ;
    public final TipSelector tipsSelector;
    public final DatabaseRecycler databaseRecycler;
    public final LagCalculator lagCalculator;

    public final int lagCalculatorTransactionCount = 100;

    /**
     * Creates all services needed to run an IOTA node.
     * 
     * @param configuration Information about how this node will be configured.
     */
    public Iota(IotaConfig configuration) {
        this.configuration = configuration;
        tangle = new Tangle();
        messageQ = MessageQ.createWith(configuration);
        tipsViewModel = new TipsViewModel(tangle);
        transactionRequester = new TransactionRequester(tangle, messageQ);
        transactionValidator = new TransactionValidator(tangle, tipsViewModel, transactionRequester);
        node = new Node(tangle, transactionValidator, transactionRequester, tipsViewModel, messageQ,
                configuration);
        replicator = new Replicator(node, configuration);
        udpReceiver = new UDPReceiver(node, configuration);
        ledgerValidator = new LedgerValidatorImpl();
        tipsSolidifier = new TipsSolidifier(tangle, transactionValidator, tipsViewModel);
        tipsSelector = createTipSelector(configuration);
        transactionStatsPublisher = new TransactionStatsPublisher(tangle, tipsViewModel, tipsSelector, messageQ);
        databaseRecycler = new DatabaseRecycler(transactionValidator, transactionRequester, tipsViewModel, tangle);
        RecentTransactionsGetter recentTransactionsGetter = new RecentTransactionsGetterImpl(tipsViewModel, tangle);
        lagCalculator = new LagCalculator(lagCalculatorTransactionCount, tangle, recentTransactionsGetter);
    }

    /**
     * Adds all database providers, and starts initialization of our services.
     * According to the {@link IotaConfig}, data is optionally cleared, reprocessed and reverified.<br/>
     * After this function, incoming and outbound transaction processing has started.
     * 
     * @throws Exception If along the way a service fails to initialize.
     *                   Most common cause is a file read or database error.
     */
    public void init() throws Exception {
        initializeTangle();
        tangle.init();

        if (configuration.isRescanDb()){
            rescanDb();
        }

        if (configuration.isZmqEnabled()) {
            transactionStatsPublisher.init();
        }
        transactionValidator.init(configuration.isTestnet(), configuration.getMwm());
        tipsSolidifier.init();
        transactionRequester.init(configuration.getpRemoveRequest());
        udpReceiver.init();
        replicator.init();
        node.init();
        databaseRecycler.init(new Date(System.currentTimeMillis()));
    }

    private void rescanDb() throws Exception {
        //delete all transaction indexes
        tangle.clearColumn(com.iota.iri.model.persistables.Address.class);
        tangle.clearColumn(com.iota.iri.model.persistables.Bundle.class);
        tangle.clearColumn(com.iota.iri.model.persistables.Approvee.class);
        tangle.clearColumn(com.iota.iri.model.persistables.ObsoleteTag.class);
        tangle.clearColumn(com.iota.iri.model.persistables.Tag.class);
        tangle.clearMetadata(com.iota.iri.model.persistables.Transaction.class);

        //rescan all tx & refill the columns
        TransactionViewModel tx = TransactionViewModel.first(tangle);
        int counter = 0;
        while (tx != null) {
            if (++counter % 10000 == 0) {
                log.info("Rescanned {} Transactions", counter);
            }
            List<Pair<Indexable, Persistable>> saveBatch = tx.getSaveBatch();
            saveBatch.remove(5);
            tangle.saveBatch(saveBatch);
            tx = tx.next(tangle);
        }
    }
    
    /**
     * Gracefully shuts down by calling <tt>shutdown()</tt> on all used services.
     * Exceptions during shutdown are not caught.
     */
    public void shutdown() throws Exception {
        transactionStatsPublisher.shutdown();
        tipsSolidifier.shutdown();
        node.shutdown();
        udpReceiver.shutdown();
        replicator.shutdown();
        transactionValidator.shutdown();
        tangle.shutdown();
        messageQ.shutdown();
    }

    private void initializeTangle() {
        switch (configuration.getMainDb()) {
            case "rocksdb": {
                tangle.addPersistenceProvider(new RocksDBPersistenceProvider(
                        configuration.getDbPath(),
                        configuration.getDbLogPath(),
                        configuration.getDbCacheSize()));
                break;
            }
            default: {
                throw new NotImplementedException("No such database type.");
            }
        }
        if (configuration.isZmqEnabled()) {
            tangle.addPersistenceProvider(new ZmqPublishProvider(messageQ));
        }
    }

    private TipSelector createTipSelector(TipSelConfig config) {
        RatingCalculator ratingCalculator = new CumulativeWeightCalculator(tangle);
        TailFinder tailFinder = new TailFinderImpl(tangle);
        Walker walker = new WalkerAlpha(tailFinder, tangle, messageQ, new SecureRandom(), config);
        RecentTransactionsGetter recentTransactionsGetter = new RecentTransactionsGetterImpl(tipsViewModel, tangle);
        StartingTipSelector startingTipSelector = new ConnectedComponentsStartingTipSelector(tangle, CumulativeWeightCalculator.MAX_FUTURE_SET_SIZE, recentTransactionsGetter);
        EntryPointSelector entryPointSelector = new EntryPointSelectorCumulativeWeightThreshold(
            tangle, CumulativeWeightCalculator.MAX_FUTURE_SET_SIZE, startingTipSelector, tailFinder);
        ReferenceChecker referenceChecker = new ReferenceCheckerImpl(tangle);
        return new TipSelectorImpl(tangle, ledgerValidator, entryPointSelector, ratingCalculator, walker, referenceChecker);
    }
}
