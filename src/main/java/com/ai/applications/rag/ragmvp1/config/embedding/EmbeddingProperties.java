package com.ai.applications.rag.ragmvp1.config.embedding;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Switch providers by setting app.embedding.provider.
 * WARNING: changing providers requires dropping and recreating the vector_store
 * table because dimension counts differ between providers.
 */
@Validated
@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingProperties(
        // hash | openai | gemini | ollama  (default: hash)
        String provider,
        OpenAiProps openai,
        GeminiProps gemini,
        OllamaProps ollama
) {

    public String provider() {
        return provider != null ? provider : "hash";
    }

    public record OpenAiProps(
            @NotBlank String apiKey,
            String model,     // default: text-embedding-3-small
            Integer dimensions // null = use model default (1536 for 3-small)
    ) {
        public String model() { return model != null ? model : "text-embedding-3-small"; }
    }

    public record GeminiProps(
            @NotBlank String projectId,
            String location,  // default: us-central1
            String model      // default: text-embedding-004
    ) {
        public String location() { return location != null ? location : "us-central1"; }
        public String model()    { return model    != null ? model    : "text-embedding-004"; }
    }

    public record OllamaProps(
            String baseUrl,   // default: http://localhost:11434
            String model      // default: nomic-embed-text
    ) {
        public String baseUrl() { return baseUrl != null ? baseUrl : "http://localhost:11434"; }
        public String model()   { return model   != null ? model   : "nomic-embed-text"; }
    }
}
