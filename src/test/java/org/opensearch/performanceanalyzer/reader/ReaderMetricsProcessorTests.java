/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor.BATCH_METRICS_ENABLED_CONF_FILE;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.config.PluginSettings;
import org.opensearch.performanceanalyzer.core.Util;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.ClusterManagerPendingTaskDimension;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.ClusterManagerPendingValue;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.MetricName;
import org.opensearch.performanceanalyzer.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;

public class ReaderMetricsProcessorTests extends AbstractReaderTests {
    public String rootLocation;

    public ReaderMetricsProcessorTests() throws SQLException, ClassNotFoundException {
        super();
    }

    @Before
    public void before() throws Exception {
        rootLocation = "build/resources/test/reader/";
    }

    @Test
    public void testShardBulkParseAndQuery() throws Exception {
        deleteAll();
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());

        mp.processMetrics(rootLocation, 1566413975000L);
        mp.processMetrics(rootLocation, 1566413980000L);
        mp.processMetrics(rootLocation, 1566413985000L);
        mp.processMetrics(rootLocation, 1566413990000L);

        Result<Record> res =
                mp.getMetricsDB()
                        .getValue()
                        .queryMetric(
                                Arrays.asList("ShardEvents", "ShardBulkDocs", "CPU_Utilization"),
                                Arrays.asList("sum", "sum", "sum"),
                                Arrays.asList("Operation"));

        boolean shardbulkEncountered = false;

        for (Record record : res) {
            if (PerformanceAnalyzerMetrics.sShardBulkPath.equals(record.get("Operation"))) {
                assertNotNull(record.get("ShardEvents"));
                assertNotNull(record.get("ShardBulkDocs"));
                assertEquals(1519.0, (Double) record.get("ShardEvents"), 0.0);
                assertEquals(507096.0, (Double) record.get("ShardBulkDocs"), 0.0);
                assertEquals(1.89, (Double) record.get("CPU_Utilization"), 0.02);
                shardbulkEncountered = true;
            }
        }

        assertTrue(shardbulkEncountered);

        mp.trimOldSnapshots();
        mp.trimOldMetricsDBFiles();
        mp.deleteDBs();
    }

    @Test
    public void testReaderMetricsProcessorFrequently() throws Exception {
        deleteAll();
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());

        mp.processMetrics(rootLocation, 1566413975000L);
        mp.processMetrics(rootLocation, 1566413980000L);

        Result<Record> res =
                mp.getMetricsDB()
                        .getValue()
                        .queryMetric(
                                Arrays.asList("Cache_FieldData_Size"),
                                Arrays.asList("sum"),
                                Arrays.asList("ShardID", "IndexName"));

        for (Record record : res) {
            assertEquals(record.get("IndexName"), "nyc_taxis");
        }

        mp.trimOldSnapshots();
        mp.trimOldMetricsDBFiles();
        mp.deleteDBs();
    }

    @Test
    public void testReaderMetricsProcessorFrequentlyWithDelay() throws Exception {
        deleteAll();
        int delay = 2000;
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);

        mp.processMetrics(rootLocation, 1566413975000L + delay);
        mp.processMetrics(rootLocation, 1566413980000L + delay);

        Result<Record> res =
                mp.getMetricsDB()
                        .getValue()
                        .queryMetric(
                                Arrays.asList("Cache_FieldData_Size"),
                                Arrays.asList("sum"),
                                Arrays.asList("ShardID", "IndexName"));

        for (Record record : res) {
            assertEquals(record.get("IndexName"), "nyc_taxis");
        }

        mp.trimOldSnapshots();
        mp.trimOldMetricsDBFiles();
        mp.deleteDBs();
    }

    public void deleteAll() {
        final File folder = new File("/tmp");
        final File[] files =
                folder.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(final File dir, final String name) {
                                return name.matches("metricsdb_.*");
                            }
                        });
        for (final File file : files) {
            if (!file.delete()) {
                System.err.println("Can't remove " + file.getAbsolutePath());
            }
        }
    }

    private NavigableMap<Long, MemoryDBSnapshot> setUpAligningWindow(long lastUpdateTime3)
            throws Exception {
        // time line
        // writer writes to the left window at 2000l
        // reader reads at 6001l
        // writer writes to the right window at 7000l
        // reader reads at 11001l
        // writer writes to the right window at 12000l
        // reader reads at 16001l
        MemoryDBSnapshot clusterManagerPendingSnap1 =
                new MemoryDBSnapshot(conn, MetricName.CLUSTER_MANAGER_PENDING, 6001L);
        long lastUpdateTime1 = 2000L;
        clusterManagerPendingSnap1.setLastUpdatedTime(lastUpdateTime1);
        Object[][] values1 = {{"delete-index", 0}};
        clusterManagerPendingSnap1.insertMultiRows(values1);

        MemoryDBSnapshot clusterManagerPendingSnap2 =
                new MemoryDBSnapshot(conn, MetricName.CLUSTER_MANAGER_PENDING, 11001L);
        long lastUpdateTime2 = 7000L;
        clusterManagerPendingSnap2.setLastUpdatedTime(lastUpdateTime2);
        Object[][] values2 = {{"create-index", 1}};
        clusterManagerPendingSnap2.insertMultiRows(values2);

        MemoryDBSnapshot clusterManagerPendingSnap3 =
                new MemoryDBSnapshot(conn, MetricName.CLUSTER_MANAGER_PENDING, 16001L);
        clusterManagerPendingSnap2.setLastUpdatedTime(lastUpdateTime3);
        Object[][] values3 = {{"updateSnapshot", 3}};
        clusterManagerPendingSnap3.insertMultiRows(values3);

        NavigableMap<Long, MemoryDBSnapshot> metricMap = new TreeMap<>();
        metricMap.put(lastUpdateTime1, clusterManagerPendingSnap1);
        metricMap.put(lastUpdateTime2, clusterManagerPendingSnap2);
        metricMap.put(lastUpdateTime3, clusterManagerPendingSnap3);

        return metricMap;
    }

    private NavigableMap<Long, MemoryDBSnapshot> setUpAligningWindow() throws Exception {
        return setUpAligningWindow(12000L);
    }

    /**
     * Time line + writer writes 0 to the left window at 2000l + reader reads at 6001l + writer
     * writes 1 to the right window at 7000l + reader reads at 11001l + writer writes 3 to the right
     * window at 12000l + reader reads at 16001l
     *
     * <p>Given metrics in two writer windows calculates a new reader window which overlaps with the
     * given windows. |------leftWindow-------|-------rightWindow--------| 7000 5000 100000
     * |-----------alignedWindow———|
     *
     * <p>We retrieve left and right window using a metric map, whose key is the largest last
     * modification time. leftWindow = metricsMap.get(7000) = 1 rightWindow = metricsMap.get(12000)
     * = 3
     *
     * <p>We use values in the future to represent values in the past. So if at t1, writer writes
     * values 1, the interval [t1-sample interval, t1] has value 1. So [2000, 7000] maps to 1, and
     * [7000, 12000] maps to 3. We end up having (1 * 2000 + 3 * 3000) / 5000 = 2.2
     *
     * @throws Exception If something went wrong.
     */
    @Test
    public void testAlignNodeMetrics() throws Exception {
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        long readerTime1 = 6001L;
        long readerTime2 = 11001L;
        NavigableMap<Long, MemoryDBSnapshot> metricMap = setUpAligningWindow();
        // The 3rd parameter is windowEndTime.
        // So we compute algined metrics based previous reader window [6000L,
        // 11000l]. But we use PerformanceAnalyzerMetrics.getTimeInterval to
        // compute the aligned reader window time: 10000.
        // So our aligned window time is [5000,10000].
        MemoryDBSnapshot clusterManagerPendingFinal =
                new MemoryDBSnapshot(
                        conn,
                        MetricName.CLUSTER_MANAGER_PENDING,
                        PerformanceAnalyzerMetrics.getTimeInterval(
                                readerTime2, MetricsConfiguration.SAMPLING_INTERVAL),
                        true);

        MemoryDBSnapshot alignedWindow =
                mp.alignNodeMetrics(
                        MetricName.CLUSTER_MANAGER_PENDING,
                        metricMap,
                        PerformanceAnalyzerMetrics.getTimeInterval(
                                readerTime1, MetricsConfiguration.SAMPLING_INTERVAL),
                        PerformanceAnalyzerMetrics.getTimeInterval(
                                readerTime2, MetricsConfiguration.SAMPLING_INTERVAL),
                        clusterManagerPendingFinal);

        Result<Record> res = alignedWindow.fetchAll();
        assertTrue(2 == res.size());
        Field<Double> valueField =
                DSL.field(
                        ClusterManagerPendingValue.CLUSTER_MANAGER_PENDING_QUEUE_SIZE.toString(),
                        Double.class);
        Field<String> dimensionField =
                DSL.field(
                        ClusterManagerPendingTaskDimension.CLUSTER_MANAGER_PENDING_TASK_TYPE
                                .toString(),
                        String.class);
        Double pending = Double.parseDouble(res.get(0).get(valueField).toString());
        assertEquals(1.0d, pending, 0.001);
        assertEquals("create-index", res.get(0).get(dimensionField));

        pending = Double.parseDouble(res.get(1).get(valueField).toString());
        assertEquals(3.0d, pending, 0.001);
        assertEquals("updateSnapshot", res.get(1).get(dimensionField));
    }

    @Test
    public void testEmitNodeMetrics() throws Exception {
        // the Connection that the test uses and ReaderMetricsProcessor uses are
        // different.
        // Need to use the same one otherwise table created in the test won't be
        // visible in ReaderMetricsProcessor.
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor spyMp = Mockito.spy(mp);
        Mockito.doReturn(this.conn).when(spyMp).getConnection();

        long readerTime2 = 11001L;
        NavigableMap<Long, MemoryDBSnapshot> metricMap = setUpAligningWindow();

        spyMp.putNodeMetricsMap(MetricName.CLUSTER_MANAGER_PENDING, metricMap);

        MetricsDB db = new MetricsDB(1553713512);
        spyMp.emitNodeMetrics(
                PerformanceAnalyzerMetrics.getTimeInterval(
                        readerTime2, MetricsConfiguration.SAMPLING_INTERVAL),
                db);

        Result<Record> res =
                db.queryMetric(
                        ClusterManagerPendingValue.CLUSTER_MANAGER_PENDING_QUEUE_SIZE.toString());

        assertTrue(2 == res.size());

        Record row0 = res.get(0);
        for (int i = 1; i < row0.size(); i++) {
            Double pending = Double.parseDouble(row0.get(i).toString());
            assertEquals(1.0d, pending, 0.001);
        }
        assertEquals("create-index", row0.get(0).toString());

        Record row1 = res.get(1);
        for (int i = 1; i < row1.size(); i++) {
            Double pending = Double.parseDouble(row1.get(i).toString());
            assertEquals(3.0d, pending, 0.001);
        }
        assertEquals("updateSnapshot", row1.get(0).toString());

        db.remove();
    }

    /**
     * Reader window is: 10000~15000 Writer hasn't write to 17000 yet. Writer only has written at:
     * 2001, 7001, 12001 Since the reader needs two windows to align: [7001 ~ 12001] and [12001 ~
     * 17001] and the window [12001 ~ 17001] does not exist, reader would skip aligning and use the
     * value of [7001 ~ 12001] instead.
     *
     * @throws Exception if something went wrong.
     */
    @Test
    public void testMissingUpperWriterWindow() throws Exception {
        // the Connection that the test uses and ReaderMetricsProcessor uses are
        // different.
        // Need to use the same one otherwise table created in the test won't be
        // visible in ReaderMetricsProcessor.
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor spyMp = Mockito.spy(mp);
        Mockito.doReturn(this.conn).when(spyMp).getConnection();

        long readerTime2 = 16001L;
        NavigableMap<Long, MemoryDBSnapshot> metricMap = setUpAligningWindow();

        spyMp.putNodeMetricsMap(MetricName.CLUSTER_MANAGER_PENDING, metricMap);

        MetricsDB db = new MetricsDB(1553713518);
        spyMp.emitNodeMetrics(
                PerformanceAnalyzerMetrics.getTimeInterval(
                        readerTime2, MetricsConfiguration.SAMPLING_INTERVAL),
                db);

        Result<Record> res =
                db.queryMetric(
                        ClusterManagerPendingValue.CLUSTER_MANAGER_PENDING_QUEUE_SIZE.toString());

        assertTrue(1 == res.size());

        Record row0 = res.get(0);
        for (int i = 1; i < row0.size(); i++) {
            Double pending = Double.parseDouble(row0.get(i).toString());
            assertEquals(3.0, pending, 0.001);
        }
        assertEquals("updateSnapshot", row0.get(0));

        // db tables should not be deleted
        for (MemoryDBSnapshot value : metricMap.values()) {
            assertTrue(value.dbTableExists());
        }
        db.remove();
    }

    /**
     * Make sure we return null in alignNodeMetrics when right window {} snapshot ends at or before
     * endTime. This is possible because writer writes in less than 5 seconds (writer does not
     * guarantee write every 5 seconds). Reader does not expect that. Changed to return null in this
     * case.
     *
     * @throws Exception if something went wrong.
     */
    @Test
    public void testWriterWindowEndsBeforeReaderWindow() throws Exception {
        // the Connection that the test uses and ReaderMetricsProcessor uses are
        // different.
        // Need to use the same one otherwise table created in the test won't be
        // visible in ReaderMetricsProcessor.
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor spyMp = Mockito.spy(mp);
        Mockito.doReturn(this.conn).when(spyMp).getConnection();

        long readerTime2 = 11001L;
        NavigableMap<Long, MemoryDBSnapshot> metricMap = setUpAligningWindow(9999L);

        spyMp.putNodeMetricsMap(MetricName.CLUSTER_MANAGER_PENDING, metricMap);

        MetricsDB db = new MetricsDB(1553713524);
        spyMp.emitNodeMetrics(
                PerformanceAnalyzerMetrics.getTimeInterval(
                        readerTime2, MetricsConfiguration.SAMPLING_INTERVAL),
                db);

        assertTrue(
                !db.metricExists(
                        ClusterManagerPendingValue.CLUSTER_MANAGER_PENDING_QUEUE_SIZE.toString()));
        db.remove();
    }

    @Test
    public void testReadBatchMetricsEnabledFromConf() throws Exception {
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Path batchMetricsEnabledConfFile =
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE);
        Files.deleteIfExists(batchMetricsEnabledConfFile);
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);

        // Test default
        assertTrue(
                mp.getBatchMetricsEnabled() == ReaderMetricsProcessor.defaultBatchMetricsEnabled);

        // Test disabled
        Files.write(batchMetricsEnabledConfFile, Boolean.toString(false).getBytes());
        mp.readBatchMetricsEnabledFromConfShim();
        assertFalse(mp.getBatchMetricsEnabled());

        // Test reverts back to default when file is deleted
        Files.delete(batchMetricsEnabledConfFile);
        mp.readBatchMetricsEnabledFromConfShim();
        assertTrue(
                mp.getBatchMetricsEnabled() == ReaderMetricsProcessor.defaultBatchMetricsEnabled);

        // Test enabled
        Files.write(batchMetricsEnabledConfFile, Boolean.toString(true).getBytes());
        mp.readBatchMetricsEnabledFromConfShim();
        assertTrue(mp.getBatchMetricsEnabled());

        // Test reverts back to default when file is deleted
        Files.delete(batchMetricsEnabledConfFile);
        mp.readBatchMetricsEnabledFromConfShim();
        assertTrue(
                mp.getBatchMetricsEnabled() == ReaderMetricsProcessor.defaultBatchMetricsEnabled);

        assertTrue(RcaTestHelper.verify(ReaderMetrics.BATCH_METRICS_CONFIG_ERROR));
    }

    @Test
    public void testGetBatchMetrics() throws Exception {
        deleteAll();
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Path batchMetricsEnabledConfFile =
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE);
        Files.deleteIfExists(batchMetricsEnabledConfFile);
        long currentTimestamp = System.currentTimeMillis();
        ReaderMetricsProcessor mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);
        PluginSettings.instance().setBatchMetricsRetentionPeriodMinutes(1);
        PluginSettings.instance().setShouldCleanupMetricsDBFiles(true);

        // Test with batch metrics disabled
        Files.write(batchMetricsEnabledConfFile, Boolean.toString(false).getBytes());
        mp.processMetrics(rootLocation, currentTimestamp);
        mp.trimOldSnapshots();
        mp.trimOldMetricsDBFiles();
        currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
        assertNull(mp.getBatchMetrics());

        boolean secondRun = false;
        do {
            // Test with batch metrics recently enabled
            Files.write(batchMetricsEnabledConfFile, Boolean.toString(true).getBytes());
            for (int i = 0; i <= 12; i++) {
                mp.processMetrics(rootLocation, currentTimestamp);
                mp.trimOldSnapshots();
                mp.trimOldMetricsDBFiles();
                currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                assertEquals(i, mp.getBatchMetrics().size());
            }

            // Test batch metrics data during steady-state
            for (int i = 0; i < 5; i++) {
                mp.processMetrics(rootLocation, currentTimestamp);
                mp.trimOldSnapshots();
                mp.trimOldMetricsDBFiles();
                currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                assertEquals(12, mp.getBatchMetrics().size());
            }

            // Test batch metrics data as it's being disabled
            Files.write(batchMetricsEnabledConfFile, Boolean.toString(false).getBytes());
            mp.processMetrics(rootLocation, currentTimestamp);
            currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            assertEquals(12, mp.getBatchMetrics().size());

            // Test batch metrics data right after as it's disabled
            mp.trimOldSnapshots();
            mp.trimOldMetricsDBFiles();
            assertNull(mp.getBatchMetrics());

            secondRun = !secondRun;
        } while (secondRun);
    }

    @Test
    public void testTrimOldSnapshots() throws Exception {
        deleteAll();
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(false).getBytes());
        long currentTimestamp = 1597091740000L;
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());
        ReaderMetricsProcessor.setCurrentInstance(mp);
        PluginSettings.instance().setBatchMetricsRetentionPeriodMinutes(1);
        PluginSettings.instance().setShouldCleanupMetricsDBFiles(true);
        NavigableSet<Long> expectedTimestamps = new TreeSet<>();
        long metricsDBTimestamp = 1597091720000L;

        // Test ramp up
        for (int i = 0; i < 2; i++) {
            mp.processMetrics(rootLocation, currentTimestamp);
            mp.trimOldSnapshots();
            mp.trimOldMetricsDBFiles();
            currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            expectedTimestamps.add(metricsDBTimestamp);
            metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            verifyAvailableFiles(expectedTimestamps);
            verifyMetricsDBMap(expectedTimestamps);
        }

        // Test steady-state
        for (int i = 0; i < 7; i++) {
            mp.processMetrics(rootLocation, currentTimestamp);
            mp.trimOldSnapshots();
            mp.trimOldMetricsDBFiles();
            currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            expectedTimestamps.add(metricsDBTimestamp);
            metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            expectedTimestamps.pollFirst();
            verifyAvailableFiles(expectedTimestamps);
            verifyMetricsDBMap(expectedTimestamps);
        }

        boolean secondRun = false;
        do {
            // Test batch metrics enabled ramp up
            Files.write(
                    Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                    Boolean.toString(true).getBytes());
            for (int i = 0; i < 3; i++) {
                mp.processMetrics(rootLocation, currentTimestamp);
                mp.trimOldSnapshots();
                mp.trimOldMetricsDBFiles();
                currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                expectedTimestamps.add(metricsDBTimestamp);
                metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                expectedTimestamps.pollFirst();
                verifyAvailableFiles(expectedTimestamps);
                verifyMetricsDBMap(expectedTimestamps);
            }
            for (int i = 3; i <= 13; i++) {
                mp.processMetrics(rootLocation, currentTimestamp);
                mp.trimOldSnapshots();
                mp.trimOldMetricsDBFiles();
                currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                expectedTimestamps.add(metricsDBTimestamp);
                metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                verifyAvailableFiles(expectedTimestamps);
                verifyMetricsDBMap(expectedTimestamps);
            }

            // Test batch metrics enabled steady-state
            for (int i = 0; i < 7; i++) {
                mp.processMetrics(rootLocation, currentTimestamp);
                mp.trimOldSnapshots();
                mp.trimOldMetricsDBFiles();
                currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                expectedTimestamps.add(metricsDBTimestamp);
                metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                expectedTimestamps.pollFirst();
                verifyAvailableFiles(expectedTimestamps);
                verifyMetricsDBMap(expectedTimestamps);
            }

            // Test batch metrics disabled
            Files.write(
                    Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                    Boolean.toString(false).getBytes());
            mp.processMetrics(rootLocation, currentTimestamp);
            mp.trimOldSnapshots();
            mp.trimOldMetricsDBFiles();
            currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            expectedTimestamps.add(metricsDBTimestamp);
            metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            expectedTimestamps.pollFirst();
            verifyAvailableFiles(expectedTimestamps);
            verifyMetricsDBMap(expectedTimestamps);

            mp.processMetrics(rootLocation, currentTimestamp);
            mp.trimOldSnapshots();
            mp.trimOldMetricsDBFiles();
            currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            expectedTimestamps.add(metricsDBTimestamp);
            metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            while (expectedTimestamps.size() != 2) {
                expectedTimestamps.pollFirst();
            }
            verifyAvailableFiles(expectedTimestamps);
            verifyMetricsDBMap(expectedTimestamps);

            secondRun = !secondRun;
        } while (secondRun);
    }

    @Test
    public void testTrimOldSnapshots_fileCleanupDisabled() throws Exception {
        deleteAll();
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(false).getBytes());
        long currentTimestamp = 1597091740000L;
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());
        ReaderMetricsProcessor.setCurrentInstance(mp);
        PluginSettings.instance().setBatchMetricsRetentionPeriodMinutes(1);
        PluginSettings.instance().setShouldCleanupMetricsDBFiles(false);
        NavigableSet<Long> expectedTimestamps = new TreeSet<>();
        long metricsDBTimestamp = 1597091720000L;

        // Test metrics rampup and steady state
        for (int i = 0; i < 9; i++) {
            mp.processMetrics(rootLocation, currentTimestamp);
            mp.trimOldSnapshots();
            mp.trimOldMetricsDBFiles();
            currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            expectedTimestamps.add(metricsDBTimestamp);
            metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
            verifyAvailableFiles(expectedTimestamps);
        }

        boolean secondRun = false;
        do {
            // Test batch metrics enabled ramp up and steady-state
            Files.write(
                    Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                    Boolean.toString(true).getBytes());
            for (int i = 0; i < 21; i++) {
                mp.processMetrics(rootLocation, currentTimestamp);
                mp.trimOldSnapshots();
                mp.trimOldMetricsDBFiles();
                currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                expectedTimestamps.add(metricsDBTimestamp);
                metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                verifyAvailableFiles(expectedTimestamps);
            }

            // Test batch metrics disabled
            Files.write(
                    Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                    Boolean.toString(false).getBytes());
            for (int i = 0; i < 2; i++) {
                mp.processMetrics(rootLocation, currentTimestamp);
                mp.trimOldSnapshots();
                mp.trimOldMetricsDBFiles();
                currentTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                expectedTimestamps.add(metricsDBTimestamp);
                metricsDBTimestamp += MetricsConfiguration.SAMPLING_INTERVAL;
                verifyAvailableFiles(expectedTimestamps);
            }

            secondRun = !secondRun;
        } while (secondRun);
    }

    @Test
    public void testRestoreBatchMetricsState_restoreCleanup() throws Exception {
        // Test behavior when batch metrics is enabled and metricsdb cleanup is enabled
        deleteAll();
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(true).getBytes());
        PluginSettings.instance().setShouldCleanupMetricsDBFiles(true);
        PluginSettings.instance().setBatchMetricsRetentionPeriodMinutes(7);

        long currTime = System.currentTimeMillis();
        long ts1 = currTime - 1 * 60 * 1000;
        long ts2 = currTime - 2 * 60 * 1000;
        long ts3 =
                currTime
                        - (PluginSettings.instance().getBatchMetricsRetentionPeriodMinutes() + 1)
                                * 60
                                * 1000;
        for (Long ts : Arrays.asList(ts1, ts2, ts3)) {
            new MetricsDB(ts).remove();
        }
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());
        NavigableSet<Long> restored = mp.getBatchMetrics();
        assertTrue(restored.containsAll(Arrays.asList(ts1, ts2)) && restored.size() == 2);
        try {
            MetricsDB.fetchExisting(ts3);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testRestoreBatchMetricsState_restoreRetain() throws Exception {
        // Test behavior when batch metrics is enabled and metricsdb cleanup is disabled
        deleteAll();
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(true).getBytes());
        PluginSettings.instance().setShouldCleanupMetricsDBFiles(false);
        PluginSettings.instance().setBatchMetricsRetentionPeriodMinutes(7);

        long currTime = System.currentTimeMillis();
        long ts1 = currTime - 1 * 60 * 1000;
        long ts2 = currTime - 2 * 60 * 1000;
        long ts3 =
                currTime
                        - (PluginSettings.instance().getBatchMetricsRetentionPeriodMinutes() + 1)
                                * 60
                                * 1000;
        for (Long ts : Arrays.asList(ts1, ts2, ts3)) {
            new MetricsDB(ts).remove();
        }
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());
        NavigableSet<Long> restored = mp.getBatchMetrics();
        assertTrue(restored.containsAll(Arrays.asList(ts1, ts2)) && restored.size() == 2);
        try {
            MetricsDB.fetchExisting(ts3).remove();
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testRestoreBatchMetricsState_ignoreCleanup() throws Exception {
        // Test behavior when batch metrics is disabled and metricsdb cleanup is enabled
        deleteAll();
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(false).getBytes());
        PluginSettings.instance().setShouldCleanupMetricsDBFiles(true);
        PluginSettings.instance().setBatchMetricsRetentionPeriodMinutes(7);

        long currTime = System.currentTimeMillis();
        long ts1 = currTime - 1 * 60 * 1000;
        long ts2 = currTime - 2 * 60 * 1000;
        long ts3 =
                currTime
                        - (PluginSettings.instance().getBatchMetricsRetentionPeriodMinutes() + 1)
                                * 60
                                * 1000;
        for (Long ts : Arrays.asList(ts1, ts2, ts3)) {
            new MetricsDB(ts).remove();
        }
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(true).getBytes());
        mp.readBatchMetricsEnabledFromConfShim();
        NavigableSet<Long> restored = mp.getBatchMetrics();
        assertTrue(restored.isEmpty());
        for (Long ts : Arrays.asList(ts1, ts2, ts3)) {
            try {
                MetricsDB.fetchExisting(ts3);
                fail();
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testRestoreBatchMetricsState_ignoreRetain() throws Exception {
        // Test behavior when batch metrics is disabled and metricsdb cleanup is disabled
        deleteAll();
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(false).getBytes());
        PluginSettings.instance().setShouldCleanupMetricsDBFiles(false);
        PluginSettings.instance().setBatchMetricsRetentionPeriodMinutes(7);

        long currTime = System.currentTimeMillis();
        long ts1 = currTime - 1 * 60 * 1000;
        long ts2 = currTime - 2 * 60 * 1000;
        long ts3 =
                currTime
                        - (PluginSettings.instance().getBatchMetricsRetentionPeriodMinutes() + 1)
                                * 60
                                * 1000;
        for (Long ts : Arrays.asList(ts1, ts2, ts3)) {
            new MetricsDB(ts).remove();
        }
        ReaderMetricsProcessor mp =
                new ReaderMetricsProcessor(rootLocation, true, new AppContext());
        Files.write(
                Paths.get(Util.DATA_DIR, BATCH_METRICS_ENABLED_CONF_FILE),
                Boolean.toString(true).getBytes());
        mp.readBatchMetricsEnabledFromConfShim();
        NavigableSet<Long> restored = mp.getBatchMetrics();
        assertTrue(restored.isEmpty());
        try {
            for (Long ts : Arrays.asList(ts1, ts2, ts3)) {
                MetricsDB.fetchExisting(ts3).remove();
            }
        } catch (Exception e) {
            fail();
        }
    }

    public void verifyAvailableFiles(Set<Long> expectedFiles) {
        final File folder = new File("/tmp");
        final File[] files =
                folder.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(final File dir, final String name) {
                                return name.matches("metricsdb_.*");
                            }
                        });
        assertEquals(expectedFiles.size(), files.length);
        for (final File file : files) {
            String name = file.getName();
            long ts = Long.parseLong(name.substring(10));
            assertTrue(expectedFiles.contains(ts));
        }
    }

    public void verifyMetricsDBMap(Set<Long> possibleTimestamps) {
        Set<Long> metricsTimestamps =
                ReaderMetricsProcessor.getInstance().getMetricsDBMap().keySet();
        assertTrue(possibleTimestamps.containsAll(metricsTimestamps));
    }
}
