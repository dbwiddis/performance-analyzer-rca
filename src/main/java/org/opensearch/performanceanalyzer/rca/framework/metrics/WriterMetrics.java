/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.metrics;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public enum WriterMetrics implements MeasurementSet {
    /** Measures the time spent in deleting the event log files */
    EVENT_LOG_FILES_DELETION_TIME(
            "EventLogFilesDeletionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    /** Measures the count of event log files deleted */
    EVENT_LOG_FILES_DELETED(
            "EventLogFilesDeleted", "count", Arrays.asList(Statistics.MAX, Statistics.SUM)),

    /** Metrics tracking Plugin level: 1. Errors 2. Exceptions */

    /** Tracks stale metrics - metrics to be collected is behind current bucket */
    STALE_METRICS("StaleMetrics"),

    /** Tracks the number of VM attach/dataDump or detach failures. */
    JVM_ATTACH_ERROR("JvmAttachErrror"),

    /** java_pid file is missing. */
    JVM_ATTACH_ERROR_JAVA_PID_FILE_MISSING("JvmAttachErrorJavaPidFileMissing"),

    /** Lock could not be acquired within the timeout. */
    JVM_ATTACH_LOCK_ACQUISITION_FAILED("JvmAttachLockAcquisitionFailed"),

    /** ThreadState could not be found for an OpenSearch thread in the critical OpenSearch path. */
    NO_THREAD_STATE_INFO("NoThreadStateInfo"),

    /**
     * Successfully completed a thread-dump. An omission of indicate thread taking the dump got
     * stuck.
     */
    JVM_THREAD_DUMP_SUCCESSFUL("JvmThreadDumpSuccessful"),

    /** Thread ID is no loner exists */
    JVM_THREAD_ID_NO_LONGER_EXISTS("JVMThreadIdNoLongerExists"),

    /** Tracks the number of muted collectors */
    COLLECTORS_MUTED(
            "CollectorsMutedCount",
            "namedCount",
            Collections.singletonList(Statistics.NAMED_COUNTERS)),

    /** This metric indicates failure in collecting ClusterManagerServiceEventMetrics */
    CLUSTER_MANAGER_METRICS_ERROR("ClusterManagerMetricsError"),

    /** This metric indicates cluster_manager is not up */
    CLUSTER_MANAGER_NODE_NOT_UP("ClusterManagerNodeNotUp"),

    /** This metric indicates faiure in intercepting opensearch requests at transport channel */
    OPENSEARCH_REQUEST_INTERCEPTOR_ERROR("OpenSearchRequestInterceptorError"),

    /** Collector specific metrics */
    SHARD_STATE_COLLECTOR_EXECUTION_TIME(
            "ShardStateCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    CLUSTER_MANAGER_THROTTLING_COLLECTOR_EXECUTION_TIME(
            "ClusterManagerThrottlingCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    CIRCUIT_BREAKER_COLLECTOR_EXECUTION_TIME(
            "CircuitBreakerCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    CLUSTER_MANAGER_THROTTLING_COLLECTOR_NOT_AVAILABLE(
            "ClusterManagerThrottlingCollectorNotAvailable",
            "count",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    ADMISSION_CONTROL_COLLECTOR_EXECUTION_TIME(
            "AdmissionControlCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    ADMISSION_CONTROL_COLLECTOR_NOT_AVAILABLE(
            "AdmissionControlCollectorNotAvailable",
            "count",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    FAULT_DETECTION_COLLECTOR_EXECUTION_TIME(
            "FaultDetectionCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    CLUSTER_APPLIER_SERVICE_STATS_COLLECTOR_EXECUTION_TIME(
            "ClusterApplierServiceStatsCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    CLUSTER_MANAGER_CLUSTER_UPDATE_STATS_COLLECTOR_DISABLED(
            "ClusterManagerClusterUpdateStatsCollectorDisabled",
            "count",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    CLUSTER_MANAGER_CLUSTER_UPDATE_STATS_COLLECTOR_EXECUTION_TIME(
            "ClusterManagerClusterUpdateStatsCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    ELECTION_TERM_COLLECTOR_EXECUTION_TIME(
            "ElectionTermCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    SHARD_INDEXING_PRESSURE_COLLECTOR_EXECUTION_TIME(
            "ShardIndexingPressureCollectorExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    /** This metric indicates that the writer file creation was skipped. */
    WRITER_FILE_CREATION_SKIPPED(
            "WriterFileCreationSkipped", "count", Arrays.asList(Statistics.COUNT)),

    /** This metric indicates metric entry insertion to event log queue failed */
    METRICS_WRITE_ERROR(
            "MetricsWriteError",
            "namedCount",
            Collections.singletonList(Statistics.NAMED_COUNTERS)),

    /** This metric indicates faiure in cleaning up the event log files */
    METRICS_REMOVE_ERROR("MetricsRemoveError", "count", Arrays.asList(Statistics.COUNT)),

    /** This metric indicates faiure in cleaning up the event log files */
    METRICS_REMOVE_FAILURE("MetricsRemoveFailure", "count", Arrays.asList(Statistics.COUNT)),

    /** This metric indicates that error occurred while closing grpc channels. */
    GRPC_CHANNEL_CLOSURE_ERROR("GrpcChannelClosureError", "count", Arrays.asList(Statistics.COUNT)),

    /** This metric indicates that error occurred while closing grpc server. */
    GRPC_SERVER_CLOSURE_ERROR("GrpcServerClosureError", "count", Arrays.asList(Statistics.COUNT)),

    /** This metric indicates that error occurred while closing metrics db. */
    METRICS_DB_CLOSURE_ERROR("MetricsDbClosureError", "count", Arrays.asList(Statistics.COUNT)),

    /** This metric indicates that error occurred while closing database connection. */
    IN_MEMORY_DATABASE_CONN_CLOSURE_ERROR(
            "InMemoryDatabaseConnClosureError", "count", Arrays.asList(Statistics.COUNT));

    /** What we want to appear as the metric name. */
    private String name;

    /**
     * The unit the measurement is in. This is not used for the statistics calculations but as an
     * information that will be dumped with the metrics.
     */
    private String unit;

    /**
     * Multiple statistics can be collected for each measurement like MAX, MIN and MEAN. This is a
     * collection of one or more such statistics.
     */
    private List<Statistics> statsList;

    WriterMetrics(String name) {
        this.name = name;
        this.unit = "count";
        this.statsList = Collections.singletonList(Statistics.COUNT);
    }

    WriterMetrics(String name, String unit, List<Statistics> stats) {
        this.name = name;
        this.unit = unit;
        this.statsList = stats;
    }

    WriterMetrics(String name, String unit, Statistics stats) {
        this(name, unit, Collections.singletonList(stats));
    }

    public String toString() {
        return new StringBuilder(name).append("-").append(unit).toString();
    }

    @Override
    public List<Statistics> getStatsList() {
        return statsList;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUnit() {
        return unit;
    }
}
