package com.ai.applications.rag.ragmvp1.service.monitoring;

import com.ai.applications.rag.ragmvp1.domain.entity.DocumentStatus;
import com.ai.applications.rag.ragmvp1.domain.dto.MonitoringStatistics;
import com.ai.applications.rag.ragmvp1.domain.entity.SearchQueryLog;
import com.ai.applications.rag.ragmvp1.repository.KnowledgeDocumentRepository;
import com.ai.applications.rag.ragmvp1.repository.SearchQueryLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class MonitoringService {

    private final KnowledgeDocumentRepository documentRepository;
    private final SearchQueryLogRepository queryLogRepository;

    public MonitoringService(KnowledgeDocumentRepository documentRepository,
                             SearchQueryLogRepository queryLogRepository) {
        this.documentRepository = documentRepository;
        this.queryLogRepository = queryLogRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public MonitoringStatistics getStatistics() {
        // --- Ingestion ---
        long totalDocuments = documentRepository.count();
        long readyDocuments = documentRepository.countByStatus(DocumentStatus.READY);
        long processingDocuments = documentRepository.countByStatus(DocumentStatus.PROCESSING);
        long failedDocuments = documentRepository.countByStatus(DocumentStatus.FAILED);

        long totalChunksIndexed = documentRepository.findAllByOrderByUploadedAtDesc().stream()
                .mapToLong(d -> d.getChunkCount())
                .sum();
        double averageChunksPerDocument = totalDocuments == 0 ? 0.0
                : (double) totalChunksIndexed / totalDocuments;

        // --- Retrieval ---
        long totalQueries = queryLogRepository.count();

        Instant startOfDay = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault()).toInstant();
        long queriesToday = queryLogRepository.countSince(startOfDay);

        long uniqueSearchingUsers = queryLogRepository.countDistinctUsers();
        double averageLatencyMs = queryLogRepository.avgLatencyMs();
        double averageTopScore = queryLogRepository.avgTopScore();
        double averageResultCount = queryLogRepository.avgResultCount();
        long zeroResultQueries = queryLogRepository.countZeroResults();

        // --- Quality signals ---
        long totalPositiveFeedback = queryLogRepository.countPositiveFeedback();
        long totalNegativeFeedback = queryLogRepository.countNegativeFeedback();
        long feedbackTotal = totalPositiveFeedback + totalNegativeFeedback;
        double positiveFeedbackRate = feedbackTotal == 0 ? -1.0
                : (double) totalPositiveFeedback / feedbackTotal;

        // P95 latency — computed in-process from sorted values
        long p95LatencyMs = computePercentile(queryLogRepository.findAllLatenciesSorted(), 95);

        // --- Recent activity ---
        List<SearchQueryLog> recentQueries = queryLogRepository.findTop20ByOrderByQueryTimestampDesc();

        return new MonitoringStatistics(
                totalDocuments, readyDocuments, processingDocuments, failedDocuments,
                totalChunksIndexed, averageChunksPerDocument,
                totalQueries, queriesToday, uniqueSearchingUsers,
                averageLatencyMs, p95LatencyMs, averageTopScore, averageResultCount, zeroResultQueries,
                totalPositiveFeedback, totalNegativeFeedback, positiveFeedbackRate,
                recentQueries
        );
    }

    private long computePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }
}
