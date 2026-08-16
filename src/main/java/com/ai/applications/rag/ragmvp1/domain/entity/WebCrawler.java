package com.ai.applications.rag.ragmvp1.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "web_crawler")
public class WebCrawler {

    public enum CrawlerStatus {
        IDLE, RUNNING, PAUSED, STOPPED, ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2000)
    private String seedUrl;

    @Column(nullable = false, length = 120)
    private String createdByUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlerStatus status;

    @Column(nullable = false)
    private long pagesCrawled;

    @Column(nullable = false)
    private long pagesQueued;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant pausedAt;

    @Column
    private Instant stoppedAt;

    @Column(length = 1000)
    private String errorMessage;

    @Column
    private int maxDepth;

    @Column
    private long maxPages;

    protected WebCrawler() {
    }

    public WebCrawler(String seedUrl, String createdByUsername) {
        this.seedUrl = seedUrl;
        this.createdByUsername = createdByUsername;
        this.status = CrawlerStatus.IDLE;
        this.pagesCrawled = 0;
        this.pagesQueued = 1;
        this.maxDepth = 3;
        this.maxPages = 500;
    }

    @PrePersist
    void beforeSave() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public void markRunning() {
        this.status = CrawlerStatus.RUNNING;
        this.startedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markPaused() {
        this.status = CrawlerStatus.PAUSED;
        this.pausedAt = Instant.now();
    }

    public void markResumed() {
        this.status = CrawlerStatus.RUNNING;
        this.pausedAt = null;
    }

    public void markStopped() {
        this.status = CrawlerStatus.STOPPED;
        this.stoppedAt = Instant.now();
    }

    public void markError(String error) {
        this.status = CrawlerStatus.ERROR;
        this.errorMessage = error;
        this.stoppedAt = Instant.now();
    }

    public void incrementPagesCrawled() {
        this.pagesCrawled++;
    }

    public void setQueued(long count) {
        this.pagesQueued = count;
    }

    // Getters
    public UUID getId() { return id; }
    public String getSeedUrl() { return seedUrl; }
    public String getCreatedByUsername() { return createdByUsername; }
    public CrawlerStatus getStatus() { return status; }
    public long getPagesCrawled() { return pagesCrawled; }
    public long getPagesQueued() { return pagesQueued; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getPausedAt() { return pausedAt; }
    public Instant getStoppedAt() { return stoppedAt; }
    public String getErrorMessage() { return errorMessage; }
    public int getMaxDepth() { return maxDepth; }
    public long getMaxPages() { return maxPages; }
}
