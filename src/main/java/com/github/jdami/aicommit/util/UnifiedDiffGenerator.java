package com.github.jdami.aicommit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for generating unified diff format from VCS changes.
 * This is VCS-agnostic and works with Git, SVN, Mercurial, etc.
 */
public class UnifiedDiffGenerator {

    /**
     * Generate unified diff from a Change object
     */
    public static String generateDiff(Change change, Project project) throws VcsException {
        ContentRevision before = change.getBeforeRevision();
        ContentRevision after = change.getAfterRevision();

        FilePath filePath = after != null ? after.getFile() : before.getFile();
        String path = filePath.getPath();

        String beforeContent = before != null ? before.getContent() : "";
        String afterContent = after != null ? after.getContent() : "";

        // Determine change type
        boolean isNewFile = before == null;
        boolean isDeletedFile = after == null;

        // Check for binary files (getContent returning null often implies binary or too large)
        // Also check FileType API for robustness
        boolean isBinary = (before != null && beforeContent == null) || 
                           (after != null && afterContent == null) ||
                           (filePath != null && filePath.getFileType().isBinary());

        if (isBinary) {
            String operation = isNewFile ? "New File" : (isDeletedFile ? "Deleted File" : "Modified File");
            String fileType = "Binary";
            if (filePath != null) {
                fileType = filePath.getFileType().getName();
            }
            // Fallback if needed
            if ("UNKNOWN".equals(fileType) || "Binary".equals(fileType)) {
                 int lastDotIndex = path.lastIndexOf('.');
                if (lastDotIndex > 0 && lastDotIndex < path.length() - 1) {
                    fileType = path.substring(lastDotIndex + 1).toUpperCase() + " File";
                }
            }

            return String.format("File: %s (%s)\nOperation: %s\n", path, fileType, operation);
        }

        // Fallback for non-binary nulls (shouldn't happen with logic above, but safety first)
        if (beforeContent == null) beforeContent = "";
        if (afterContent == null) afterContent = "";

        return createUnifiedDiff(path, beforeContent, afterContent, isNewFile, isDeletedFile, filePath, project);
    }

    /**
     * Create unified diff format from file contents
     */
    private static String createUnifiedDiff(String path, String beforeContent, String afterContent,
                                           boolean isNewFile, boolean isDeletedFile, FilePath filePath, Project project) {
        StringBuilder diff = new StringBuilder();

        // Normalize path (use forward slashes)
        String normalizedPath = path.replace('\\', '/');
        
        // Extract relative path using IntelliJ's API
        String relativePath = getRelativePath(normalizedPath, filePath, project);

        // Header
        diff.append("diff --git a/").append(relativePath).append(" b/").append(relativePath).append("\n");

        if (isNewFile) {
            diff.append("new file mode 100644\n");
        } else if (isDeletedFile) {
            diff.append("deleted file mode 100644\n");
        }

        // File paths
        String beforePath = isNewFile ? "/dev/null" : "a/" + relativePath;
        String afterPath = isDeletedFile ? "/dev/null" : "b/" + relativePath;
        diff.append("--- ").append(beforePath).append("\n");
        diff.append("+++ ").append(afterPath).append("\n");

        // Generate diff hunks
        String[] beforeLines = beforeContent.split("\\r?\\n", -1);
        String[] afterLines = afterContent.split("\\r?\\n", -1);

        List<DiffHunk> hunks = computeDiffHunks(beforeLines, afterLines);
        for (DiffHunk hunk : hunks) {
            diff.append(hunk.toString());
        }

        return diff.toString();
    }

    /**
     * Compute diff hunks using simple line-based comparison
     */
    private static List<DiffHunk> computeDiffHunks(String[] beforeLines, String[] afterLines) {
        List<DiffHunk> hunks = new ArrayList<>();

        // Simple implementation: treat entire file as one hunk
        // This is sufficient for AI models and easier to implement
        DiffHunk hunk = new DiffHunk(1, beforeLines.length, 1, afterLines.length);

        // For simplicity, show all before lines as removed and all after lines as added
        // A more sophisticated implementation would use Myers diff algorithm
        if (beforeLines.length > 0 || afterLines.length > 0) {
            for (String line : beforeLines) {
                hunk.addLine("-" + line);
            }
            for (String line : afterLines) {
                hunk.addLine("+" + line);
            }
            hunks.add(hunk);
        }

        return hunks;
    }

    /**
     * Extract relative path from absolute path using IntelliJ's API
     */
    private static String getRelativePath(String path, FilePath filePath, Project project) {
        // Try to get project base path and calculate relative path
        if (project != null && project.getBasePath() != null) {
            String basePath = project.getBasePath();
            if (path.startsWith(basePath)) {
                String relative = path.substring(basePath.length());
                if (relative.startsWith("/") || relative.startsWith("\\")) {
                    relative = relative.substring(1);
                }
                return relative.replace('\\', '/');
            }
        }
        
        // Fallback: try using VirtualFile if available
        if (filePath != null && filePath.getVirtualFile() != null) {
            VirtualFile vf = filePath.getVirtualFile();
            Project guessedProject = ProjectUtil.guessProjectForFile(vf);
            if (guessedProject != null && guessedProject.getBasePath() != null) {
                String basePath = guessedProject.getBasePath();
                if (path.startsWith(basePath)) {
                    String relative = path.substring(basePath.length());
                    if (relative.startsWith("/") || relative.startsWith("\\")) {
                        relative = relative.substring(1);
                    }
                    return relative.replace('\\', '/');
                }
            }
        }
        
        // Final fallback: return normalized path as-is
        return path.replace('\\', '/');
    }

    /**
     * Represents a diff hunk in unified format
     */
    private static class DiffHunk {
        private final int beforeStart;
        private final int beforeCount;
        private final int afterStart;
        private final int afterCount;
        private final List<String> lines = new ArrayList<>();

        public DiffHunk(int beforeStart, int beforeCount, int afterStart, int afterCount) {
            this.beforeStart = beforeStart;
            this.beforeCount = beforeCount;
            this.afterStart = afterStart;
            this.afterCount = afterCount;
        }

        public void addLine(String line) {
            lines.add(line);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("@@ -").append(beforeStart).append(",").append(beforeCount)
              .append(" +").append(afterStart).append(",").append(afterCount)
              .append(" @@\n");
            for (String line : lines) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
