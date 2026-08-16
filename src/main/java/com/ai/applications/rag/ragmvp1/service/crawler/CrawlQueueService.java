package com.ai.applications.rag.ragmvp1.service.crawler;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-crawler URL queue, keyed by crawler ID so concurrent sessions don't interfere.
 */
@Service
public class CrawlQueueService {

    private static class CrawlerState {
        final Queue<String> queue = new LinkedList<>();
        final Set<String> visited = new HashSet<>();
        final Set<String> failed = new HashSet<>();
        final Set<String> pending = new HashSet<>();   // URLs currently in queue
        String seedDomain = "";
    }

    private final Map<UUID, CrawlerState> states = new ConcurrentHashMap<>();

    public void initializeQueue(UUID crawlerId, String seedUrl) {
        CrawlerState state = new CrawlerState();
        String clean = cleanUrl(seedUrl);
        state.seedDomain = extractDomain(clean);
        state.queue.add(clean);
        state.pending.add(clean);
        states.put(crawlerId, state);
    }

    public String dequeueUrl(UUID crawlerId) {
        CrawlerState state = states.get(crawlerId);
        if (state == null) return null;
        String url = state.queue.poll();
        if (url != null) state.pending.remove(url);
        return url;
    }

    public void enqueueUrl(UUID crawlerId, String url) {
        CrawlerState state = states.get(crawlerId);
        if (state == null || url == null || url.isBlank()) return;
        if (!isValidUrl(url)) return;
        String clean = cleanUrl(url);
        // Skip if already visited, failed, or already sitting in the queue
        if (!state.visited.contains(clean) && !state.failed.contains(clean)
                && !state.pending.contains(clean) && isSameDomain(state, clean)) {
            state.queue.add(clean);
            state.pending.add(clean);
        }
    }

    public void markVisited(UUID crawlerId, String url) {
        CrawlerState state = states.get(crawlerId);
        if (state != null) {
            String clean = cleanUrl(url);
            state.visited.add(clean);
            state.pending.remove(clean);
        }
    }

    public void markFailed(UUID crawlerId, String url) {
        CrawlerState state = states.get(crawlerId);
        if (state != null) {
            String clean = cleanUrl(url);
            state.failed.add(clean);
            state.pending.remove(clean);
        }
    }

    public boolean isVisited(UUID crawlerId, String url) {
        CrawlerState state = states.get(crawlerId);
        return state != null && state.visited.contains(cleanUrl(url));
    }

    public int getQueueSize(UUID crawlerId) {
        CrawlerState state = states.get(crawlerId);
        return state != null ? state.queue.size() : 0;
    }

    public long getVisitedCount(UUID crawlerId) {
        CrawlerState state = states.get(crawlerId);
        return state != null ? state.visited.size() : 0;
    }

    public long getFailedCount(UUID crawlerId) {
        CrawlerState state = states.get(crawlerId);
        return state != null ? state.failed.size() : 0;
    }

    public void clear(UUID crawlerId) {
        states.remove(crawlerId);
    }

    private String extractDomain(String url) {
        try {
            String host = new URI(url).getHost();
            return host != null ? host.toLowerCase() : "";
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private boolean isSameDomain(CrawlerState state, String url) {
        String domain = extractDomain(url);
        return domain.equals(state.seedDomain) || domain.endsWith("." + state.seedDomain);
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            new URI(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String cleanUrl(String url) {
        int idx = url.indexOf('#');
        return idx > 0 ? url.substring(0, idx).trim() : url.trim();
    }
}

