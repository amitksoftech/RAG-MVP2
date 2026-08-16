package com.ai.applications.rag.ragmvp1.domain.event;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId) {
}