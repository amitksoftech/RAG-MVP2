package com.ai.applications.rag.ragmvp1.service.document;

import com.ai.applications.rag.ragmvp1.domain.entity.KnowledgeDocument;
import com.ai.applications.rag.ragmvp1.repository.KnowledgeDocumentRepository;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionProcessor {

    private final KnowledgeDocumentRepository documentRepository;
    private final EmbeddingIndexer embeddingIndexer;

    public IngestionProcessor(KnowledgeDocumentRepository documentRepository, EmbeddingIndexer embeddingIndexer) {
        this.documentRepository = documentRepository;
        this.embeddingIndexer = embeddingIndexer;
    }

    @Transactional
    public void process(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        try {
            document.markProcessing();
            documentRepository.save(document);

            String extractedText = extractText(Path.of(document.getStoragePath()));
            if (extractedText == null || extractedText.isBlank()) {
                throw new IllegalStateException("No readable text found in the uploaded document.");
            }

            List<String> chunks = splitIntoChunks(extractedText);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("The uploaded file did not produce any usable text chunks.");
            }

            embeddingIndexer.index(document, chunks);
            document.markReady(chunks.size());
            documentRepository.save(document);
        } catch (Exception ex) {
            String failure = ex.getMessage() == null ? "Unknown ingestion error" : ex.getMessage();
            document.markFailed(failure);
            documentRepository.save(document);
        }
    }

    private String extractText(Path filePath) {
        try {
            Tika tika = new Tika();
            return tika.parseToString(filePath.toFile());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read and parse the uploaded file: " + filePath.getFileName(), ex);
        }
    }

    private List<String> splitIntoChunks(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] sentences = normalized.split("(?<=[.!?])\\s+|\\n+");
        List<String> chunks = new ArrayList<>();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isBlank()) {
                chunks.add(trimmed);
            }
        }
        return chunks.isEmpty() ? List.of(normalized.trim()) : chunks;
    }
}