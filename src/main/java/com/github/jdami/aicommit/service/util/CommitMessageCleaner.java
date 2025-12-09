package com.github.jdami.aicommit.service.util;

/**
 * Utility methods to clean AI responses into valid commit messages.
 */
public final class CommitMessageCleaner {

    private CommitMessageCleaner() {
    }

    public static String clean(String rawResponse) {
        String cleaned = removeThinkTags(rawResponse);
        return extractCommitMessage(cleaned);
    }

    /**
     * Remove <think>...</think> tags and their content from the response
     */
    public static String removeThinkTags(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?is)<think>.*?</think>", "").trim();
    }

    /**
     * Extract only the commit message from the response, removing any explanatory text
     */
    public static String extractCommitMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text.trim();

        String[] unwantedPrefixes = {
                "Based on the git diff",
                "Here's the analysis",
                "This message:",
                "You can use this",
                "The suggested commit message",
                "Here is the suggested commit message",
                "Analysis of the changes:",
                "```",
                "Here's the commit message:",
                "Commit message:"
        };

        for (String prefix : unwantedPrefixes) {
            if (cleaned.toLowerCase().startsWith(prefix.toLowerCase())) {
                cleaned = cleaned.substring(prefix.length()).trim();
                cleaned = cleaned.replaceFirst("^[:\\-\\s]+", "").trim();
            }
        }

        // Remove markdown code blocks
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "").trim();
        cleaned = cleaned.replaceAll("```.*", "").trim();

        String[] unwantedSuffixes = {
                "This message:",
                "You can use this message",
                "This commit message",
                "The above message"
        };

        for (String suffix : unwantedSuffixes) {
            int index = cleaned.toLowerCase().indexOf(suffix.toLowerCase());
            if (index > 0) {
                cleaned = cleaned.substring(0, index).trim();
            }
        }

        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundCommitStart = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (!foundCommitStart && isCommitMessageLine(line)) {
                foundCommitStart = true;
                result.append(line).append("\n");
            } else if (foundCommitStart) {
                if (line.startsWith("- ") || isCommitMessageLine(line)) {
                    result.append(line).append("\n");
                } else if (line.toLowerCase().contains("this message") ||
                        line.toLowerCase().contains("you can use") ||
                        line.toLowerCase().contains("analysis")) {
                    break;
                }
            }
        }

        return result.toString().trim();
    }

    private static boolean isCommitMessageLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }

        String trimmed = line.trim();

        String[] commitTypes = {"feat(", "fix(", "docs(", "style(", "refactor(", "test(", "chore("};
        for (String type : commitTypes) {
            if (trimmed.toLowerCase().startsWith(type)) {
                return true;
            }
        }

        if (trimmed.startsWith("- ")) {
            return true;
        }

        return false;
    }
}
