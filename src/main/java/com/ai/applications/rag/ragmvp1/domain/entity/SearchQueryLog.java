package com.ai.applications.rag.ragmvp1.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_query_log")
public class SearchQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2000)
    private String query;

    @Column(nullable = false, length = 120)
    private String username;

    @Column(nullable = false, updatable = false)
    private Instant queryTimestamp;

    @Column(nullable = false)
    private long latencyMs;

    @Column(nullable = false)
    private int resultCount;

    /** Cosine similarity of the top-ranked result; null when no results were found. */
    @Column
    private Double topScore;

    /** Mean cosine similarity across all returned results; null when no results were found. */
    @Column
    private Double averageScore;

    /** Null = no feedback yet; true = helpful; false = not helpful. */
    @Column
    private Boolean feedbackPositive;

    protected SearchQueryLog() {
    }

    public SearchQueryLog(String query, String username, long latencyMs,
                          int resultCount, Double topScore, Double averageScore) {
        this.query = query;
        this.username = username;
        this.latencyMs = latencyMs;
        this.resultCount = resultCount;
        this.topScore = topScore;
        this.averageScore = averageScore;
    }

    @PrePersist
    void beforeSave() {
        if (this.queryTimestamp == null) {
            this.queryTimestamp = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public String getQuery() { return query; }
    public String getUsername() { return username; }
    public Instant getQueryTimestamp() { return queryTimestamp; }
    public long getLatencyMs() { return latencyMs; }
    public int getResultCount() { return resultCount; }
    public Double getTopScore() { return topScore; }
    public Double getAverageScore() { return averageScore; }
    public Boolean getFeedbackPositive() { return feedbackPositive; }
    public void setFeedbackPositive(Boolean feedbackPositive) { this.feedbackPositive = feedbackPositive; }
}
