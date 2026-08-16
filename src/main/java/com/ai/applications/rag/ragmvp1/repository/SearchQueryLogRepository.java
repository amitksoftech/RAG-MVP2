package com.ai.applications.rag.ragmvp1.repository;

import com.ai.applications.rag.ragmvp1.domain.entity.SearchQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SearchQueryLogRepository extends JpaRepository<SearchQueryLog, UUID> {

    List<SearchQueryLog> findTop20ByOrderByQueryTimestampDesc();

    @Query("SELECT COUNT(s) FROM SearchQueryLog s WHERE s.queryTimestamp >= :since")
    long countSince(@Param("since") Instant since);

    @Query("SELECT COUNT(DISTINCT s.username) FROM SearchQueryLog s")
    long countDistinctUsers();

    @Query("SELECT COALESCE(AVG(s.latencyMs), 0) FROM SearchQueryLog s")
    double avgLatencyMs();

    @Query("SELECT COALESCE(AVG(s.topScore), 0) FROM SearchQueryLog s WHERE s.topScore IS NOT NULL")
    double avgTopScore();

    @Query("SELECT COALESCE(AVG(s.resultCount), 0) FROM SearchQueryLog s")
    double avgResultCount();

    @Query("SELECT COUNT(s) FROM SearchQueryLog s WHERE s.resultCount = 0")
    long countZeroResults();

    @Query("SELECT COUNT(s) FROM SearchQueryLog s WHERE s.feedbackPositive = true")
    long countPositiveFeedback();

    @Query("SELECT COUNT(s) FROM SearchQueryLog s WHERE s.feedbackPositive = false")
    long countNegativeFeedback();

    /** Returns all latency values sorted ascending — used for percentile computation. */
    @Query("SELECT s.latencyMs FROM SearchQueryLog s ORDER BY s.latencyMs ASC")
    List<Long> findAllLatenciesSorted();
}
