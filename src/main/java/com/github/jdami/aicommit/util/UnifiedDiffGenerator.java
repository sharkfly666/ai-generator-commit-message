package com.github.jdami.aicommit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for generating unified diff format from VCS changes.
 * Uses a line-level LCS-based diff algorithm instead of removed IntelliJ internal patch API.
 */
public class UnifiedDiffGenerator {

    private static final int CONTEXT_LINES = 3;

    /**
     * Generate unified diff from a Change object.
     */
    public static String generateDiff(@NotNull Change change, @NotNull Project project) {
        try {
            VirtualFile vf = change.getVirtualFile();
            String filePath = vf != null ? vf.getPath() : (change.getAfterRevision() != null 
                ? change.getAfterRevision().getFile().getPath() 
                : "unknown");

            ContentRevision beforeRev = change.getBeforeRevision();
            ContentRevision afterRev = change.getAfterRevision();

            String beforeContent = beforeRev != null ? beforeRev.getContent() : "";
            String afterContent = afterRev != null ? afterRev.getContent() : "";

            // Handle binary / null content
            if (beforeContent == null) beforeContent = "";
            if (afterContent == null) afterContent = "";

            // Skip if no changes and not a new/deleted file
            if (beforeContent.equals(afterContent) && beforeRev != null && afterRev != null) {
                return "";
            }

            String[] beforeLines = beforeContent.isEmpty() ? new String[0] : beforeContent.split("\n", -1);
            String[] afterLines = afterContent.isEmpty() ? new String[0] : afterContent.split("\n", -1);

            return buildUnifiedDiff(filePath, beforeLines, afterLines);
        } catch (VcsException e) {
            return "Error generating diff: " + e.getMessage();
        } catch (Exception e) {
            return "Failed to generate diff for: " + (change.getVirtualFile() != null 
                ? change.getVirtualFile().getName() : "unknown");
        }
    }

    private static String buildUnifiedDiff(String filePath, String[] beforeLines, String[] afterLines) {
        StringBuilder sb = new StringBuilder();

        // --- header ---
        String relativePath = toRelative(filePath);
        sb.append("--- a/").append(relativePath).append("\n");
        sb.append("+++ b/").append(relativePath).append("\n");

        // LCS-based diff
        int[][] lcs = computeLcsMatrix(beforeLines, afterLines);
        List<int[]> hunks = extractHunks(beforeLines, afterLines, lcs);

        if (hunks.isEmpty()) {
            return sb.toString();
        }

        for (int[] hunk : hunks) {
            int beforeStart = hunk[0];
            int beforeCount = hunk[1];
            int afterStart = hunk[2];
            int afterCount = hunk[3];

            // Compute the start with context
            int ctxBeforeStart = Math.max(0, beforeStart - CONTEXT_LINES);
            int ctxAfterStart = Math.max(0, afterStart - CONTEXT_LINES);

            // The hunk may be followed by context lines extending beyond the hunk
            int ctxBeforeEnd = Math.min(beforeLines.length, beforeStart + beforeCount + CONTEXT_LINES);
            int ctxAfterEnd = Math.min(afterLines.length, afterStart + afterCount + CONTEXT_LINES);

            // Hunk line numbers are 1-based
            sb.append("@@ -").append(ctxBeforeStart + 1).append(",").append(ctxBeforeEnd - ctxBeforeStart)
              .append(" +").append(ctxAfterStart + 1).append(",").append(ctxAfterEnd - ctxAfterStart)
              .append(" @@\n");

            // Context before the change
            for (int i = ctxBeforeStart; i < beforeStart; i++) {
                sb.append(" ").append(beforeLines[i]).append("\n");
            }

            // Removed lines
            for (int i = beforeStart; i < beforeStart + beforeCount; i++) {
                if (i < beforeLines.length) {
                    sb.append("-").append(beforeLines[i]).append("\n");
                }
            }

            // Added lines
            for (int i = afterStart; i < afterStart + afterCount; i++) {
                if (i < afterLines.length) {
                    sb.append("+").append(afterLines[i]).append("\n");
                }
            }

            // Context after the change
            int ctxBeforeAfter = beforeStart + beforeCount;
            int ctxAfterAfter = afterStart + afterCount;

            // Compare equal trailing lines in the context window
            int ctxLine = 0;
            while (ctxBeforeAfter + ctxLine < beforeLines.length
                   && ctxAfterAfter + ctxLine < afterLines.length
                   && ctxLine < CONTEXT_LINES
                   && beforeLines[ctxBeforeAfter + ctxLine].equals(afterLines[ctxAfterAfter + ctxLine])) {
                sb.append(" ").append(beforeLines[ctxBeforeAfter + ctxLine]).append("\n");
                ctxLine++;
            }
        }

        return sb.toString();
    }

    /**
     * LCS matrix: lcs[i][j] = length of LCS of beforeLines[0..i-1] and afterLines[0..j-1].
     * Standard O(M*N) dynamic programming.
     */
    private static int[][] computeLcsMatrix(String[] before, String[] after) {
        int m = before.length;
        int n = after.length;
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (before[i - 1].equals(after[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }

    /**
     * Extract hunks from the LCS matrix by walking the backtrace.
     * Each hunk: [beforeStart, beforeCount, afterStart, afterCount].
     */
    private static List<int[]> extractHunks(String[] before, String[] after, int[][] lcs) {
        List<int[]> hunks = new ArrayList<>();
        List<int[]> diffs = new ArrayList<>();
        
        int i = before.length;
        int j = after.length;

        // Collect all change coordinates walking backwards through the LCS matrix
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && before[i - 1].equals(after[j - 1])) {
                i--;
                j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                // Line added (in after, not in before)
                diffs.add(new int[]{i, -1, j - 1, 1});
                j--;
            } else if (i > 0) {
                // Line removed (in before, not in after)
                diffs.add(new int[]{i - 1, -1, j, 1});
                i--;
            }
        }

        Collections.reverse(diffs);

        // Merge contiguous changes into hunks
        int beforeStart = -1;
        int beforeEnd = -1;
        int afterStart = -1;
        int afterEnd = -1;

        for (int[] d : diffs) {
            int bIdx = d[0];
            int aIdx = d[2];

            if (beforeStart == -1) {
                beforeStart = bIdx;
                beforeEnd = bIdx + 1;
                afterStart = aIdx;
                afterEnd = aIdx + 1;
            } else if (bIdx == beforeEnd && aIdx == afterEnd) {
                beforeEnd++;
                afterEnd++;
            } else {
                hunks.add(new int[]{beforeStart, beforeEnd - beforeStart, afterStart, afterEnd - afterStart});
                beforeStart = bIdx;
                beforeEnd = bIdx + 1;
                afterStart = aIdx;
                afterEnd = aIdx + 1;
            }
        }

        if (beforeStart != -1) {
            hunks.add(new int[]{beforeStart, beforeEnd - beforeStart, afterStart, afterEnd - afterStart});
        }

        return hunks;
    }

    private static String toRelative(String path) {
        // Make paths look like typical diff paths
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
