# AI Commit Message Generator / AI 提交信息生成器

[English](#english) | [中文](#中文)

---

<a name="english"></a>
## English

An IntelliJ IDEA plugin that automatically generates professional commit messages using AI (Ollama or OpenAI) based on your code changes.

### ✨ Features

- 🎯 **Smart Change Type Detection**: Automatically identifies change types (feat/fix/refactor/perf/docs/style/test/chore)
- 🧠 **Core Logic Analysis**: Extracts and summarizes the essential business logic changes
- 📝 **Professional Format**: Generates structured commit messages following industry best practices
- 🔀 **Multiple AI Providers**: Support for both Ollama (local) and OpenAI (cloud)
- ⚙️ **Highly Customizable**: Configure endpoints, models, and system prompts
- 🌐 **Bilingual Support**: Generates commit messages in Chinese with English type/scope
- 🚀 **Seamless Integration**: Works directly in the commit dialog
- ⏱️ **Cancellable**: Stop generation at any time

### 📋 Prerequisites

- IntelliJ IDEA 2025.3 or higher
- **For Ollama**: [Ollama](https://ollama.ai/) running locally with a downloaded model (e.g., qwen3:8b, llama2, codellama)
- **For OpenAI**: Valid OpenAI API key

### 📦 Installation

#### From Source

1. Clone the repository:
```bash
git clone https://github.com/jdami/ai-generator-commit-message.git
cd ai-generator-commit-message
```

2. Build the plugin:
```bash
./gradlew buildPlugin
```

3. Install in IntelliJ IDEA:
   - Open `Settings/Preferences` → `Plugins`
   - Click the gear icon → `Install Plugin from Disk...`
   - Select `build/distributions/ai-generator-commit-message-1.0.5.zip`

4. Restart IntelliJ IDEA

### ⚙️ Configuration

1. Open `Settings/Preferences` → `Tools` → `AI Commit Message Generator`
2. Configure the following options:

#### Provider Settings

**Ollama (Local AI)**
- **Endpoint**: Default `http://localhost:11434`
- **Model**: Default `qwen3:8b` (or llama2, codellama, mistral, etc.)
- **Timeout**: Request timeout in seconds (default: 30)

**OpenAI (Cloud AI)**
- **API Base**: Default `https://api.openai.com`
- **Model**: Default `gpt-4o-mini` (or gpt-4, gpt-3.5-turbo, etc.)
- **API Key**: Your OpenAI API key
- **Timeout**: Request timeout in seconds (default: 30)

#### System Prompt

The system prompt guides the AI on how to generate commit messages. The default prompt includes:
- Change type classification rules (feat/fix/refactor/perf/docs/style/test/chore)
- Core logic analysis methodology
- Structured output format requirements
- Examples of professional commit messages

You can customize the system prompt to match your team's commit message conventions.

### 🚀 Usage

1. **Make code changes** in your project
2. **Open the commit dialog** (`Ctrl+K` / `Cmd+K`)
3. **Select the changes** you want to commit
4. **Click the "Commit助手" button** in the toolbar (AI icon)
5. **Wait for AI generation** (you can click again to cancel)
6. **Review and edit** the generated commit message if needed
7. **Commit your changes**

### 📝 Commit Message Format

The plugin generates commit messages in the following format:

```
type(scope): Brief summary of all changes

- Detailed change point 1
- Detailed change point 2
- Detailed change point 3
```

#### Change Types

- **feat**: New features, files, classes, methods, or API endpoints
- **fix**: Bug fixes, error corrections, exception handling
- **refactor**: Code restructuring without changing functionality
- **perf**: Performance optimizations, algorithm improvements
- **style**: Code formatting, naming improvements (no logic changes)
- **docs**: Documentation updates, README changes
- **test**: Test case additions or modifications
- **chore**: Build configuration, dependency updates, tooling

#### Examples

**New Feature:**
```
feat(commit): 增强 git diff 获取逻辑以支持新文件和删除文件

- 添加文件变更类型识别（新增/删除/修改）
- 针对新文件使用 git diff --cached HEAD 命令
- 针对删除文件使用专门的 diff 命令序列
- 改进日志输出，显示文件类型和 diff 字符数
```

**Bug Fix:**
```
fix(api): 修复用户登录接口空指针异常

- 添加用户对象空值检查
- 优化异常处理逻辑
- 完善错误日志输出
```

**Refactoring:**
```
refactor(service): 重构订单处理服务以提高代码可维护性

- 提取订单验证逻辑到独立方法
- 简化订单状态更新流程
- 移除重复的数据库查询代码
```

### 🛠️ Development

#### Project Structure

```
src/main/java/com/github/jdami/aicommit/
├── actions/
│   └── GenerateCommitMessageAction.java    # Main action for generating commit messages
├── service/
│   ├── AiService.java                       # Provider routing facade
│   ├── provider/                            # AI provider implementations
│   │   ├── OllamaProviderClient.java
│   │   └── OpenAiProviderClient.java
│   ├── model/
│   │   └── GenerationInputs.java            # Request parameters
│   └── util/
│       ├── CommitMessageCleaner.java        # Response cleaning
│       └── PromptBuilder.java               # Prompt construction
├── settings/
│   ├── AiSettingsState.java                # Settings persistence
│   ├── AiSettingsConfigurable.java         # Settings configurator
│   └── AiSettingsComponent.java            # Settings UI component
└── vcs/
    └── CommitMessageGeneratorCheckinHandlerFactory.java  # VCS integration
```

#### Run Development Version

```bash
./gradlew runIde
```

#### Build Plugin

```bash
./gradlew buildPlugin
```

The built plugin will be in `build/distributions/`.

#### Create Release

The project uses GitHub Actions to automatically build and release the plugin.

**Method 1: Manual Trigger**
1. Go to GitHub repository → Actions → Release Plugin
2. Click "Run workflow"
3. Enter version number (e.g., `1.0.5`)
4. Click "Run workflow"

**Method 2: Git Tag**
```bash
git tag v1.0.5
git push origin v1.0.5
```

The workflow will:
- Build the plugin
- Create a GitHub Release
- Upload the plugin ZIP file
- Include installation instructions

### 🔧 Troubleshooting

**"No diff content found" error:**
- Ensure you have selected changes in the commit dialog
- Make sure the files are tracked by Git
- For new files, ensure they are added to Git (`git add`)

**Connection timeout:**
- Check if Ollama/OpenAI service is running
- Verify the endpoint URL is correct
- Increase the timeout value in settings

**Poor quality commit messages:**
- Try a different AI model (larger models often perform better)
- Customize the system prompt to better match your needs
- Ensure your code changes are meaningful and well-structured

### 🛠️ Tech Stack

- **Language**: Java 21
- **Build Tool**: Gradle 9.4.1
- **Framework**: IntelliJ Platform SDK
- **Target IDE**: IntelliJ IDEA 2025.3+
- **Dependencies**:
  - OkHttp 4.12.0 - HTTP client
  - Gson 2.10.1 - JSON parsing

### 📄 License

MIT License

### 🤝 Contributing

Issues and Pull Requests are welcome!

### 🙏 Acknowledgments

- [Ollama](https://ollama.ai/) - Local AI model runtime
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/) - Plugin development documentation

---

<a name="中文"></a>
## 中文

一个 IntelliJ IDEA 插件，使用 AI（Ollama 或 OpenAI）根据代码变更自动生成专业的提交信息。

### ✨ 功能特性

- 🎯 **智能变更类型识别**：自动识别变更类型（feat/fix/refactor/perf/docs/style/test/chore）
- 🧠 **核心逻辑分析**：提取并总结关键的业务逻辑变化
- � **专业格式**：生成遵循行业最佳实践的结构化提交信息
- �🔀 **多 AI 提供商**：支持 Ollama（本地）和 OpenAI（云端）
- ⚙️ **高度可定制**：配置端点、模型和系统提示词
- 🌐 **双语支持**：生成中文描述的提交信息（type/scope 使用英文）
- 🚀 **无缝集成**：直接在提交对话框中工作
- ⏱️ **可取消**：随时停止生成

### 📋 前置要求

- IntelliJ IDEA 2025.3 或更高版本
- **使用 Ollama**：本地运行 [Ollama](https://ollama.ai/) 并下载模型（如 qwen3:8b、llama2、codellama）
- **使用 OpenAI**：有效的 OpenAI API 密钥

### 📦 安装

#### 从源码构建

1. 克隆仓库：
```bash
git clone https://github.com/jdami/ai-generator-commit-message.git
cd ai-generator-commit-message
```

2. 构建插件：
```bash
./gradlew buildPlugin
./gradlew verifyPlugin
```

3. 在 IntelliJ IDEA 中安装：
   - 打开 `Settings/Preferences` → `Plugins`
   - 点击齿轮图标 → `Install Plugin from Disk...`
   - 选择 `build/distributions/ai-generator-commit-message-1.0.5.zip`

4. 重启 IntelliJ IDEA

### ⚙️ 配置

1. 打开 `Settings/Preferences` → `Tools` → `AI Commit Message Generator`
2. 配置以下选项：

#### 提供商设置

**Ollama（本地 AI）**
- **端点**：默认 `http://localhost:11434`
- **模型**：默认 `qwen3:8b`（或 llama2、codellama、mistral 等）
- **超时时间**：请求超时时间（秒），默认 30

**OpenAI（云端 AI）**
- **API 基地址**：默认 `https://api.openai.com`
- **模型**：默认 `gpt-4o-mini`（或 gpt-4、gpt-3.5-turbo 等）
- **API 密钥**：您的 OpenAI API 密钥
- **超时时间**：请求超时时间（秒），默认 30

#### 系统提示词

系统提示词用于指导 AI 如何生成提交信息。默认提示词包括：
- 变更类型分类规则（feat/fix/refactor/perf/docs/style/test/chore）
- 核心逻辑分析方法
- 结构化输出格式要求
- 专业提交信息示例

您可以自定义系统提示词以匹配团队的提交信息规范。

### 🚀 使用方法

1. **修改代码**
2. **打开提交对话框**（`Ctrl+K` / `Cmd+K`）
3. **选择要提交的变更**
4. **点击工具栏中的"Commit助手"按钮**（AI 图标）
5. **等待 AI 生成**（可再次点击取消）
6. **检查并编辑**生成的提交信息（如需要）
7. **提交代码**

### 📝 提交信息格式

插件生成以下格式的提交信息：

```
type(scope): 简明扼要的变更总结

- 详细变更点 1
- 详细变更点 2
- 详细变更点 3
```

#### 变更类型

- **feat**：新功能、新文件、新类/方法、新 API 接口
- **fix**：Bug 修复、错误更正、异常处理
- **refactor**：代码重构（不改变功能）
- **perf**：性能优化、算法改进
- **style**：代码格式、命名改进（不影响逻辑）
- **docs**：文档更新、README 修改
- **test**：测试用例添加或修改
- **chore**：构建配置、依赖更新、工具链

#### 示例

**新功能：**
```
feat(commit): 增强 git diff 获取逻辑以支持新文件和删除文件

- 添加文件变更类型识别（新增/删除/修改）
- 针对新文件使用 git diff --cached HEAD 命令
- 针对删除文件使用专门的 diff 命令序列
- 改进日志输出，显示文件类型和 diff 字符数
```

**Bug 修复：**
```
fix(api): 修复用户登录接口空指针异常

- 添加用户对象空值检查
- 优化异常处理逻辑
- 完善错误日志输出
```

**重构：**
```
refactor(service): 重构订单处理服务以提高代码可维护性

- 提取订单验证逻辑到独立方法
- 简化订单状态更新流程
- 移除重复的数据库查询代码
```

### 🛠️ 开发

#### 项目结构

```
src/main/java/com/github/jdami/aicommit/
├── actions/
│   └── GenerateCommitMessageAction.java    # 生成提交信息的主要动作
├── service/
│   ├── AiService.java                       # 提供商路由门面
│   ├── provider/                            # AI 提供商实现
│   │   ├── OllamaProviderClient.java
│   │   └── OpenAiProviderClient.java
│   ├── model/
│   │   └── GenerationInputs.java            # 请求参数
│   └── util/
│       ├── CommitMessageCleaner.java        # 响应清洗
│       └── PromptBuilder.java               # 提示词构建
├── settings/
│   ├── AiSettingsState.java                # 设置持久化
│   ├── AiSettingsConfigurable.java         # 设置配置器
│   └── AiSettingsComponent.java            # 设置 UI 组件
└── vcs/
    └── CommitMessageGeneratorCheckinHandlerFactory.java  # VCS 集成
```

#### 运行开发版本

```bash
./gradlew runIde
```

#### 构建插件

```bash
./gradlew buildPlugin
./gradlew verifyPlugin
```

生成的插件位于 `build/distributions/` 目录。

#### 创建发布版本

项目使用 GitHub Actions 自动构建和发布插件。

**方法 1：手动触发**
1. 进入 GitHub 仓库 → Actions → Release Plugin
2. 点击 "Run workflow"
3. 输入版本号（如 `1.0.5`）
4. 点击 "Run workflow"

**方法 2：Git 标签**
```bash
git tag v1.0.5
git push origin v1.0.5
```

工作流将：
- 构建插件
- 创建 GitHub Release
- 上传插件 ZIP 文件
- 包含安装说明

### 🔧 故障排除

**"No diff content found" 错误：**
- 确保在提交对话框中选择了变更
- 确保文件已被 Git 跟踪
- 对于新文件，确保已添加到 Git（`git add`）

**连接超时：**
- 检查 Ollama/OpenAI 服务是否运行
- 验证端点 URL 是否正确
- 在设置中增加超时时间

**提交信息质量不佳：**
- 尝试不同的 AI 模型（更大的模型通常表现更好）
- 自定义系统提示词以更好地匹配您的需求
- 确保代码变更有意义且结构良好

### 🛠️ 技术栈

- **语言**：Java 21
- **构建工具**：Gradle 9.4.1
- **框架**：IntelliJ Platform SDK
- **目标 IDE**：IntelliJ IDEA 2025.3+
- **依赖**：
  - OkHttp 4.12.0 - HTTP 客户端
  - Gson 2.10.1 - JSON 解析

### 📄 许可证

MIT License

### 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 🙏 致谢

- [Ollama](https://ollama.ai/) - 本地 AI 模型运行时
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/) - 插件开发文档
