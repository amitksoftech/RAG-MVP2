package com.ai.applications.rag.ragmvp1.service.document;

import com.ai.applications.rag.ragmvp1.domain.entity.KnowledgeDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingIndexer {

    private final VectorStore vectorStore;

    public EmbeddingIndexer(@Qualifier("applicationVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void index(KnowledgeDocument source, List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<Document> vectorDocuments = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            vectorDocuments.add(toVectorDocument(source, chunks.get(index), index));
        }

        vectorStore.add(vectorDocuments);
    }

    private Document toVectorDocument(KnowledgeDocument source, String chunkText, int chunkNumber) {
        Document document = new Document(chunkText);
        document.getMetadata().put("document_id", source.getId().toString());
        document.getMetadata().put("owner_user_id", source.getOwner().getId().toString());
        document.getMetadata().put("source_name", source.getOriginalFilename());
        document.getMetadata().put("chunk_number", chunkNumber);
        return document;
    }
}