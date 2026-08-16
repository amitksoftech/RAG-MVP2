package com.ai.applications.rag.ragmvp1.config.embedding;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

/** Simple deterministic hash embedding — zero external dependencies, for dev/testing. */
public class HashEmbeddingModel implements EmbeddingModel {

    private static final int DIMS = 8;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = request.getInstructions().stream()
                .map(text -> new Embedding(embed(text), 0))
                .toList();
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(String text) {
        float[] v = new float[DIMS];
        for (int i = 0; i < text.length(); i++) {
            v[i % DIMS] += (float) text.charAt(i) / 31.0f;
        }
        return v;
    }

    @Override
    public float[] embed(org.springframework.ai.document.Document document) {
        return embed(document.getText());
    }

    @Override
    public int dimensions() {
        return DIMS;
    }
}
