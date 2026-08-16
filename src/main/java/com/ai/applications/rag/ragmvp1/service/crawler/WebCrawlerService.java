package com.ai.applications.rag.ragmvp1.service.crawler;

import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawledContent;
import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawler;
import com.ai.applications.rag.ragmvp1.repository.WebCrawledContentRepository;
import com.ai.applications.rag.ragmvp1.repository.WebCrawlerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class WebCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(WebCrawlerService.class);

    private final WebCrawlerRepository crawlerRepository;
    private final WebCrawledContentRepository contentRepository;
    private final WebScraperService scraperService;
    private final CrawlQueueService queueService;
    private final VectorStore vectorStore;
    private final CrawlerAsyncRunner asyncRunner;

    public WebCrawlerService(WebCrawlerRepository crawlerRepository,
                             WebCrawledContentRepository contentRepository,
                             WebScraperService scraperService,
                             CrawlQueueService queueService,
                             @Qualifier("applicationVectorStore") VectorStore vectorStore,
                             @Lazy CrawlerAsyncRunner asyncRunner) {
        this.crawlerRepository = crawlerRepository;
        this.contentRepository = contentRepository;
        this.scraperService = scraperService;
        this.queueService = queueService;
        this.vectorStore = vectorStore;
        this.asyncRunner = asyncRunner;
    }

    /** Persist the crawler, seed the queue, then fire-and-forget the crawl loop. */
    public WebCrawler startCrawler(String seedUrl, String username) {
        WebCrawler crawler = new WebCrawler(seedUrl, username);
        crawler.markRunning();
        crawler = crawlerRepository.save(crawler);   // commits immediately — no outer @Transactional

        queueService.initializeQueue(crawler.getId(), seedUrl);
        log.info("Crawler {} started for seed URL: {}", crawler.getId(), seedUrl);

        asyncRunner.runCrawlLoop(crawler.getId());   // fires after commit is visible
        return crawler;
    }

    /**
     * Called from the async loop. HTTP fetch is intentionally outside @Transactional
     * so we don't hold a DB connection open during a 10-second network call.
     */
    public void crawlOnePage(UUID crawlerId) {
        String url = queueService.dequeueUrl(crawlerId);
        if (url == null) return;

        if (queueService.isVisited(crawlerId, url)) {
            log.debug("Already visited, skipping: {}", url);
            return;
        }

        log.info("[Crawler {}] Fetching: {}", crawlerId, url);

        // Scrape outside any transaction — can take up to 10 s
        WebScraperService.ScrapedPage page;
        try {
            page = scraperService.scrapeUrl(url);
        } catch (Exception e) {
            log.warn("[Crawler {}] Scrape exception for {}: {}", crawlerId, url, e.getMessage());
            queueService.markFailed(crawlerId, url);
            return;
        }

        if (page == null || page.text == null || page.text.isBlank()) {
            log.warn("[Crawler {}] No content from: {}", crawlerId, url);
            queueService.markFailed(crawlerId, url);
            return;
        }

        log.info("[Crawler {}] Scraped '{}' — {} chars, {} links",
                crawlerId, page.title, page.text.length(), page.links.size());

        // Enqueue all links before persisting so queue is populated even if save fails
        int enqueued = 0;
        for (String link : page.links) {
            int before = queueService.getQueueSize(crawlerId);
            queueService.enqueueUrl(crawlerId, link);
            if (queueService.getQueueSize(crawlerId) > before) enqueued++;
        }
        log.info("[Crawler {}] Enqueued {} new links from {}", crawlerId, enqueued, url);

        // Only dedup non-trivial content (skip dedup for very short pages)
        String contentHash = sha256(page.text);
        if (page.text.length() > 200 && !contentHash.isEmpty()
                && contentRepository.countByContentHash(contentHash) > 0) {
            log.info("[Crawler {}] Duplicate content at {}, skipping index", crawlerId, url);
            queueService.markVisited(crawlerId, url);
            return;
        }

        // Persist scraped page record — short-lived transaction, no vector ops inside
        WebCrawledContent saved;
        try {
            saved = saveContent(crawlerId, url, page, contentHash);
        } catch (Exception e) {
            log.error("[Crawler {}] DB save failed for {}: {}", crawlerId, url, e.getMessage(), e);
            queueService.markVisited(crawlerId, url);
            return;
        }

        // Index into vector store — outside transaction to avoid long-held connections
        try {
            List<String> chunks = chunkText(page.text);
            indexChunks(saved, chunks);
            updateChunkCount(saved.getId(), chunks.size());
            log.info("[Crawler {}] Indexed {} chunks from '{}'", crawlerId, chunks.size(), url);
        } catch (Exception e) {
            log.error("[Crawler {}] Vector indexing failed for {}: {}", crawlerId, url, e.getMessage(), e);
            // Content is already saved — just log and continue
        }
    }

    @Transactional
    public void pauseCrawler(UUID crawlerId) {
        crawlerRepository.findById(crawlerId).ifPresent(c -> {
            c.markPaused();
            crawlerRepository.save(c);
        });
    }

    @Transactional
    public void resumeCrawler(UUID crawlerId) {
        crawlerRepository.findById(crawlerId).ifPresent(c -> {
            c.markResumed();
            crawlerRepository.save(c);
        });
        // NOTE: controller must call relaunchLoop() AFTER this method returns so commit is visible
    }

    /** Re-launches the async loop; call after resumeCrawler() has committed. */
    public void relaunchLoop(UUID crawlerId) {
        asyncRunner.runCrawlLoop(crawlerId);
    }

    @Transactional
    public void stopCrawler(UUID crawlerId) {
        crawlerRepository.findById(crawlerId).ifPresent(c -> {
            c.markStopped();
            crawlerRepository.save(c);
        });
        queueService.clear(crawlerId);
    }

    @Transactional
    public void markStopped(UUID crawlerId) {
        crawlerRepository.findById(crawlerId).ifPresent(c -> {
            c.markStopped();
            crawlerRepository.save(c);
        });
    }

    @Transactional
    public void markError(UUID crawlerId, String message) {
        crawlerRepository.findById(crawlerId).ifPresent(c -> {
            c.markError(message != null ? message : "Unknown error");
            crawlerRepository.save(c);
        });
    }

    public WebCrawler getCrawler(UUID crawlerId) {
        return crawlerRepository.findById(crawlerId)
                .orElseThrow(() -> new IllegalArgumentException("Crawler not found: " + crawlerId));
    }

    public List<WebCrawledContent> getCrawledContent(UUID crawlerId) {
        return contentRepository.findByCrawlerIdOrderByScrapedAtDesc(crawlerId);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Saves the crawled page and updates pagesCrawled on the crawler entity. */
    @Transactional
    public WebCrawledContent saveContent(UUID crawlerId, String url,
                                         WebScraperService.ScrapedPage page, String contentHash) {
        WebCrawler crawler = crawlerRepository.findById(crawlerId)
                .orElseThrow(() -> new IllegalArgumentException("Crawler not found: " + crawlerId));

        WebCrawledContent content = new WebCrawledContent(crawler, url, page.title, page.text);
        content.setContentHash(contentHash);
        content = contentRepository.save(content);

        crawler.incrementPagesCrawled();
        queueService.markVisited(crawlerId, url);
        crawler.setQueued(queueService.getQueueSize(crawlerId));
        crawlerRepository.save(crawler);
        return content;
    }

    @Transactional
    public void updateChunkCount(UUID contentId, int count) {
        contentRepository.findById(contentId).ifPresent(c -> {
            c.setChunkCount(count);
            contentRepository.save(c);
        });
    }

    private void indexChunks(WebCrawledContent content, List<String> chunks) {
        List<Document> docs = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document doc = new Document(chunks.get(i));
            doc.getMetadata().put("source_type", "crawler");
            doc.getMetadata().put("crawler_id", content.getCrawler().getId().toString());
            doc.getMetadata().put("content_id", content.getId().toString());
            doc.getMetadata().put("source_url", content.getUrl());
            doc.getMetadata().put("source_title", content.getTitle() != null ? content.getTitle() : "");
            doc.getMetadata().put("chunk_number", i);
            docs.add(doc);
        }
        if (!docs.isEmpty()) {
            vectorStore.add(docs);
        }
    }

    private List<String> chunkText(String text) {
        if (text == null || text.isBlank()) return List.of();

        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] sentences = normalized.split("(?<=[.!?])\\s+|\\n{2,}");

        List<String> chunks = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        final int MAX = 1000;

        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.isBlank()) continue;
            if (buf.length() + trimmed.length() > MAX && buf.length() > 0) {
                chunks.add(buf.toString().trim());
                buf = new StringBuilder();
            }
            buf.append(trimmed).append(" ");
        }
        if (buf.length() > 0) chunks.add(buf.toString().trim());

        return chunks.isEmpty() ? List.of(normalized.substring(0, Math.min(normalized.length(), MAX))) : chunks;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
