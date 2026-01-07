package com.github.jdami.aicommit.service.util;

import org.jetbrains.annotations.NotNull;

/**
 * Utility to build prompts for commit message generation.
 */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String buildPrompt(@NotNull String diffContent) {
        return "请分析以下 Git Diff 并生成专业的 commit message。\n\n" +
                "## 分析步骤\n\n" +
                "1. **识别变更类型**\n" +
                "   - 检查是否有新增文件（new file mode）→ 可能是 feat\n" +
                "   - 检查是否修复了问题或错误 → 可能是 fix\n" +
                "   - 检查是否重构了代码结构 → 可能是 refactor\n" +
                "   - 检查是否优化了性能 → 可能是 perf\n" +
                "   - 根据系统指令中的规则选择最合适的类型\n\n" +
                "2. **提取核心逻辑**\n" +
                "   - 识别主要改动的文件和方法\n" +
                "   - 理解改动的业务目的\n" +
                "   - 总结关键的逻辑变化\n\n" +
                "3. **确定 scope**\n" +
                "   - 基于主要改动的模块/组件/文件确定 scope\n" +
                "   - 保持简短精确（如：api, service, config, ui 等）\n\n" +
                "4. **生成 commit message**\n" +
                "   - 第一行：type(scope): 简明总结\n" +
                "   - 空一行\n" +
                "   - 详细变更点：2-5 个要点，每行一个，使用 `-` 开头\n\n" +
                "---\n\n" +
                "## Git Diff 内容\n\n" +
                "```diff\n" +
                diffContent + "\n" +
                "```\n\n" +
                "---\n\n" +
                "现在请直接输出 commit message（不要任何解释或前缀）：";
    }
}
