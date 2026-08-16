package com.ai.applications.rag.ragmvp1.domain.dto;

import com.ai.applications.rag.ragmvp1.domain.entity.SearchQueryLog;

import java.util.List;

public record MonitoringStatistics(
        // --- Ingestion health ---
        long totalDocuments,
        long readyDocuments,
        long processingDocuments,
        long failedDocuments,
        long totalChunksIndexed,
        double averageChunksPerDocument,

        // --- Retrieval health ---
        long totalQueries,
        long queriesToday,
        long uniqueSearchingUsers,
        double averageLatencyMs,
        long p95LatencyMs,
        double averageTopScore,
        double averageResultCount,
        long zeroResultQueries,

        // --- Quality signals ---
        long totalPositiveFeedback,
        long totalNegativeFeedback,
        /** Precision proxy: positive / (positive + negative); -1 when no feedback exists. */
        double positiveFeedbackRate,

        // --- Recent activity ---
        List<SearchQueryLog> recentQueries
) {
}
