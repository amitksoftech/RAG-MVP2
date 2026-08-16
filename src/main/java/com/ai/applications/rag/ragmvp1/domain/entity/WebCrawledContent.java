package com.ai.applications.rag.ragmvp1.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "web_crawled_content")
public class WebCrawledContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crawler_id")
    private WebCrawler crawler;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(nullable = false)
    private int chunkCount;

    @Column(nullable = false, updatable = false)
    private Instant scrapedAt;

    @Column
    private String contentHash;

    protected WebCrawledContent() {
    }

    public WebCrawledContent(WebCrawler crawler, String url, String title, String rawText) {
        this.crawler = crawler;
        this.url = url;
        this.title = title;
        this.rawText = rawText;
        this.chunkCount = 0;
    }

    @PrePersist
    void beforeSave() {
        if (this.scrapedAt == null) {
            this.scrapedAt = Instant.now();
        }
    }

    public void setChunkCount(int count) {
        this.chunkCount = count;
    }

    public void setContentHash(String hash) {
        this.contentHash = hash;
    }

    // Getters
    public UUID getId() { return id; }
    public WebCrawler getCrawler() { return crawler; }
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getRawText() { return rawText; }
    public int getChunkCount() { return chunkCount; }
    public Instant getScrapedAt() { return scrapedAt; }
    public String getContentHash() { return contentHash; }
}
