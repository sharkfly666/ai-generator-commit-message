package com.github.jdami.aicommit.service.util;

import com.github.jdami.aicommit.settings.AiSettingsState;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Utility class for intelligently compressing diff content.
 * It removes context lines (lines starting with space) when the diff is too large,
 * keeping only headers and changes (+/- lines).
 */
public final class DiffCompressor {

    private DiffCompressor() {
    }

    /**
     * Compresses the diff content if it exceeds the max chars limit.
     * Compression is achieved by removing context lines.
     *
     * @param diffContent original diff content
     * @return compressed diff if compression was needed and successful, otherwise original diff
     */
    @NotNull
    public static String compress(@NotNull String diffContent) {
        AiSettingsState settings = AiSettingsState.getInstance();
        int maxChars = settings.maxDiffChars;

        // If unlimited or within limits, no need to compress
        if (maxChars <= 0 || diffContent.length() <= maxChars) {
            return diffContent;
        }

        System.out.println("Diff length (" + diffContent.length() + ") exceeds limit (" + maxChars + "). Attempting compression...");

        StringBuilder compressed = new StringBuilder(diffContent.length() / 2);
        try (BufferedReader reader = new BufferedReader(new StringReader(diffContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Keep headers and metadata
                if (line.startsWith("diff --git") ||
                    line.startsWith("index") ||
                    line.startsWith("---") ||
                    line.startsWith("+++") ||
                    line.startsWith("@@") ||
                    line.startsWith("File:") || // Binary file header
                    line.startsWith("Operation:") // Binary file operation
                ) {
                    compressed.append(line).append('\n');
                    continue;
                }

                // Keep additions and deletions
                if (line.startsWith("+") || line.startsWith("-")) {
                    compressed.append(line).append('\n');
                    continue;
                }

                // Skip context lines (starting with space)
                // Note: empty lines are also context usually, or just formatting
            }
        } catch (IOException e) {
            // Should not happen with StringReader
            return diffContent;
        }

        String result = compressed.toString();
        System.out.println("Compressed diff from " + diffContent.length() + " to " + result.length() + " chars.");
        
        return result;
    }
}
