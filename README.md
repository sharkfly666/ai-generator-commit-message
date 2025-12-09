# AI Commit Message Generator

一个 IntelliJ IDEA 插件,使用 Ollama 或 OpenAI 根据代码变更自动生成 commit message。

## 功能特性

- ✨ 在 commit 对话框中添加"生成 Commit Message"按钮
- 🤖 分析暂存的代码变更并生成有意义的 commit message
- ⚙️ 可自定义 Ollama/OpenAI API 地址与模型
- 🔀 设置页可切换模型厂商,不同厂商支持不同参数
- 📝 可配置系统提示词以控制生成风格

## 前置要求

- IntelliJ IDEA 2023.2 或更高版本
- [Ollama](https://ollama.ai/) 服务运行中
- 已下载的 Ollama 模型(如 llama2,qwen, codellama, mistral 等)

## 安装

### 从源码构建

1. 克隆此仓库:
```bash
git clone https://github.com/jdami/ai-generator-commit-message.git
cd ai-generator-commit-message
```

2. 构建插件:
```bash
./gradlew buildPlugin
```

3. 在 IntelliJ IDEA 中安装:
   - 打开 `Settings/Preferences` → `Plugins`
   - 点击齿轮图标 → `Install Plugin from Disk...`
   - 选择 `build/distributions/ai-generator-commit-message-1.0.0.zip`

## 配置

1. 打开 `Settings/Preferences` → `Tools` → `AI Commit Message Generator`
2. 配置以下选项:
   - **Provider**: 选择 `Ollama` 或 `OpenAI`
   - **Ollama Endpoint / Model**: 服务地址(默认 `http://localhost:11434`) 与模型(默认 `qwen3:8b`)
   - **OpenAI API Base / Model / Key**: API 基地址(默认 `https://api.openai.com`)、模型(默认 `gpt-4o-mini`)与 API Key
   - **Timeout**: 请求超时时间(秒)
   - **System Prompt**: 系统提示词,用于指导 AI 生成风格

## 使用方法

1. 在项目中进行代码修改
2. 打开 commit 对话框(`Ctrl+K` / `Cmd+K`)
3. 选择要提交的变更
4. 点击工具栏中的 "Generate Commit Message" 按钮(💡图标)
5. 等待 AI 生成 commit message
6. 根据需要编辑生成的消息
7. 提交代码

## 开发

### 项目结构

```
src/main/java/com/github/jdami/aicommit/
├── actions/
│   └── GenerateCommitMessageAction.java    # 生成 commit message 的动作
├── service/
│   ├── AiService.java                       # Provider 路由/门面
│   ├── provider/                            # 各模型厂商实现
│   │   ├── OllamaProviderClient.java
│   │   └── OpenAiProviderClient.java
│   ├── model/
│   │   └── GenerationInputs.java            # 请求入参
│   └── util/
│       ├── CommitMessageCleaner.java        # 响应清洗
│       └── PromptBuilder.java               # Prompt 构造
├── settings/
│   ├── AiSettingsState.java                # 设置持久化
│   ├── AiSettingsConfigurable.java         # 设置配置器
│   └── AiSettingsComponent.java            # 设置 UI 组件
└── vcs/
    └── CommitMessageGeneratorCheckinHandlerFactory.java  # VCS 集成
```

### 运行开发版本

```bash
./gradlew runIde
```

### 构建插件

```bash
./gradlew buildPlugin
```

生成的插件位于 `build/distributions/` 目录。

## 技术栈

- **语言**: Java 17
- **构建工具**: Gradle 8.5
- **框架**: IntelliJ Platform SDK
- **依赖**:
  - OkHttp 4.12.0 - HTTP 客户端
  - Gson 2.10.1 - JSON 解析

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request!

## 致谢

- [Ollama](https://ollama.ai/) - 本地 AI 模型运行时
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/) - 插件开发文档
# ai-generator-commit-message
