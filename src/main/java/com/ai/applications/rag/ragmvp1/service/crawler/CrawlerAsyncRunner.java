package com.ai.applications.rag.ragmvp1.service.crawler;

import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawler;
import com.ai.applications.rag.ragmvp1.repository.WebCrawlerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Separate bean so @Async works — calling @Async from the same bean bypasses the proxy.
 */
@Service
public class CrawlerAsyncRunner {

    private static final Logger log = LoggerFactory.getLogger(CrawlerAsyncRunner.class);
    private static final long CRAWL_DELAY_MS = 800;

    private final WebCrawlerRepository crawlerRepository;
    private final WebCrawlerService crawlerService;
    private final CrawlQueueService queueService;

    public CrawlerAsyncRunner(WebCrawlerRepository crawlerRepository,
                              WebCrawlerService crawlerService,
                              CrawlQueueService queueService) {
        this.crawlerRepository = crawlerRepository;
        this.crawlerService = crawlerService;
        this.queueService = queueService;
    }

    @Async("crawlerExecutor")
    public void runCrawlLoop(UUID crawlerId) {
        log.info("[Crawler {}] Loop started", crawlerId);
        int consecutiveEmpty = 0;

        try {
            while (true) {
                WebCrawler crawler = crawlerRepository.findById(crawlerId).orElse(null);
                if (crawler == null) {
                    log.warn("[Crawler {}] Entity disappeared, stopping", crawlerId);
                    break;
                }

                WebCrawler.CrawlerStatus status = crawler.getStatus();

                if (status == WebCrawler.CrawlerStatus.STOPPED || status == WebCrawler.CrawlerStatus.ERROR) {
                    log.info("[Crawler {}] Status is {}, exiting loop", crawlerId, status);
                    break;
                }

                if (status == WebCrawler.CrawlerStatus.PAUSED) {
                    Thread.sleep(2000);
                    continue;
                }

                if (crawler.getPagesCrawled() >= crawler.getMaxPages()) {
                    log.info("[Crawler {}] Reached maxPages {}, stopping", crawlerId, crawler.getMaxPages());
                    crawlerService.markStopped(crawlerId);
                    break;
                }

                int queueSize = queueService.getQueueSize(crawlerId);
                if (queueSize == 0) {
                    consecutiveEmpty++;
                    if (consecutiveEmpty >= 3) {
                        log.info("[Crawler {}] Queue empty after {} checks, stopping (crawled={})",
                                crawlerId, consecutiveEmpty, crawler.getPagesCrawled());
                        crawlerService.markStopped(crawlerId);
                        break;
                    }
                    // Brief wait then retry \u2014 pages in-flight may still enqueue new links
                    Thread.sleep(1000);
                    continue;
                }

                consecutiveEmpty = 0;
                log.info("[Crawler {}] Queue size={}, crawled={}", crawlerId, queueSize, crawler.getPagesCrawled());
                crawlerService.crawlOnePage(crawlerId);
                Thread.sleep(CRAWL_DELAY_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("[Crawler {}] Loop interrupted", crawlerId);
        } catch (Exception e) {
            log.error("[Crawler {}] Loop fatal error: {}", crawlerId, e.getMessage(), e);
            crawlerService.markError(crawlerId, e.getMessage());
        }

        log.info("[Crawler {}] Loop ended", crawlerId);
    }
}
