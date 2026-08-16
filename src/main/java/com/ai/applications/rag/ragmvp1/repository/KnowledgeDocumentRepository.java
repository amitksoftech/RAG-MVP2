package com.ai.applications.rag.ragmvp1.repository;

import com.ai.applications.rag.ragmvp1.domain.entity.DocumentStatus;
import com.ai.applications.rag.ragmvp1.domain.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    @EntityGraph(attributePaths = "owner")
    List<KnowledgeDocument> findByOwnerUsernameOrderByUploadedAtDesc(String username);

    @EntityGraph(attributePaths = "owner")
    List<KnowledgeDocument> findByOwnerIdOrderByUploadedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = "owner")
    List<KnowledgeDocument> findAllByOrderByUploadedAtDesc();

    long countByStatus(DocumentStatus status);
}
