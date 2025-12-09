package com.github.jdami.aicommit.actions;

import com.github.jdami.aicommit.service.OllamaService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.openapi.util.IconLoader;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Action to generate commit message using Ollama AI
 */
public class GenerateCommitMessageAction extends AnAction {

    private OllamaService ollamaService;
    private volatile boolean isGenerating;
    private volatile boolean wasCancelled;
    private volatile ProgressIndicator currentIndicator;
    private final Object stateLock = new Object();

    private OllamaService getOllamaService() {
        if (ollamaService == null) {
            ollamaService = new OllamaService();
        }
        return ollamaService;
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
        if (changes.isEmpty()) {
            Messages.showWarningDialog(project, "No changes selected for commit", "Warning");
            return;
        }

        // Generate diff content in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Commit Message...", true) {
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
                    String diffContent = getDiffContent(project, changes);

                    if (diffContent == null || diffContent.trim().isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(
                                () -> Messages.showWarningDialog(project, "No diff content found", "Warning"));
                        return;
                    }

                    indicator.setText("Calling Ollama service...");
                    indicator.setFraction(0.6);
                    indicator.checkCanceled();

                    // Call Ollama service
                    generatedMessage = getOllamaService().generateCommitMessage(diffContent, indicator);

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
     * Get diff content from changes using git diff command
     */
    private String getDiffContent(Project project, Collection<Change> changes) {
        try {
            GitRepository repository = GitUtil.getRepositoryManager(project).getRepositories().stream()
                    .findFirst()
                    .orElse(null);

            if (repository == null) {
                System.err.println("No Git repository found");
                return null;
            }

            String repoPath = repository.getRoot().getPath();
            StringBuilder diffBuilder = new StringBuilder();

            // Use git diff to get actual diff content
            for (Change change : changes) {
                String absolutePath = null;

                if (change.getAfterRevision() != null) {
                    absolutePath = change.getAfterRevision().getFile().getPath();
                } else if (change.getBeforeRevision() != null) {
                    absolutePath = change.getBeforeRevision().getFile().getPath();
                }

                if (absolutePath != null) {
                    // Convert to relative path
                    String relativePath = absolutePath;
                    if (absolutePath.startsWith(repoPath)) {
                        relativePath = absolutePath.substring(repoPath.length());
                        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                            relativePath = relativePath.substring(1);
                        }
                    }

                    System.out.println("Processing file: " + relativePath);

                    try {
                        String fileDiff = null;

                        // Try 1: Staged changes (git diff --cached)
                        fileDiff = executeGitDiff(repoPath, "diff", "--cached", "--", relativePath);

                        // Try 2: Unstaged changes (git diff)
                        if (fileDiff == null || fileDiff.isEmpty()) {
                            fileDiff = executeGitDiff(repoPath, "diff", "--", relativePath);
                        }

                        // Try 3: Compare with HEAD (for new/untracked files or modified files)
                        if (fileDiff == null || fileDiff.isEmpty()) {
                            fileDiff = executeGitDiff(repoPath, "diff", "HEAD", "--", relativePath);
                        }

                        if (fileDiff != null && !fileDiff.isEmpty()) {
                            diffBuilder.append(fileDiff);
                        } else {
                            System.err.println("No diff found for file: " + relativePath);
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting diff for file " + relativePath + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            String finalDiff = diffBuilder.toString();
            System.out.println("=== Git Diff Content ===");
            System.out.println("Diff length: " + finalDiff.length() + " characters");
            System.out.println(finalDiff);
            System.out.println("========================");

            return finalDiff.isEmpty() ? null : finalDiff;
        } catch (Exception e) {
            System.err.println("Error getting diff content: " + e.getMessage());
            e.printStackTrace();
            return "Unable to get diff content: " + e.getMessage();
        }
    }

    /**
     * Execute git diff command and return output
     */
    private String executeGitDiff(String repoPath, String... args) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(args);
        processBuilder.directory(new java.io.File(repoPath));
        processBuilder.redirectErrorStream(true);

        // Prepend "git" to the command
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(java.util.Arrays.asList(args));
        processBuilder.command(command);

        System.out.println("Executing: " + String.join(" ", command));

        Process process = processBuilder.start();
        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        System.out.println("Exit code: " + exitCode + ", Output length: " + output.length());

        return output.toString();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
        e.getPresentation().setIcon(IconLoader.getIcon(
                isGenerating ? "/icons/aiCommitStop.svg" : "/icons/aiCommit.svg",
                GenerateCommitMessageAction.class));
        e.getPresentation().setText(isGenerating ? "停止生成" : "Git助手");
        e.getPresentation().setDescription(isGenerating ? "停止生成 commit message" : "Generate commit message using AI");
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
        getOllamaService().cancelOngoingCall();
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
        getOllamaService().cancelOngoingCall();
    }
}
