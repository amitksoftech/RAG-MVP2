package com.ai.applications.rag.ragmvp1.service.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class WebScraperService {

    private static final Logger log = LoggerFactory.getLogger(WebScraperService.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; InsightVaultBot/1.0; +https://github.com/insightvault)";
    private static final int TIMEOUT_MS = 15000;
    private static final int MAX_BODY_CHARS = 200_000;

    public ScrapedPage scrapeUrl(String urlString) {
        try {
            Connection.Response resp = Jsoup.connect(urlString)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)         // don't throw on 4xx/5xx
                    .ignoreContentType(false)
                    .maxBodySize(MAX_BODY_CHARS)
                    .execute();

            int status = resp.statusCode();
            if (status >= 400) {
                log.warn("HTTP {} for {}", status, urlString);
                return null;
            }

            String contentType = resp.contentType();
            if (contentType != null && !contentType.contains("html") && !contentType.contains("text")) {
                log.debug("Skipping non-HTML content type '{}' at {}", contentType, urlString);
                return null;
            }

            Document doc = resp.parse();
            String title = extractTitle(doc);
            String text = extractText(doc);
            List<String> links = extractLinks(doc);

            log.debug("Scraped '{}' ({} chars, {} links) from {}", title, text.length(), links.size(), urlString);
            return new ScrapedPage(title, text, links);

        } catch (Exception e) {
            log.warn("Failed to scrape {}: {}", urlString, e.getMessage());
            return null;
        }
    }

    private String extractTitle(Document doc) {
        String title = doc.title();
        if (title != null && !title.isBlank()) return title.trim();
        Element h1 = doc.selectFirst("h1");
        return (h1 != null && !h1.text().isBlank()) ? h1.text().trim() : "Untitled";
    }

    private String extractText(Document doc) {
        // Remove noise before any text extraction
        doc.select("script, style, noscript, nav, header, footer, aside, " +
                   ".nav, .navbar, .menu, .sidebar, .advertisement, .ads, .ad, " +
                   ".cookie, .popup, .modal, [role=navigation]").remove();

        StringBuilder sb = new StringBuilder();

        // Prefer semantic content containers; fall back to <body>
        Elements content = doc.select("main, article, [role=main], #content, #main, .content, .post, .entry");
        Element root = content.isEmpty() ? doc.body() : content.first();

        if (root != null) {
            for (Element el : root.select("h1, h2, h3, h4, h5, h6, p, li, td, th, blockquote, pre")) {
                String t = el.ownText().trim();
                if (!t.isBlank()) {
                    sb.append(t).append("\n");
                }
            }
        }

        String result = sb.toString().trim();
        // Fallback: get all body text if structured extraction yielded nothing
        if (result.isEmpty() && doc.body() != null) {
            result = doc.body().text().trim();
        }

        return result.length() > MAX_BODY_CHARS ? result.substring(0, MAX_BODY_CHARS) : result;
    }

    private List<String> extractLinks(Document doc) {
        Set<String> seen = new LinkedHashSet<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("abs:href").trim();
            if (href.isEmpty()) continue;
            if (!href.startsWith("http://") && !href.startsWith("https://")) continue;
            // Strip fragment and query-only anchors
            int hash = href.indexOf('#');
            if (hash > 0) href = href.substring(0, hash);
            if (href.endsWith("?")) href = href.substring(0, href.length() - 1);
            seen.add(href);
        }
        return new ArrayList<>(seen);
    }

    public static class ScrapedPage {
        public final String title;
        public final String text;
        public final List<String> links;

        public ScrapedPage(String title, String text, List<String> links) {
            this.title = title;
            this.text = text;
            this.links = links;
        }
    }
}

