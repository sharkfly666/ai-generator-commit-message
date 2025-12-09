# AI Commit Message Generator 项目说明

## 项目简介
- IntelliJ IDEA 插件，通过调用本地 Ollama 服务基于 Git 变更自动生成符合规范的 commit message。
- 目标：在提交对话框中一键生成中文的 `type(scope): summary` + 变更要点格式，减少人工编写成本并提升一致性。

## 核心功能
- 提供工具栏按钮（动作 `GenerateCommitMessageAction`）在提交面板中触发生成。
- 收集选中变更的 diff：优先使用已暂存内容，若为空再回退到未暂存或 `HEAD` 对比，确保能覆盖新文件和修改文件。
- 支持 Ollama `/api/generate` 与 OpenAI `/v1/chat/completions`，均可自定义 API 地址和模型；OpenAI 需配置 API Key。
- 对返回内容做清洗：移除 `<think>` 等大模型思考片段、去掉说明性前后缀，仅保留 commit message。
- 设置页（`Settings/Tools/AI Commit Message Generator`）提供 UI 配置，插件升级时自动重置为新的默认值。

## 运行流程（提交侧）
1. 用户在提交对话框选择变更并点击“Git助手”按钮。
2. `GenerateCommitMessageAction` 在后台任务中调用 `git diff` 生成合并后的 diff 文本。
3. `AiService` 构建 prompt 并将请求分发到对应 Provider 客户端（Ollama/OpenAI）。
4. Provider 使用 OkHttp 请求其 API，解析响应并清洗后写入提交信息文本框，用户可直接提交或二次编辑。

## 关键模块与文件
- `src/main/java/com/github/jdami/aicommit/actions/GenerateCommitMessageAction.java`：入口动作，收集 diff 并调用服务。
- `src/main/java/com/github/jdami/aicommit/service/AiService.java`：Provider 路由与门面。
- `src/main/java/com/github/jdami/aicommit/service/provider/*`：Ollama / OpenAI 客户端实现。
- `src/main/java/com/github/jdami/aicommit/service/util/*`：Prompt 构造与响应清洗工具。
- `src/main/java/com/github/jdami/aicommit/settings/*`：配置状态持久化、设置页 UI 与校验，`AiSettingsState` 内含默认系统提示词。
- `src/main/java/com/github/jdami/aicommit/startup/PluginUpdateActivity.java`：启动时检测插件版本变化，必要时重置配置。
- `src/main/java/com/github/jdami/aicommit/vcs/CommitMessageGeneratorCheckinHandlerFactory.java`：确保动作正确挂载到提交 UI。
- `src/main/resources/META-INF/plugin.xml`：插件元数据、依赖声明、动作与扩展点注册。

## 配置项（默认值）
- `provider`: `OLLAMA`（可切换为 `OPENAI`）
- Ollama: `ollamaEndpoint`=`http://localhost:11434`, `ollamaModel`=`qwen3:8b`
- OpenAI: `openAiEndpoint`=`https://api.openai.com`, `openAiModel`=`gpt-4o-mini`, `openAiApiKey`=`""`
- 通用: `timeout`=`30` 秒、`systemPrompt`（`type(scope)` + 中文要点模板）、`pluginVersion`

## 构建与运行
- 构建插件包：`./gradlew buildPlugin`（产物在 `build/distributions/`）。
- 运行开发版 IDE：`./gradlew runIde`。
- 依赖：Java 17、Gradle 8.5、IntelliJ Platform 2023.2.5，插件依赖 `Git4Idea`，第三方库使用 OkHttp 4.12.0、Gson 2.10.1。

## 与 Ollama 交互细节
- 请求体（Ollama）：`model`、`prompt`（包含 diff）、`system`（系统提示词）、`stream:false`。
- 请求体（OpenAI）：`model`、`messages:[{role:system,content:systemPrompt},{role:user,content:prompt}]`、`stream:false`，带 `Authorization: Bearer <API_KEY>`。
- 连接测试：设置页中的“Test Connection”调用 `/api/tags` 判断服务可用性（当前针对 Ollama）。
- 响应清洗：过滤 `<think>` 标签、多余说明或 Markdown 代码块，保留形如 `type(scope): ...` 的首行和要点列表。

## 开发注意事项
- 仅在存在 Git 仓库和选中变更时才会触发生成；否则会在 UI 提示。
- Diff 采集依赖本地 `git` 命令，必要时检查仓库路径或权限。
- 系统提示词包含严格格式要求，调整时需确保不会让模型输出额外说明或代码块。

## 常见排查提示
- 生成失败：检查 Ollama 是否运行、端点地址/模型名称是否正确，或提升超时配置。
- 生成内容为空：确认变更已选中且 `git diff` 能产生输出（包括新增文件）。
- 提示未找到 Git 仓库：确保项目已初始化 Git 并在 IDEA 中打开对应根目录。
