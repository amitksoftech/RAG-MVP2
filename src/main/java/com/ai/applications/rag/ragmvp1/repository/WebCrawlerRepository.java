package com.ai.applications.rag.ragmvp1.repository;

import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebCrawlerRepository extends JpaRepository<WebCrawler, UUID> {

    List<WebCrawler> findByCreatedByUsernameOrderByCreatedAtDesc(String username);

    Optional<WebCrawler> findByIdAndCreatedByUsername(UUID crawlerId, String username);

    long countByStatus(WebCrawler.CrawlerStatus status);

    List<WebCrawler> findByStatusOrderByStartedAtDesc(WebCrawler.CrawlerStatus status);
}
