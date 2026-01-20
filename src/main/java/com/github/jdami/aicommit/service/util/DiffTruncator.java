package com.github.jdami.aicommit.service.util;

import com.github.jdami.aicommit.settings.AiSettingsState;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for truncating diff content to prevent AI model 400 errors
 * caused by content exceeding token limits.
 */
public final class DiffTruncator {

    // Estimated overhead from prompt template (PromptBuilder adds ~700 chars)
    private static final int PROMPT_TEMPLATE_OVERHEAD = 800;
    
    // Reserve space for model response (at least 500 tokens = ~2000 chars)
    private static final int RESPONSE_RESERVE = 2000;

    private DiffTruncator() {
    }

    /**
     * Truncates the diff content based on user settings.
     * Takes into account the system prompt length and prompt template overhead.
     *
     * @param diffContent the original diff content
     * @return truncated diff content if necessary, or original content if within limits
     */
    @NotNull
    public static String truncate(@NotNull String diffContent) {
        AiSettingsState settings = AiSettingsState.getInstance();
        int maxTotalChars = settings.maxDiffChars;
        
        // Calculate actual available space for diff
        int systemPromptLength = settings.systemPrompt != null ? settings.systemPrompt.length() : 0;
        int totalOverhead = systemPromptLength + PROMPT_TEMPLATE_OVERHEAD + RESPONSE_RESERVE;
        int maxDiffChars = maxTotalChars - totalOverhead;
        
        // Debug logging
        System.out.println("=== DiffTruncator Debug ===");
        System.out.println("User configured limit: " + maxTotalChars + " chars");
        System.out.println("System prompt length: " + systemPromptLength + " chars");
        System.out.println("Prompt template overhead: " + PROMPT_TEMPLATE_OVERHEAD + " chars");
        System.out.println("Response reserve: " + RESPONSE_RESERVE + " chars");
        System.out.println("Total overhead: " + totalOverhead + " chars");
        System.out.println("Actual max for diff: " + maxDiffChars + " chars");
        System.out.println("Original diff length: " + diffContent.length() + " chars");
        System.out.println("Estimated total tokens: ~" + estimateTokens(systemPromptLength + PROMPT_TEMPLATE_OVERHEAD + diffContent.length()));
        
        if (maxTotalChars <= 0) {
            System.out.println("Truncation DISABLED (limit <= 0)");
            System.out.println("===========================");
            return diffContent;
        }
        
        if (maxDiffChars <= 0) {
            System.out.println("WARNING: System prompt too long, leaving minimal space for diff!");
            maxDiffChars = 5000; // Fallback minimum
        }
        
        if (diffContent.length() <= maxDiffChars) {
            System.out.println("No truncation needed (within limit)");
            System.out.println("===========================");
            return diffContent;
        }
        
        // Find a good breaking point (end of a line near the limit)
        int cutPoint = findLineBreakBefore(diffContent, maxDiffChars);
        String truncated = diffContent.substring(0, cutPoint);
        int removedChars = diffContent.length() - cutPoint;
        
        System.out.println("TRUNCATING: " + diffContent.length() + " -> " + truncated.length() + " chars");
        System.out.println("Removed: " + removedChars + " chars (" + (removedChars * 100 / diffContent.length()) + "%)");
        System.out.println("Final estimated tokens: ~" + estimateTokens(systemPromptLength + PROMPT_TEMPLATE_OVERHEAD + truncated.length()));
        System.out.println("===========================");
        
        String truncationMessage = String.format(
            "\n\n[... 内容过长，已截断 %,d 字符 (原始 %,d 字符, 保留 %.1f%%) ...]",
            removedChars, diffContent.length(), (truncated.length() * 100.0 / diffContent.length())
        );
        
        return truncated + truncationMessage;
    }

    /**
     * Finds the last line break before the given position.
     * This ensures we don't cut in the middle of a line.
     */
    private static int findLineBreakBefore(String content, int maxPosition) {
        if (maxPosition >= content.length()) {
            return content.length();
        }
        int lastNewline = content.lastIndexOf('\n', maxPosition);
        if (lastNewline > 0) {
            return lastNewline;
        }
        // No newline found, just use the max position
        return maxPosition;
    }

    /**
     * Estimates the number of tokens for a given character count.
     * For Chinese/code mixed content, approximately 3-4 characters = 1 token.
     *
     * @param charCount number of characters
     * @return estimated token count
     */
    public static int estimateTokens(int charCount) {
        return (int) Math.ceil(charCount / 3.5);
    }

    /**
     * Formats diff statistics for display.
     *
     * @param diffContent the diff content
     * @return formatted statistics string
     */
    @NotNull
    public static String getStats(@NotNull String diffContent) {
        int chars = diffContent.length();
        int lines = diffContent.split("\n").length;
        int estimatedTokens = estimateTokens(chars);
        
        return String.format(
            "字符数: %,d | 行数: %,d | 预估 Token: ~%,d",
            chars, lines, estimatedTokens
        );
    }
}
