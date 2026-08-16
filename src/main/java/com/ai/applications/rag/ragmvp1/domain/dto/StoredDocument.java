package com.ai.applications.rag.ragmvp1.domain.dto;

import java.nio.file.Path;

public record StoredDocument(
        String storedFilename,
        Path path,
        String checksum
) {
}
