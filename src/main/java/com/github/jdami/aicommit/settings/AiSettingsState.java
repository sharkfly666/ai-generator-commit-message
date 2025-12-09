package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.settings.model.OllamaConfig;
import com.github.jdami.aicommit.settings.model.OpenAiConfig;
import com.github.jdami.aicommit.settings.model.ProviderSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent state for Ollama settings
 */
@State(name = "com.github.jdami.aicommit.settings.AiSettingsState", storages = @Storage("OllamaSettings.xml"))
public class AiSettingsState implements PersistentStateComponent<AiSettingsState> {

    public enum Provider {
        OLLAMA,
        OPENAI
    }

    public Provider provider = Provider.OLLAMA;

    public ProviderSettings providers = new ProviderSettings();

    // Deprecated legacy fields kept for migration compatibility
    @Deprecated
    public String ollamaEndpoint = "http://localhost:11434";
    @Deprecated
    public String ollamaModel = "qwen3:8b";
    @Deprecated
    public String modelName = "qwen3:8b";
    @Deprecated
    public String openAiEndpoint = "https://api.openai.com";
    @Deprecated
    public String openAiApiKey = "";
    @Deprecated
    public String openAiModel = "gpt-4o-mini";

    public int timeout = 30;
    public String systemPrompt = "CRITICAL: You are a commit message generator. You MUST output ONLY the commit message in the exact format below. NO explanations, NO analysis, NO extra text, NO markdown.\n\n"
            +
            "MANDATORY OUTPUT FORMAT:\n" +
            "type(scope): <SUMMARY OF ALL CHANGES>\n" +
            "\n" +
            "- detailed change point 1\n" +
            "- detailed change point 2\n" +
            "- detailed change point 3\n\n" +
            "ABSOLUTE RULES - VIOLATION WILL FAIL:\n" +
            "1. Output ONLY the commit message - NOTHING ELSE\n" +
            "2. The first line description MUST be a concise summary of ALL changes\n" +
            "3. NO explanations like 'Based on the git diff' or 'Here's the analysis'\n" +
            "4. NO markdown (```), NO code blocks, NO formatting\n" +
            "5. NO sentences like 'This message:', 'You can use this', etc.\n" +
            "6. Use Chinese for descriptions\n" +
            "7. type: feat, fix, docs, style, refactor, test, chore\n" +
            "8. Start immediately with the commit message\n\n" +
            "CORRECT OUTPUT:\n" +
            "fix(weather): 修复城市搜索接口日志输出错误\n" +
            "\n" +
            "- 修正了城市搜索接口返回结果的日志输出格式\n" +
            "- 移除了日志中的无效字符\n" +
            "- 确保日志能正确显示响应内容\n\n" +
            "FORBIDDEN OUTPUTS (NEVER DO THIS):\n" +
            "- 'Based on the git diff...'\n" +
            "- 'Here's the analysis...'\n" +
            "- 'This message:'\n" +
            "- Any explanation or commentary\n" +
            "- Markdown formatting\n\n" +
            "START YOUR RESPONSE IMMEDIATELY WITH: type(scope):";

    public String pluginVersion = "";

    public static AiSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AiSettingsState.class);
    }

    @Nullable
    @Override
    public AiSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AiSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
        migrateLegacyFields();
        ensureDefaults();
    }

    /**
     * Reset settings to default values
     */
    public void resetToDefaults() {
        this.provider = Provider.OLLAMA;
        this.providers = new ProviderSettings();
        // keep legacy defaults for compatibility
        this.ollamaEndpoint = this.providers.ollama.endpoint;
        this.ollamaModel = this.providers.ollama.model;
        this.modelName = this.providers.ollama.model;
        this.openAiEndpoint = this.providers.openAi.endpoint;
        this.openAiApiKey = this.providers.openAi.apiKey;
        this.openAiModel = this.providers.openAi.model;
        this.timeout = 30;
        this.systemPrompt = "CRITICAL: You are a commit message generator. You MUST output ONLY the commit message in the exact format below. NO explanations, NO analysis, NO extra text, NO markdown.\n\n"
                +
                "MANDATORY OUTPUT FORMAT:\n" +
                "type(scope): <SUMMARY OF ALL CHANGES>\n" +
                "\n" +
                "- detailed change point 1\n" +
                "- detailed change point 2\n" +
                "- detailed change point 3\n\n" +
                "ABSOLUTE RULES - VIOLATION WILL FAIL:\n" +
                "1. Output ONLY the commit message - NOTHING ELSE\n" +
                "2. The first line description MUST be a concise summary of ALL changes\n" +
                "3. NO explanations like 'Based on the git diff' or 'Here's the analysis'\n" +
                "4. NO markdown (```), NO code blocks, NO formatting\n" +
                "5. NO sentences like 'This message:', 'You can use this', etc.\n" +
                "6. Use Chinese for descriptions\n" +
                "7. type: feat, fix, docs, style, refactor, test, chore\n" +
                "8. Start immediately with the commit message\n\n" +
                "CORRECT OUTPUT:\n" +
                "fix(weather): 修复城市搜索接口日志输出错误\n" +
                "\n" +
                "- 修正了城市搜索接口返回结果的日志输出格式\n" +
                "- 移除了日志中的无效字符\n" +
                "- 确保日志能正确显示响应内容\n\n" +
                "FORBIDDEN OUTPUTS (NEVER DO THIS):\n" +
                "- 'Based on the git diff...'\n" +
                "- 'Here's the analysis...'\n" +
                "- 'This message:'\n" +
                "- Any explanation or commentary\n" +
                "- Markdown formatting\n\n" +
                "START YOUR RESPONSE IMMEDIATELY WITH: type(scope):";
    }

    private void migrateLegacyFields() {
        if (providers == null) {
            providers = new ProviderSettings();
        }
        if (providers.ollama == null) {
            providers.ollama = new OllamaConfig();
        }
        if (providers.openAi == null) {
            providers.openAi = new OpenAiConfig();
        }

        if (ollamaEndpoint != null && !ollamaEndpoint.isEmpty()) {
            providers.ollama.endpoint = ollamaEndpoint;
        }
        if (ollamaModel != null && !ollamaModel.isEmpty()) {
            providers.ollama.model = ollamaModel;
        } else if (modelName != null && !modelName.isEmpty()) {
            providers.ollama.model = modelName;
        }

        if (openAiEndpoint != null && !openAiEndpoint.isEmpty()) {
            providers.openAi.endpoint = openAiEndpoint;
        }
        if (openAiModel != null && !openAiModel.isEmpty()) {
            providers.openAi.model = openAiModel;
        }
        if (openAiApiKey != null && !openAiApiKey.isEmpty()) {
            providers.openAi.apiKey = openAiApiKey;
        }
    }

    private void ensureDefaults() {
        if (providers.ollama.endpoint == null || providers.ollama.endpoint.isEmpty()) {
            providers.ollama.endpoint = "http://localhost:11434";
        }
        if (providers.ollama.model == null || providers.ollama.model.isEmpty()) {
            providers.ollama.model = "qwen3:8b";
        }
        if (providers.openAi.endpoint == null || providers.openAi.endpoint.isEmpty()) {
            providers.openAi.endpoint = "https://api.openai.com";
        }
        if (providers.openAi.model == null || providers.openAi.model.isEmpty()) {
            providers.openAi.model = "gpt-4o-mini";
        }
        if (providers.openAi.apiKey == null) {
            providers.openAi.apiKey = "";
        }
    }
}
