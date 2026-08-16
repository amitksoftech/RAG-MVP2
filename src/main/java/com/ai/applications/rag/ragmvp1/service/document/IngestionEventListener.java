package com.ai.applications.rag.ragmvp1.service.document;

import com.ai.applications.rag.ragmvp1.domain.event.DocumentUploadedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IngestionEventListener {

    private final IngestionProcessor ingestionProcessor;

    public IngestionEventListener(IngestionProcessor ingestionProcessor) {
        this.ingestionProcessor = ingestionProcessor;
    }

    @Async("ingestionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(DocumentUploadedEvent event) {
        ingestionProcessor.process(event.documentId());
    }
}