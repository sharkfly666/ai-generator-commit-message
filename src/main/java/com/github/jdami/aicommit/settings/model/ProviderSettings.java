package com.github.jdami.aicommit.settings.model;

/**
 * Wrapper for provider-specific configuration.
 */
public class ProviderSettings {
    public OllamaConfig ollama = new OllamaConfig();
    public OpenAiConfig openAi = new OpenAiConfig();
}
