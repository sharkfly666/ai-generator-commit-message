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
        String currentOpenRouterEndpoint = settingsComponent.getOpenRouterEndpoint();
        String currentOpenRouterModel = settingsComponent.getOpenRouterModel();
        String currentOpenRouterApiKey = settingsComponent.getOpenRouterApiKey();
        int currentTimeout = settingsComponent.getTimeout();
        int currentMaxDiffChars = settingsComponent.getMaxDiffChars();
        String currentSystemPrompt = settingsComponent.getSystemPrompt();
        boolean currentProxyEnabled = settingsComponent.isProxyEnabled();
        String currentProxyHost = settingsComponent.getProxyHost().trim();
        int currentProxyPort = settingsComponent.getProxyPort();
        
        return currentProvider != settings.provider
                || !currentEndpoint.equals(settings.providers.ollama.endpoint)
                || !currentOllamaModel.equals(settings.providers.ollama.model)
                || !currentOpenAiEndpoint.equals(settings.providers.openAi.endpoint)
                || !currentOpenAiModel.equals(settings.providers.openAi.model)
                || !currentOpenAiApiKey.equals(settings.providers.openAi.apiKey)
                || !currentOpenRouterEndpoint.equals(settings.providers.openRouter.endpoint)
                || !currentOpenRouterModel.equals(settings.providers.openRouter.model)
                || !currentOpenRouterApiKey.equals(settings.providers.openRouter.apiKey)
                || currentTimeout != settings.timeout
                || currentMaxDiffChars != settings.maxDiffChars
                || !currentSystemPrompt.equals(settings.systemPrompt)
                || currentProxyEnabled != settings.proxyEnabled
                || !currentProxyHost.equals(settings.proxyHost != null ? settings.proxyHost : "")
                || currentProxyPort != settings.proxyPort;
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
        String openRouterEndpoint = settingsComponent.getOpenRouterEndpoint().trim();
        String openRouterModel = settingsComponent.getOpenRouterModel().trim();
        String openRouterApiKey = settingsComponent.getOpenRouterApiKey().trim();
        
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
        } else if (provider == Provider.OPENROUTER) {
            if (openRouterEndpoint.isEmpty()) {
                throw new ConfigurationException("OpenRouter API base cannot be empty");
            }
            if (openRouterModel.isEmpty()) {
                throw new ConfigurationException("OpenRouter model cannot be empty");
            }
            if (openRouterApiKey.isEmpty()) {
                throw new ConfigurationException("OpenRouter API key cannot be empty");
            }
        }

        boolean proxyEnabled = settingsComponent.isProxyEnabled();
        String proxyHost = settingsComponent.getProxyHost().trim();
        int proxyPort = settingsComponent.getProxyPort();
        if (proxyEnabled) {
            if (proxyHost.isEmpty()) {
                throw new ConfigurationException("Proxy host cannot be empty when proxy is enabled");
            }
            if (proxyHost.contains("://")) {
                throw new ConfigurationException("Proxy host must not include a scheme (use host only, e.g. 127.0.0.1)");
            }
            if (proxyHost.contains("/") || proxyHost.contains("?")) {
                throw new ConfigurationException("Proxy host is invalid: " + proxyHost);
            }
            if (proxyPort <= 0 || proxyPort > 65535) {
                throw new ConfigurationException("Proxy port must be between 1 and 65535");
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
        providers.openRouter.endpoint = openRouterEndpoint;
        providers.openRouter.model = openRouterModel;
        providers.openRouter.apiKey = openRouterApiKey;
        settings.providers = providers;
        // legacy fields
        settings.ollamaEndpoint = endpoint;
        settings.ollamaModel = ollamaModel;
        settings.modelName = ollamaModel;
        settings.openAiEndpoint = openAiEndpoint;
        settings.openAiModel = openAiModel;
        settings.openAiApiKey = openAiApiKey;
        settings.timeout = settingsComponent.getTimeout();
        settings.maxDiffChars = settingsComponent.getMaxDiffChars();
        settings.systemPrompt = systemPrompt;
        settings.proxyEnabled = proxyEnabled;
        settings.proxyHost = proxyHost;
        settings.proxyPort = proxyPort;
        
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
        settingsComponent.setOpenRouterEndpoint(providers.openRouter != null && providers.openRouter.endpoint != null
                ? providers.openRouter.endpoint : "https://openrouter.ai/api");
        settingsComponent.setOpenRouterModel(providers.openRouter != null && providers.openRouter.model != null
                ? providers.openRouter.model : "anthropic/claude-3.5-sonnet");
        settingsComponent.setOpenRouterApiKey(providers.openRouter != null && providers.openRouter.apiKey != null
                ? providers.openRouter.apiKey : "");
        settingsComponent.setTimeout(settings.timeout);
        settingsComponent.setMaxDiffChars(settings.maxDiffChars);
        settingsComponent.setSystemPrompt(settings.systemPrompt != null ? settings.systemPrompt : getDefaultSystemPrompt());
        settingsComponent.setProxyEnabled(settings.proxyEnabled);
        settingsComponent.setProxyHost(settings.proxyHost != null ? settings.proxyHost : "");
        settingsComponent.setProxyPort(settings.proxyPort > 0 ? settings.proxyPort : 7890);
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
