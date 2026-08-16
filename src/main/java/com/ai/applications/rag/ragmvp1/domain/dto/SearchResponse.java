package com.ai.applications.rag.ragmvp1.domain.dto;

import java.util.List;
import java.util.UUID;

/**
 * Wraps the list of retrieval results together with the persisted query log ID,
 * which is needed to record user feedback.
 */
public record SearchResponse(
        UUID queryLogId,
        List<RetrievalResult> results
) {
}
