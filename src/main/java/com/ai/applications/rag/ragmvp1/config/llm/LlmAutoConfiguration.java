package com.ai.applications.rag.ragmvp1.config.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Strategy pattern for LLM/chat providers.
 * Exactly one ChatModel bean is created when app.llm.enabled=true.
 * Inject ChatModel wherever answer generation is needed.
 * When enabled=false (retrieval-only mode) no bean is created — injecting
 * ChatModel with @Autowired(required=false) is the recommended pattern.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LlmAutoConfiguration.class);

    // ── OpenAI ────────────────────────────────────────────────────────────────

    @Bean("chatModel")
    @ConditionalOnExpression("'${app.llm.enabled:false}' == 'true' && '${app.llm.provider:none}' == 'openai'")
    @ConditionalOnClass(name = "org.springframework.ai.openai.OpenAiChatModel")
    public ChatModel openAiChatModel(LlmProperties props) {
        LlmProperties.OpenAiProps p = props.openai();
        log.info("[LLM] Provider: openai, model={}", p.model());
        try {
            var apiClass    = Class.forName("org.springframework.ai.openai.api.OpenAiApi");
            var optsClass   = Class.forName("org.springframework.ai.openai.OpenAiChatOptions");
            var modelClass  = Class.forName("org.springframework.ai.openai.OpenAiChatModel");

            Object api      = apiClass.getConstructor(String.class).newInstance(p.apiKey());
            var builder     = optsClass.getMethod("builder").invoke(null);
            optsClass.getMethod("model",       String.class).invoke(builder, p.model());
            optsClass.getMethod("temperature", Double.class).invoke(builder, p.temperature());
            Object opts     = optsClass.getMethod("build").invoke(builder);

            return (ChatModel) modelClass.getConstructor(apiClass, optsClass).newInstance(api, opts);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create OpenAI ChatModel.", e);
        }
    }

    // ── Gemini (Vertex AI) ────────────────────────────────────────────────────
    // Requires spring-ai-vertex-ai-gemini dependency (add to pom.xml when needed).
    // @Bean("chatModel")
    // @ConditionalOnExpression("'${app.llm.enabled:false}' == 'true' && '${app.llm.provider:none}' == 'gemini'")
    // @ConditionalOnClass(name = "org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel")
    // public ChatModel geminiChatModel(LlmProperties props) { ... }

    // ── Ollama ────────────────────────────────────────────────────────────────

    @Bean("chatModel")
    @ConditionalOnExpression("'${app.llm.enabled:false}' == 'true' && '${app.llm.provider:none}' == 'ollama'")
    @ConditionalOnClass(name = "org.springframework.ai.ollama.OllamaChatModel")
    public ChatModel ollamaChatModel(LlmProperties props) {
        LlmProperties.OllamaProps p = props.ollama();
        log.info("[LLM] Provider: ollama, model={}, url={}", p.model(), p.baseUrl());
        try {
            var apiClass    = Class.forName("org.springframework.ai.ollama.api.OllamaApi");
            var optsClass   = Class.forName("org.springframework.ai.ollama.OllamaOptions");
            var modelClass  = Class.forName("org.springframework.ai.ollama.OllamaChatModel");

            Object api      = apiClass.getConstructor(String.class).newInstance(p.baseUrl());
            var builder     = optsClass.getMethod("builder").invoke(null);
            optsClass.getMethod("model", String.class).invoke(builder, p.model());
            Object opts     = optsClass.getMethod("build").invoke(builder);

            return (ChatModel) modelClass.getConstructor(apiClass, optsClass).newInstance(api, opts);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Ollama ChatModel. " +
                    "Add spring-ai-ollama to pom.xml and start Ollama.", e);
        }
    }
}
