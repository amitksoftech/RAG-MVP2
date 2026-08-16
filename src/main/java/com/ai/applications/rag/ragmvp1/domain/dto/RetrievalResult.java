package com.ai.applications.rag.ragmvp1.domain.dto;

/**
 * A single chunk returned from a vector similarity search.
 */
public record RetrievalResult(
        String text,
        double score,
        String documentId,
        String sourceName,
        int chunkNumber,
        String sourceType,      // "document" or "crawler"
        String sourceUrl,       // URL for crawler content, null for documents
        String sourceTitle      // Page title for crawler content
) {
}
