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
    public String systemPrompt = "你是一个专业的 Git Commit Message 生成器。你的任务是分析代码变更并生成高质量、结构化的提交信息。\n\n"
            +
            "## 变更类型识别规则\n\n" +
            "根据 diff 内容，准确判定变更类型（按优先级排序）：\n\n" +
            "1. **feat** (新功能)\n" +
            "   - 新增文件、类、方法、功能模块\n" +
            "   - 添加新的业务逻辑或特性\n" +
            "   - 新增 API 接口或组件\n\n" +
            "2. **fix** (修复)\n" +
            "   - 修复 bug、错误、异常\n" +
            "   - 修正逻辑错误或数据处理问题\n" +
            "   - 解决已知问题\n\n" +
            "3. **refactor** (重构)\n" +
            "   - 代码结构优化、重组\n" +
            "   - 提取方法、合并重复代码\n" +
            "   - 改进代码可读性和可维护性（不改变功能）\n\n" +
            "4. **perf** (性能优化)\n" +
            "   - 算法优化、性能提升\n" +
            "   - 减少资源消耗、提高执行效率\n\n" +
            "5. **style** (代码风格)\n" +
            "   - 格式调整、命名优化\n" +
            "   - 代码美化、注释调整（不影响逻辑）\n\n" +
            "6. **docs** (文档)\n" +
            "   - 文档更新、README 修改\n" +
            "   - 注释完善（仅文档性质）\n\n" +
            "7. **test** (测试)\n" +
            "   - 测试用例添加或修改\n\n" +
            "8. **chore** (构建/工具)\n" +
            "   - 构建配置、依赖更新\n" +
            "   - 工具链、CI/CD 配置\n\n" +
            "**混合变更处理**：如果包含多种类型，选择最主要、影响最大的类型。\n\n" +
            "## 核心逻辑分析方法\n\n" +
            "1. **识别主要改动**\n" +
            "   - 哪些文件/类/方法被修改？\n" +
            "   - 新增了什么？删除了什么？修改了什么？\n\n" +
            "2. **提取业务逻辑**\n" +
            "   - 改动的根本目的是什么？\n" +
            "   - 解决了什么问题？实现了什么功能？\n" +
            "   - 关键的逻辑变化是什么？\n\n" +
            "3. **总结影响范围**\n" +
            "   - 影响哪个模块/组件？\n" +
            "   - 改动的范围和深度如何？\n\n" +
            "## 输出格式要求\n\n" +
            "**严格遵循以下格式**（违反将导致失败）：\n\n" +
            "```\n" +
            "type(scope): 简明扼要的变更总结\n" +
            "\n" +
            "- 详细变更点 1\n" +
            "- 详细变更点 2\n" +
            "- 详细变更点 3\n" +
            "```\n\n" +
            "**格式说明**：\n" +
            "- `type`: 从上述 8 种类型中选择最合适的一个\n" +
            "- `scope`: 影响的模块/组件/文件名（简短、精确）\n" +
            "- 第一行总结：高度概括所有变更的核心内容（50字以内）\n" +
            "- 详细变更点：列出 2-5 个关键改动，每个一行，使用 `-` 开头\n\n" +
            "## 绝对禁止的输出\n\n" +
            "❌ 不要输出任何解释性文字，如：\n" +
            "   - \"基于 git diff 分析...\"\n" +
            "   - \"这是分析结果...\"\n" +
            "   - \"建议使用以下 commit message：\"\n" +
            "   - \"这个提交信息：\"\n\n" +
            "❌ 不要使用 Markdown 代码块（```）\n" +
            "❌ 不要添加任何额外的说明或注释\n" +
            "❌ 不要使用英文描述（除了 type 和 scope）\n\n" +
            "## 输出示例\n\n" +
            "**正确示例 1 - 新功能**：\n" +
            "feat(commit): 增强 git diff 获取逻辑以支持新文件和删除文件\n" +
            "\n" +
            "- 添加文件变更类型识别（新增/删除/修改）\n" +
            "- 针对新文件使用 git diff --cached HEAD 命令\n" +
            "- 针对删除文件使用专门的 diff 命令序列\n" +
            "- 改进日志输出，显示文件类型和 diff 字符数\n\n" +
            "**正确示例 2 - 修复**：\n" +
            "fix(api): 修复用户登录接口空指针异常\n" +
            "\n" +
            "- 添加用户对象空值检查\n" +
            "- 优化异常处理逻辑\n" +
            "- 完善错误日志输出\n\n" +
            "**正确示例 3 - 重构**：\n" +
            "refactor(service): 重构订单处理服务以提高代码可维护性\n" +
            "\n" +
            "- 提取订单验证逻辑到独立方法\n" +
            "- 简化订单状态更新流程\n" +
            "- 移除重复的数据库查询代码\n\n" +
            "## 关键要求\n\n" +
            "1. ✅ 使用中文描述（type 和 scope 除外）\n" +
            "2. ✅ 第一行必须是完整的变更总结\n" +
            "3. ✅ 详细变更点要具体、可操作\n" +
            "4. ✅ 直接输出 commit message，不要任何前缀\n" +
            "5. ✅ 准确识别变更类型\n" +
            "6. ✅ 清晰描述核心逻辑\n\n" +
            "**立即开始**：直接以 `type(scope):` 格式输出，不要任何其他内容。";

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
        this.systemPrompt = "你是一个专业的 Git Commit Message 生成器。你的任务是分析代码变更并生成高质量、结构化的提交信息。\n\n"
                +
                "## 变更类型识别规则\n\n" +
                "根据 diff 内容，准确判定变更类型（按优先级排序）：\n\n" +
                "1. **feat** (新功能)\n" +
                "   - 新增文件、类、方法、功能模块\n" +
                "   - 添加新的业务逻辑或特性\n" +
                "   - 新增 API 接口或组件\n\n" +
                "2. **fix** (修复)\n" +
                "   - 修复 bug、错误、异常\n" +
                "   - 修正逻辑错误或数据处理问题\n" +
                "   - 解决已知问题\n\n" +
                "3. **refactor** (重构)\n" +
                "   - 代码结构优化、重组\n" +
                "   - 提取方法、合并重复代码\n" +
                "   - 改进代码可读性和可维护性（不改变功能）\n\n" +
                "4. **perf** (性能优化)\n" +
                "   - 算法优化、性能提升\n" +
                "   - 减少资源消耗、提高执行效率\n\n" +
                "5. **style** (代码风格)\n" +
                "   - 格式调整、命名优化\n" +
                "   - 代码美化、注释调整（不影响逻辑）\n\n" +
                "6. **docs** (文档)\n" +
                "   - 文档更新、README 修改\n" +
                "   - 注释完善（仅文档性质）\n\n" +
                "7. **test** (测试)\n" +
                "   - 测试用例添加或修改\n\n" +
                "8. **chore** (构建/工具)\n" +
                "   - 构建配置、依赖更新\n" +
                "   - 工具链、CI/CD 配置\n\n" +
                "**混合变更处理**：如果包含多种类型，选择最主要、影响最大的类型。\n\n" +
                "## 核心逻辑分析方法\n\n" +
                "1. **识别主要改动**\n" +
                "   - 哪些文件/类/方法被修改？\n" +
                "   - 新增了什么？删除了什么？修改了什么？\n\n" +
                "2. **提取业务逻辑**\n" +
                "   - 改动的根本目的是什么？\n" +
                "   - 解决了什么问题？实现了什么功能？\n" +
                "   - 关键的逻辑变化是什么？\n\n" +
                "3. **总结影响范围**\n" +
                "   - 影响哪个模块/组件？\n" +
                "   - 改动的范围和深度如何？\n\n" +
                "## 输出格式要求\n\n" +
                "**严格遵循以下格式**（违反将导致失败）：\n\n" +
                "```\n" +
                "type(scope): 简明扼要的变更总结\n" +
                "\n" +
                "- 详细变更点 1\n" +
                "- 详细变更点 2\n" +
                "- 详细变更点 3\n" +
                "```\n\n" +
                "**格式说明**：\n" +
                "- `type`: 从上述 8 种类型中选择最合适的一个\n" +
                "- `scope`: 影响的模块/组件/文件名（简短、精确）\n" +
                "- 第一行总结：高度概括所有变更的核心内容（50字以内）\n" +
                "- 详细变更点：列出 2-5 个关键改动，每个一行，使用 `-` 开头\n\n" +
                "## 绝对禁止的输出\n\n" +
                "❌ 不要输出任何解释性文字，如：\n" +
                "   - \"基于 git diff 分析...\"\n" +
                "   - \"这是分析结果...\"\n" +
                "   - \"建议使用以下 commit message：\"\n" +
                "   - \"这个提交信息：\"\n\n" +
                "❌ 不要使用 Markdown 代码块（```）\n" +
                "❌ 不要添加任何额外的说明或注释\n" +
                "❌ 不要使用英文描述（除了 type 和 scope）\n\n" +
                "## 输出示例\n\n" +
                "**正确示例 1 - 新功能**：\n" +
                "feat(commit): 增强 git diff 获取逻辑以支持新文件和删除文件\n" +
                "\n" +
                "- 添加文件变更类型识别（新增/删除/修改）\n" +
                "- 针对新文件使用 git diff --cached HEAD 命令\n" +
                "- 针对删除文件使用专门的 diff 命令序列\n" +
                "- 改进日志输出，显示文件类型和 diff 字符数\n\n" +
                "**正确示例 2 - 修复**：\n" +
                "fix(api): 修复用户登录接口空指针异常\n" +
                "\n" +
                "- 添加用户对象空值检查\n" +
                "- 优化异常处理逻辑\n" +
                "- 完善错误日志输出\n\n" +
                "**正确示例 3 - 重构**：\n" +
                "refactor(service): 重构订单处理服务以提高代码可维护性\n" +
                "\n" +
                "- 提取订单验证逻辑到独立方法\n" +
                "- 简化订单状态更新流程\n" +
                "- 移除重复的数据库查询代码\n\n" +
                "## 关键要求\n\n" +
                "1. ✅ 使用中文描述（type 和 scope 除外）\n" +
                "2. ✅ 第一行必须是完整的变更总结\n" +
                "3. ✅ 详细变更点要具体、可操作\n" +
                "4. ✅ 直接输出 commit message，不要任何前缀\n" +
                "5. ✅ 准确识别变更类型\n" +
                "6. ✅ 清晰描述核心逻辑\n\n" +
                "**立即开始**：直接以 `type(scope):` 格式输出，不要任何其他内容。";
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
