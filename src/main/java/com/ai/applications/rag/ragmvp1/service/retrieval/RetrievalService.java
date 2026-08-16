package com.ai.applications.rag.ragmvp1.service.retrieval;

import com.ai.applications.rag.ragmvp1.domain.entity.AppUser;
import com.ai.applications.rag.ragmvp1.domain.dto.RetrievalResult;
import com.ai.applications.rag.ragmvp1.domain.dto.SearchResponse;
import com.ai.applications.rag.ragmvp1.domain.entity.SearchQueryLog;
import com.ai.applications.rag.ragmvp1.repository.AppUserRepository;
import com.ai.applications.rag.ragmvp1.repository.SearchQueryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    private final VectorStore vectorStore;
    private final SearchQueryLogRepository queryLogRepository;
    private final AppUserRepository userRepository;

    public RetrievalService(@Qualifier("applicationVectorStore") VectorStore vectorStore,
                            SearchQueryLogRepository queryLogRepository,
                            AppUserRepository userRepository) {
        this.vectorStore = vectorStore;
        this.queryLogRepository = queryLogRepository;
        this.userRepository = userRepository;
    }

    public SearchResponse search(String queryText, Integer topKParam, Authentication authentication) {
        if (queryText == null || queryText.isBlank()) {
            return new SearchResponse(null, List.of());
        }

        int topK = (topKParam == null || topKParam < 1) ? DEFAULT_TOP_K : Math.min(topKParam, MAX_TOP_K);
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        long startMs = System.currentTimeMillis();
        List<Document> rawResults = executeSearch(queryText, topK, isAdmin, username);
        long latencyMs = System.currentTimeMillis() - startMs;

        List<RetrievalResult> results = rawResults.stream()
                .map(this::toRetrievalResult)
                .toList();

        UUID queryLogId = persistQueryLog(queryText, username, latencyMs, results);
        return new SearchResponse(queryLogId, results);
    }

    public void recordFeedback(UUID queryLogId, boolean positive) {
        if (queryLogId == null) {
            return;
        }
        queryLogRepository.findById(queryLogId).ifPresent(entry -> {
            entry.setFeedbackPositive(positive);
            queryLogRepository.save(entry);
        });
    }

    private List<Document> executeSearch(String queryText, int topK, boolean isAdmin, String username) {
        try {
            SearchRequest request = buildSearchRequest(queryText, topK, isAdmin, username);
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.error("Vector search failed for query='{}' user='{}'", queryText, username, e);
            return List.of();
        }
    }

    private SearchRequest buildSearchRequest(String queryText, int topK, boolean isAdmin, String username) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(queryText)
                .topK(topK)
                .similarityThreshold(0.0);

        if (!isAdmin) {
            AppUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            // Filter: user's own documents (owner_user_id matches)
            // Crawler content is public (no filter applied, searchable by all users)
            Filter.Expression ownerFilter = b.eq("owner_user_id", user.getId().toString()).build();
            // Only filter out documents not owned by user; crawler content (source_type="crawler") has no owner_user_id
            builder.filterExpression(ownerFilter);
        }

        return builder.build();
    }

    private RetrievalResult toRetrievalResult(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        String documentId = String.valueOf(meta.getOrDefault("document_id", ""));
        String sourceName = String.valueOf(meta.getOrDefault("source_name", "Unknown"));
        Object chunkObj = meta.get("chunk_number");
        int chunkNumber = chunkObj instanceof Number n ? n.intValue() : 0;
        String sourceType = String.valueOf(meta.getOrDefault("source_type", "document"));
        String sourceUrl = meta.containsKey("source_url") ? String.valueOf(meta.get("source_url")) : null;
        String sourceTitle = meta.containsKey("source_title") ? String.valueOf(meta.get("source_title")) : null;
        Double score = doc.getScore();
        return new RetrievalResult(doc.getText(), score != null ? score : 0.0, documentId, sourceName, chunkNumber, sourceType, sourceUrl, sourceTitle);
    }

    private UUID persistQueryLog(String query, String username, long latencyMs, List<RetrievalResult> results) {
        Double topScore = results.isEmpty() ? null : results.get(0).score();
        Double avgScore = results.isEmpty() ? null
                : results.stream().mapToDouble(RetrievalResult::score).average().orElse(0.0);

        SearchQueryLog entry = new SearchQueryLog(query, username, latencyMs, results.size(), topScore, avgScore);
        return queryLogRepository.save(entry).getId();
    }
}
