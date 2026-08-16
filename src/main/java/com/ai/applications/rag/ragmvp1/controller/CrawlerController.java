package com.ai.applications.rag.ragmvp1.controller;

import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawledContent;
import com.ai.applications.rag.ragmvp1.domain.entity.WebCrawler;
import com.ai.applications.rag.ragmvp1.repository.WebCrawlerRepository;
import com.ai.applications.rag.ragmvp1.service.crawler.WebCrawlerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/crawler")
public class CrawlerController {

    private final WebCrawlerService crawlerService;
    private final WebCrawlerRepository crawlerRepository;

    public CrawlerController(WebCrawlerService crawlerService, WebCrawlerRepository crawlerRepository) {
        this.crawlerService = crawlerService;
        this.crawlerRepository = crawlerRepository;
    }

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        List<WebCrawler> crawlers = crawlerRepository.findByCreatedByUsernameOrderByCreatedAtDesc(auth.getName());
        model.addAttribute("crawlers", crawlers);
        return "crawler/dashboard";
    }

    /** Start crawl — returns immediately, crawling runs in background. */
    @PostMapping("/start")
    public String start(@RequestParam("seedUrl") String seedUrl,
                        Authentication auth,
                        RedirectAttributes flash) {
        if (seedUrl == null || seedUrl.isBlank()) {
            flash.addFlashAttribute("error", "Please enter a valid seed URL.");
            return "redirect:/crawler";
        }
        try {
            WebCrawler crawler = crawlerService.startCrawler(seedUrl.trim(), auth.getName());
            flash.addFlashAttribute("message", "Crawler started — crawling in background.");
            return "redirect:/crawler/" + crawler.getId();
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Failed to start crawler: " + e.getMessage());
            return "redirect:/crawler";
        }
    }

    @GetMapping("/{crawlerId}")
    public String statusPage(@PathVariable UUID crawlerId, Authentication auth, Model model) {
        WebCrawler crawler = crawlerRepository.findByIdAndCreatedByUsername(crawlerId, auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Crawler not found or access denied"));
        List<WebCrawledContent> content = crawlerService.getCrawledContent(crawlerId);
        model.addAttribute("crawler", crawler);
        model.addAttribute("content", content);
        return "crawler/status";
    }

    /** Lightweight JSON endpoint for live polling from the status page. */
    @GetMapping("/{crawlerId}/poll")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> poll(@PathVariable UUID crawlerId, Authentication auth) {
        return crawlerRepository.findByIdAndCreatedByUsername(crawlerId, auth.getName())
                .map(c -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", c.getStatus().name());
                    body.put("pagesCrawled", c.getPagesCrawled());
                    body.put("pagesQueued", c.getPagesQueued());
                    body.put("errorMessage", c.getErrorMessage() != null ? c.getErrorMessage() : "");
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{crawlerId}/pause")
    public String pause(@PathVariable UUID crawlerId, Authentication auth, RedirectAttributes flash) {
        ownerOnly(crawlerId, auth);
        crawlerService.pauseCrawler(crawlerId);
        flash.addFlashAttribute("message", "Crawler paused.");
        return "redirect:/crawler/" + crawlerId;
    }

    @PostMapping("/{crawlerId}/resume")
    public String resume(@PathVariable UUID crawlerId, Authentication auth, RedirectAttributes flash) {
        ownerOnly(crawlerId, auth);
        crawlerService.resumeCrawler(crawlerId);      // commits status=RUNNING
        crawlerService.relaunchLoop(crawlerId);       // loop starts after commit is visible
        flash.addFlashAttribute("message", "Crawler resumed.");
        return "redirect:/crawler/" + crawlerId;
    }

    @PostMapping("/{crawlerId}/stop")
    public String stop(@PathVariable UUID crawlerId, Authentication auth, RedirectAttributes flash) {
        ownerOnly(crawlerId, auth);
        crawlerService.stopCrawler(crawlerId);
        flash.addFlashAttribute("message", "Crawler stopped.");
        return "redirect:/crawler/" + crawlerId;
    }

    private void ownerOnly(UUID crawlerId, Authentication auth) {
        crawlerRepository.findByIdAndCreatedByUsername(crawlerId, auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Crawler not found or access denied"));
    }
}
