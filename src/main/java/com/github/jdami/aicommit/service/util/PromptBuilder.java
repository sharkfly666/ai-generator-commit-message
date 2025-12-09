package com.github.jdami.aicommit.service.util;

import org.jetbrains.annotations.NotNull;

/**
 * Utility to build prompts for commit message generation.
 */
public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String buildPrompt(@NotNull String diffContent) {
        return "Analyze the following git diff and generate a commit message based on the system instructions.\n\n" +
                "Git Diff:\n" +
                diffContent + "\n\n" +
                "Generate the commit message now:";
    }
}
