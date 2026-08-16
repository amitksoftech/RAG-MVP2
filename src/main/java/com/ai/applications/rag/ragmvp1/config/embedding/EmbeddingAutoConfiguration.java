package com.ai.applications.rag.ragmvp1.config.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Strategy pattern via Spring conditional beans.
 * Exactly one EmbeddingModel bean is created based on app.embedding.provider.
 * To add a new provider: add one @Bean method below + a matching properties record.
 */
@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingAutoConfiguration.class);

    // ── Hash (default, no external dependencies) ─────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.embedding.provider",
                           havingValue = "hash", matchIfMissing = true)
    public EmbeddingModel hashEmbeddingModel() {
        log.info("[Embedding] Provider: hash (8-dim, dev/test only)");
        return new HashEmbeddingModel();
    }

    // ── OpenAI ────────────────────────────────────────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "openai")
    @ConditionalOnClass(name = "org.springframework.ai.openai.OpenAiEmbeddingModel")
    public EmbeddingModel openAiEmbeddingModel(EmbeddingProperties props) {
        EmbeddingProperties.OpenAiProps p = props.openai();
        log.info("[Embedding] Provider: openai, model={}", p.model());
        try {
            var apiClass    = Class.forName("org.springframework.ai.openai.api.OpenAiApi");
            var optClass    = Class.forName("org.springframework.ai.openai.OpenAiEmbeddingOptions");
            var modelClass  = Class.forName("org.springframework.ai.openai.OpenAiEmbeddingModel");
            var metaClass   = Class.forName("org.springframework.ai.document.MetadataMode");

            Object api  = apiClass.getConstructor(String.class).newInstance(p.apiKey());
            Object meta = java.util.Arrays.stream(metaClass.getEnumConstants())
                    .filter(e -> e.toString().equals("ALL")).findFirst().orElseThrow();

            var builder = optClass.getMethod("builder").invoke(null);
            optClass.getMethod("model", String.class).invoke(builder, p.model());
            if (p.dimensions() != null) {
                optClass.getMethod("dimensions", Integer.class).invoke(builder, p.dimensions());
            }
            Object opts = optClass.getMethod("build").invoke(builder);

            return (EmbeddingModel) modelClass
                    .getConstructor(apiClass, metaClass, optClass)
                    .newInstance(api, meta, opts);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create OpenAI EmbeddingModel. " +
                    "Add spring-ai-openai to pom.xml.", e);
        }
    }

    // ── Gemini (Vertex AI text-embedding-004) ─────────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "gemini")
    @ConditionalOnClass(name = "org.springframework.ai.vertexai.embedding.VertexAiTextEmbeddingModel")
    public EmbeddingModel geminiEmbeddingModel(EmbeddingProperties props) {
        EmbeddingProperties.GeminiProps p = props.gemini();
        log.info("[Embedding] Provider: gemini, model={}, project={}", p.model(), p.projectId());
        try {
            var connClass   = Class.forName("org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails");
            var optsClass   = Class.forName("org.springframework.ai.vertexai.embedding.VertexAiTextEmbeddingOptions");
            var modelClass  = Class.forName("org.springframework.ai.vertexai.embedding.VertexAiTextEmbeddingModel");

            var connBuilder = connClass.getMethod("builder").invoke(null);
            connClass.getMethod("projectId", String.class).invoke(connBuilder, p.projectId());
            connClass.getMethod("location",  String.class).invoke(connBuilder, p.location());
            Object conn = connClass.getMethod("build").invoke(connBuilder);

            var optsBuilder = optsClass.getMethod("builder").invoke(null);
            optsClass.getMethod("model", String.class).invoke(optsBuilder, p.model());
            Object opts = optsClass.getMethod("build").invoke(optsBuilder);

            return (EmbeddingModel) modelClass.getConstructor(connClass, optsClass).newInstance(conn, opts);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Gemini EmbeddingModel. " +
                    "Add spring-ai-vertex-ai-embedding to pom.xml and set GOOGLE_APPLICATION_CREDENTIALS.", e);
        }
    }

    // ── Ollama (local) ────────────────────────────────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "ollama")
    @ConditionalOnClass(name = "org.springframework.ai.ollama.OllamaEmbeddingModel")
    public EmbeddingModel ollamaEmbeddingModel(EmbeddingProperties props) {
        EmbeddingProperties.OllamaProps p = props.ollama();
        log.info("[Embedding] Provider: ollama, model={}, url={}", p.model(), p.baseUrl());
        try {
            var apiClass    = Class.forName("org.springframework.ai.ollama.api.OllamaApi");
            var optsClass   = Class.forName("org.springframework.ai.ollama.OllamaOptions");
            var modelClass  = Class.forName("org.springframework.ai.ollama.OllamaEmbeddingModel");

            Object api  = apiClass.getConstructor(String.class).newInstance(p.baseUrl());
            var builder = optsClass.getMethod("builder").invoke(null);
            optsClass.getMethod("model", String.class).invoke(builder, p.model());
            Object opts = optsClass.getMethod("build").invoke(builder);

            return (EmbeddingModel) modelClass.getConstructor(apiClass, optsClass).newInstance(api, opts);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Ollama EmbeddingModel. " +
                    "Add spring-ai-ollama to pom.xml and start Ollama.", e);
        }
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel fallbackEmbeddingModel() {
        log.warn("[Embedding] No provider matched '{}', falling back to hash", "unknown");
        return new HashEmbeddingModel();
    }
}
