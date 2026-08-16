package com.ai.applications.rag.ragmvp1.config.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Switch LLM providers by setting app.llm.provider.
 * Use app.llm.enabled=false to disable the LLM bean entirely
 * (useful when the app runs in retrieval-only mode).
 */
@Validated
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        // none | openai | gemini | ollama  (default: none — retrieval-only mode)
        String provider,
        boolean enabled,
        OpenAiProps openai,
        GeminiProps gemini,
        OllamaProps ollama
) {

    public String provider() { return provider != null ? provider : "none"; }
    public boolean enabled() { return enabled; }

    public record OpenAiProps(
            String apiKey,
            String model,       // default: gpt-4o-mini
            Double temperature
    ) {
        public String model()       { return model       != null ? model       : "gpt-4o-mini"; }
        public Double temperature() { return temperature != null ? temperature : 0.7; }
    }

    public record GeminiProps(
            String projectId,
            String location,    // default: us-central1
            String model        // default: gemini-2.0-flash
    ) {
        public String location() { return location != null ? location : "us-central1"; }
        public String model()    { return model    != null ? model    : "gemini-2.0-flash"; }
    }

    public record OllamaProps(
            String baseUrl,     // default: http://localhost:11434
            String model        // default: llama3.2
    ) {
        public String baseUrl() { return baseUrl != null ? baseUrl : "http://localhost:11434"; }
        public String model()   { return model   != null ? model   : "llama3.2"; }
    }
}
