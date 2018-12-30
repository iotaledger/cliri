package com.iota.iri.service;

import static java.time.DayOfWeek.SUNDAY;
import static java.time.temporal.TemporalAdjusters.next;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.iota.iri.TransactionValidator;
import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.network.TransactionRequester;
import com.iota.iri.storage.Tangle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.util.Duration;

/**
 * This class deletes the database every Sunday at midnight, UTC. Its purpose is
 * to cap hard disk usage until local snapshots are merged into CLIRI.
 */
public class DatabaseRecycler {
    private final long acceptableLatencyInMs = Duration.buildByMinutes(10).getMilliseconds();

    private static final Logger log = LoggerFactory.getLogger(DatabaseRecycler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final TransactionValidator transactionValidator;
    private final TransactionRequester transactionRequester;
    private final TipsViewModel tipsViewModel;
    private final Tangle tangle;

    public DatabaseRecycler(TransactionValidator transactionValidator, TransactionRequester transactionRequester,
        TipsViewModel tipsViewModel, Tangle tangle) {
        this.transactionRequester = transactionRequester;
        this.transactionValidator = transactionValidator;
        this.tipsViewModel = tipsViewModel;
        this.tangle = tangle;
    }

    public void init(Date now) throws Exception {
        final long oneWeekInMs = Duration.buildByDays(7).getMilliseconds();
        final LocalDate nextSundayDate = now.toInstant().atZone(ZoneId.of("UTC")).toLocalDate().with(next(SUNDAY));
        final long nextSunday = Date.from(nextSundayDate.atStartOfDay(ZoneId.of("UTC")).toInstant()).getTime();

        log.info("Time until next Sunday: {}", nextSunday - now.getTime());
        log.info("Time between consecutive runs: {}", oneWeekInMs);

        scheduler.scheduleAtFixedRate(recycle, nextSunday - now.getTime(), oneWeekInMs, TimeUnit.MILLISECONDS);
    }

    protected final Runnable recycle = new Runnable() {
        public void run() {
            log.info("Recycling database at {}", new Date(System.currentTimeMillis()).toString());
            final long latestEpochTimestampInSeconds = (System.currentTimeMillis() - acceptableLatencyInMs) / 1000;
            TransactionValidator.setLatestEpochTimestamp(latestEpochTimestampInSeconds);
            transactionRequester.clearQueue();
            tipsViewModel.clear();

            try {
                transactionValidator.clearSolidTransactionsQueue();
                tangle.clearAll();
            } catch (Exception e) {
                log.error("Failed while recycling database", e);
            }
        }
    };
}
