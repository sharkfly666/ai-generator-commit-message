package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.settings.AiSettingsState;
import com.github.jdami.aicommit.settings.AiSettingsState.Provider;
import com.github.jdami.aicommit.settings.model.ProviderSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Configurable for AI provider settings UI.
 */
public class AiSettingsConfigurable implements Configurable {

    private AiSettingsComponent settingsComponent;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "AI Commit Message Generator";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingsComponent = new AiSettingsComponent();
        // Load current settings into UI
        reset();
        return settingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        if (settingsComponent == null) {
            return false;
        }
        
        AiSettingsState settings = AiSettingsState.getInstance();
        
        Provider currentProvider = settingsComponent.getProvider();
        String currentEndpoint = settingsComponent.getOllamaEndpoint();
        String currentOllamaModel = settingsComponent.getOllamaModel();
        String currentOpenAiEndpoint = settingsComponent.getOpenAiEndpoint();
        String currentOpenAiModel = settingsComponent.getOpenAiModel();
        String currentOpenAiApiKey = settingsComponent.getOpenAiApiKey();
        int currentTimeout = settingsComponent.getTimeout();
        String currentSystemPrompt = settingsComponent.getSystemPrompt();
        
        return currentProvider != settings.provider
                || !currentEndpoint.equals(settings.providers.ollama.endpoint)
                || !currentOllamaModel.equals(settings.providers.ollama.model)
                || !currentOpenAiEndpoint.equals(settings.providers.openAi.endpoint)
                || !currentOpenAiModel.equals(settings.providers.openAi.model)
                || !currentOpenAiApiKey.equals(settings.providers.openAi.apiKey)
                || currentTimeout != settings.timeout
                || !currentSystemPrompt.equals(settings.systemPrompt);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (settingsComponent == null) {
            return;
        }
        
        AiSettingsState settings = AiSettingsState.getInstance();
        
        // Validate inputs
        Provider provider = settingsComponent.getProvider();
        String endpoint = settingsComponent.getOllamaEndpoint().trim();
        String ollamaModel = settingsComponent.getOllamaModel().trim();
        String systemPrompt = settingsComponent.getSystemPrompt().trim();
        String openAiEndpoint = settingsComponent.getOpenAiEndpoint().trim();
        String openAiModel = settingsComponent.getOpenAiModel().trim();
        String openAiApiKey = settingsComponent.getOpenAiApiKey().trim();
        
        if (systemPrompt.isEmpty()) {
            throw new ConfigurationException("System prompt cannot be empty");
        }
        if (provider == Provider.OLLAMA) {
            if (endpoint.isEmpty()) {
                throw new ConfigurationException("Ollama endpoint cannot be empty");
            }
            if (ollamaModel.isEmpty()) {
                throw new ConfigurationException("Ollama model cannot be empty");
            }
        } else if (provider == Provider.OPENAI) {
            if (openAiEndpoint.isEmpty()) {
                throw new ConfigurationException("OpenAI API base cannot be empty");
            }
            if (openAiModel.isEmpty()) {
                throw new ConfigurationException("OpenAI model cannot be empty");
            }
            if (openAiApiKey.isEmpty()) {
                throw new ConfigurationException("OpenAI API key cannot be empty");
            }
        }
        
        // Apply settings
        settings.provider = provider != null ? provider : Provider.OLLAMA;
        ProviderSettings providers = settings.providers != null ? settings.providers : new ProviderSettings();
        providers.ollama.endpoint = endpoint;
        providers.ollama.model = ollamaModel;
        providers.openAi.endpoint = openAiEndpoint;
        providers.openAi.model = openAiModel;
        providers.openAi.apiKey = openAiApiKey;
        settings.providers = providers;
        // legacy fields
        settings.ollamaEndpoint = endpoint;
        settings.ollamaModel = ollamaModel;
        settings.modelName = ollamaModel;
        settings.openAiEndpoint = openAiEndpoint;
        settings.openAiModel = openAiModel;
        settings.openAiApiKey = openAiApiKey;
        settings.timeout = settingsComponent.getTimeout();
        settings.systemPrompt = systemPrompt;
        
        // Force state persistence
        settings.loadState(settings);
    }

    @Override
    public void reset() {
        if (settingsComponent == null) {
            return;
        }
        
        AiSettingsState settings = AiSettingsState.getInstance();
        settingsComponent.setProvider(settings.provider != null ? settings.provider : Provider.OLLAMA);
        ProviderSettings providers = settings.providers != null ? settings.providers : new ProviderSettings();
        settingsComponent.setOllamaEndpoint(providers.ollama != null && providers.ollama.endpoint != null
                ? providers.ollama.endpoint : "http://localhost:11434");
        settingsComponent.setOllamaModel(providers.ollama != null && providers.ollama.model != null
                ? providers.ollama.model : "qwen3:8b");
        settingsComponent.setOpenAiEndpoint(providers.openAi != null && providers.openAi.endpoint != null
                ? providers.openAi.endpoint : "https://api.openai.com");
        settingsComponent.setOpenAiModel(providers.openAi != null && providers.openAi.model != null
                ? providers.openAi.model : "gpt-4o-mini");
        settingsComponent.setOpenAiApiKey(providers.openAi != null && providers.openAi.apiKey != null
                ? providers.openAi.apiKey : "");
        settingsComponent.setTimeout(settings.timeout);
        settingsComponent.setSystemPrompt(settings.systemPrompt != null ? settings.systemPrompt : getDefaultSystemPrompt());
    }
    
    private String getDefaultSystemPrompt() {
        // Create a new instance to get the default value
        AiSettingsState defaultState = new AiSettingsState();
        return defaultState.systemPrompt;
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }
}
