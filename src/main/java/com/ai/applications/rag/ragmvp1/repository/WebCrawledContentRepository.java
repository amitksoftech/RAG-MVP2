package com.ai.applications.rag.ragmvp1.repository;

import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawledContent;
import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebCrawledContentRepository extends JpaRepository<WebCrawledContent, UUID> {

    List<WebCrawledContent> findByCrawlerIdOrderByScrapedAtDesc(UUID crawlerId);

    Optional<WebCrawledContent> findByUrl(String url);

    Optional<WebCrawledContent> findByUrlAndCrawlerId(String url, UUID crawlerId);

    long countByCrawlerId(UUID crawlerId);

    long countByContentHash(String contentHash);
}
