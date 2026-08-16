package com.ai.applications.rag.ragmvp1.service.document;

import com.ai.applications.rag.ragmvp1.domain.entity.KnowledgeDocument;
import com.ai.applications.rag.ragmvp1.repository.KnowledgeDocumentRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.UUID;

@Service
public class AdminDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final VectorStore vectorStore;

    public AdminDocumentService(KnowledgeDocumentRepository documentRepository,
                               DocumentStorageService storageService,
                               @Qualifier("applicationVectorStore") VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.vectorStore = vectorStore;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDocument(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (document.getId() != null) {
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            vectorStore.delete(builder.eq("document_id", document.getId().toString()).build());
        }

        document.markDeleting();
        documentRepository.save(document);

        if (document.getStoragePath() != null && !document.getStoragePath().isBlank()) {
            storageService.delete(Path.of(document.getStoragePath()));
        }

        documentRepository.delete(document);
    }
}