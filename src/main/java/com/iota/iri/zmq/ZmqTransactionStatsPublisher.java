package com.iota.iri.zmq;

import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.service.tipselection.TipSelector;
import com.iota.iri.storage.Tangle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publishes the number of total transactions and confirmed transactions to the ZeroMQ.
 *
 * Only transactions are counted that have an arrival time between 5 minutes and 2 hours in the past.
 * 
 * For the confirmed transactions, the normal tip selection is performed to determine a supertip. The number of
 * transactions in its past set is then published to ZMQ.
 */
public class ZmqTransactionStatsPublisher {

    private static final long PUBLISH_INTERVAL = Duration.ofMinutes(1).toMillis();

    private static final String CONFIRMED_TRANSACTIONS_TOPIC = "ct5m2h";
    private static final String TOTAL_TRANSACTIONS_TOPIC = "t5m2h";

    private static final Duration MIN_TRANSACTION_AGE_THRESHOLD = Duration.ofMinutes(5);
    private static final Duration MAX_TRANSACTION_AGE_THRESHOLD = Duration.ofHours(2);

    private final Logger log = LoggerFactory.getLogger(ZmqTransactionStatsPublisher.class);

    private final Tangle tangle;
    private final TipsViewModel tipsViewModel;
    private final TipSelector tipsSelector;
    private final TimeWindowedApproveeCounter approveeCounter;

    private final MessageQ messageQ;

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private Thread thread;

    public ZmqTransactionStatsPublisher(Tangle tangle, TipsViewModel tipsViewModel, TipSelector tipsSelector,
            MessageQ messageQ) {

        this.tangle = tangle;
        this.tipsViewModel = tipsViewModel;
        this.tipsSelector = tipsSelector;
        this.approveeCounter = new TimeWindowedApproveeCounter(tangle, MIN_TRANSACTION_AGE_THRESHOLD,
                MAX_TRANSACTION_AGE_THRESHOLD);
        this.messageQ = messageQ;
    }

    /**
     * Starts the publisher.
     */
    public void init() {
        thread = new Thread(getRunnable(), "Transaction Stats Publisher");
        thread.start();
    }

    private Runnable getRunnable() {
        return () -> {
            while (!shuttingDown.get()) {
                try {
                    final Instant now = Instant.now();

                    final long numConfirmed = getConfirmedTransactionsCount(now);
                    final long numTransactions = getAllTransactionsCount(now);

                    messageQ.publish(CONFIRMED_TRANSACTIONS_TOPIC + " %d", numConfirmed);
                    messageQ.publish(TOTAL_TRANSACTIONS_TOPIC + " %d", numTransactions);
                } catch (Exception e) {
                    log.error("Error while getting transaction counts : {}", e);
                }
                try {
                    Thread.sleep(PUBLISH_INTERVAL);
                } catch (InterruptedException e) {
                    log.error("Transaction count interrupted.");
                }
            }
        };
    }

    private Hash getSuperTip() throws Exception {

        // call the usual tip selection and return the first tip
        List<Hash> tips = tipsSelector.getTransactionsToApprove(Optional.empty());

        return tips.get(0);
    }

    private long getConfirmedTransactionsCount(Instant now) throws Exception {

        return approveeCounter.getCount(now, getSuperTip(), new HashSet<>());
    }

    private long getAllTransactionsCount(Instant now) throws Exception {

        // count all transactions in a scalable way, by counting the approvees of all the tips
        HashSet<Hash> processedTransactions = new HashSet<>();
        long count = 0;
        for (Hash tip : tipsViewModel.getTips()) {
            // count the tip, if it is the valid time window
            if (approveeCounter.isInTimeWindow(now, TransactionViewModel.fromHash(tangle, tip))) {
                count += 1 + approveeCounter.getCount(now, tip, processedTransactions);
            } else {
                // even if the tip is not in the time window, count approvees that might be older
                count += approveeCounter.getCount(now, tip, processedTransactions);
            }
        }

        return count;
    }

    /**
     * Stops the publisher.
     */
    public void shutdown() {
        shuttingDown.set(true);
        try {
            if (thread != null && thread.isAlive()) {
                thread.join();
            }
        } catch (Exception e) {
            log.error("Error in shutdown", e);
        }
    }
}
