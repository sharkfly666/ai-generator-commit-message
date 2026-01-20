package com.github.jdami.aicommit.actions;

import com.github.jdami.aicommit.service.AiService;
import com.github.jdami.aicommit.util.UnifiedDiffGenerator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Action to generate commit message using AI.
 * This action is VCS-agnostic and works with Git, SVN, Mercurial, etc.
 */
public class GenerateCommitMessageAction extends AnAction {

    private AiService aiService;
    private volatile boolean isGenerating;
    private volatile boolean wasCancelled;
    private volatile ProgressIndicator currentIndicator;
    private final Object stateLock = new Object();

    private AiService getAiService() {
        if (aiService == null) {
            aiService = new AiService();
        }
        return aiService;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        if (isGenerating) {
            cancelGeneration();
            return;
        }

        // Get the commit panel
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (!(data instanceof CheckinProjectPanel)) {
            Messages.showErrorDialog(project, "Unable to access commit panel", "Error");
            return;
        }

        CheckinProjectPanel checkinPanel = (CheckinProjectPanel) data;
        CommitMessageI commitMessageI = (CommitMessageI) checkinPanel;

        // Get the changes to be committed
        Collection<Change> changes = checkinPanel.getSelectedChanges();
        
        // Get unversioned files selected for commit (new files not yet added to VCS)
        // Use FileStatusManager to check file status (compatible with all IntelliJ versions)
        com.intellij.openapi.vcs.FileStatusManager fileStatusManager = 
            com.intellij.openapi.vcs.FileStatusManager.getInstance(project);
        
        // Get all selected files and filter for unversioned ones
        Collection<VirtualFile> selectedVirtualFiles = checkinPanel.getVirtualFiles();
        List<FilePath> unversionedFiles = selectedVirtualFiles.stream()
            .filter(vf -> {
                // Check if file is unversioned (not in any change and has UNKNOWN status)
                com.intellij.openapi.vcs.FileStatus status = fileStatusManager.getStatus(vf);
                boolean isUnversioned = status == com.intellij.openapi.vcs.FileStatus.UNKNOWN;
                
                // Also check it's not already in the changes collection
                String vfPath = vf.getPath();
                boolean notInChanges = changes.stream().noneMatch(c ->
                    (c.getAfterRevision() != null && c.getAfterRevision().getFile().getPath().equals(vfPath)) ||
                    (c.getBeforeRevision() != null && c.getBeforeRevision().getFile().getPath().equals(vfPath))
                );
                
                return isUnversioned && notInChanges;
            })
            .map(VcsUtil::getFilePath)
            .collect(Collectors.toList());
        
        if (changes.isEmpty() && unversionedFiles.isEmpty()) {
            Messages.showWarningDialog(project, "No changes selected for commit", "Warning");
            return;
        }

        // Generate diff content in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating commit message...", true) {
            private String generatedMessage;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                markGenerating(indicator);
                try {
                    indicator.setText("Analyzing changes...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.3);
                    indicator.checkCanceled();

                    // Get diff content
                    String diffContent = getDiffContent(project, changes, unversionedFiles);

                    if (diffContent == null || diffContent.trim().isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(
                                () -> Messages.showWarningDialog(project, "No diff content found", "Warning"));
                        return;
                    }

                    indicator.setText("Calling Ai service...");
                    indicator.setFraction(0.6);
                    indicator.checkCanceled();

                    // Call Ai service
                    generatedMessage = getAiService().generateCommitMessage(diffContent, indicator);

                    indicator.setFraction(1.0);

                } catch (ProcessCanceledException canceled) {
                    wasCancelled = true;
                    System.out.println("Commit message generation canceled by user.");
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                            "Failed to generate commit message: " + ex.getMessage(),
                            "Error"));
                }
            }

            @Override
            public void onSuccess() {
                if (generatedMessage != null && !generatedMessage.isEmpty() && !wasCancelled) {
                    ApplicationManager.getApplication()
                            .invokeLater(() -> commitMessageI.setCommitMessage(generatedMessage));
                }
            }

            @Override
            public void onCancel() {
                wasCancelled = true;
                System.out.println("Commit message generation canceled.");
            }

            @Override
            public void onFinished() {
                markIdle();
            }
        });
    }

    /**
     * Get diff content from changes using VCS-agnostic API.
     * This works with Git, SVN, Mercurial, and any other VCS supported by IntelliJ.
     */
    private String getDiffContent(Project project, Collection<Change> changes, List<FilePath> unversionedFiles) {
        StringBuilder diffBuilder = new StringBuilder();

        // Process versioned changes using ContentRevision API (VCS-agnostic)
        for (Change change : changes) {
            try {
                String diff = UnifiedDiffGenerator.generateDiff(change, project);
                if (!diff.isEmpty()) {
                    diffBuilder.append(diff);
                    System.out.println("Successfully generated diff for change");
                }
            } catch (VcsException e) {
                System.err.println("Error generating diff: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Process unversioned files (new files not yet added to VCS)
        for (FilePath filePath : unversionedFiles) {
            try {
                String diff = generateDiffForUnversionedFile(filePath);
                if (!diff.isEmpty()) {
                    diffBuilder.append(diff);
                    System.out.println("Successfully generated diff for unversioned file: " + filePath.getPath());
                }
            } catch (Exception e) {
                System.err.println("Error reading unversioned file " + filePath.getPath() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        String finalDiff = diffBuilder.toString();
        System.out.println("=== Diff Content ===");
        System.out.println("Diff length: " + finalDiff.length() + " characters");
        System.out.println(finalDiff);
        System.out.println("====================");

        return finalDiff.isEmpty() ? null : finalDiff;
    }

    /**
     * Generate unified diff for an unversioned file
     */
    private String generateDiffForUnversionedFile(FilePath filePath) throws Exception {
        String absolutePath = filePath.getPath();
        String relativePath = normalizePathForDiff(absolutePath);

        // Read file content directly from filesystem
        File file = new File(absolutePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("Unversioned file not found or not a file: " + absolutePath);
            return "";
        }

        // Check if file is binary using IntelliJ's FileType API
        if (filePath.getFileType().isBinary()) {
            String fileType = filePath.getFileType().getName();
             // If generic "UNKNOWN", try extension
            if ("UNKNOWN".equals(fileType)) {
                 int lastDotIndex = absolutePath.lastIndexOf('.');
                if (lastDotIndex > 0 && lastDotIndex < absolutePath.length() - 1) {
                    fileType = absolutePath.substring(lastDotIndex + 1).toUpperCase() + " File";
                }
            }
            return String.format("File: %s (%s)\nOperation: New File\n", relativePath, fileType);
        }

        try {
            // Read with explicit UTF-8 encoding for cross-platform compatibility
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            // Split on both Unix (\n) and Windows (\r\n) line endings
            String[] lines = content.split("\\r?\\n", -1);

            // Format as unified diff (all lines are additions)
            StringBuilder diff = new StringBuilder();
            diff.append("diff --git a/").append(relativePath).append(" b/").append(relativePath).append("\n");
            diff.append("new file mode 100644\n");
            diff.append("--- /dev/null\n");
            diff.append("+++ b/").append(relativePath).append("\n");
            diff.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");

            for (String line : lines) {
                diff.append("+").append(line).append("\n");
            }
            return diff.toString();

        } catch (java.nio.charset.MalformedInputException e) {
            // Binary file detected
            String fileType = "Binary";
            int lastDotIndex = absolutePath.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < absolutePath.length() - 1) {
                fileType = absolutePath.substring(lastDotIndex + 1).toUpperCase() + " File";
            }
            return String.format("File: %s (%s)\nOperation: New File\n", relativePath, fileType);
        } catch (Exception e) {
            System.err.println("Error reading file " + absolutePath + ": " + e.getMessage());
            // Fallback for other errors
            return String.format("File: %s (Unknown Type)\nOperation: New File\nNote: Error reading content\n", relativePath);
        }
    }

    /**
     * Normalize path for diff format (always use forward slashes)
     */
    private String normalizePathForDiff(String path) {
        // Replace backslashes with forward slashes for Windows compatibility
        return path.replace('\\', '/');
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
        e.getPresentation().setIcon(IconLoader.getIcon(
                isGenerating ? "/icons/aiCommitStop.svg" : "/icons/aiCommit.svg",
                GenerateCommitMessageAction.class));
        e.getPresentation().setText(isGenerating ? "停止生成" : "Commit助手");
        e.getPresentation().setDescription(isGenerating ? "停止生成 commit message" : "使用AI生成 commit message");
    }

    private void markGenerating(ProgressIndicator indicator) {
        synchronized (stateLock) {
            isGenerating = true;
            wasCancelled = false;
            currentIndicator = indicator;
        }
    }

    private void markIdle() {
        synchronized (stateLock) {
            isGenerating = false;
            currentIndicator = null;
        }
        getAiService().cancelOngoingCall();
    }

    private void cancelGeneration() {
        synchronized (stateLock) {
            if (!isGenerating) {
                return;
            }
            if (currentIndicator != null) {
                currentIndicator.cancel();
            }
            wasCancelled = true;
        }
        getAiService().cancelOngoingCall();
    }
}
