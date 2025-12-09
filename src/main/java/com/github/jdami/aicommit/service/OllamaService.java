package com.github.jdami.aicommit.service;

import com.github.jdami.aicommit.settings.OllamaSettingsState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service to interact with Ollama API
 */
public class OllamaService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final Gson gson = new Gson();
    private volatile Call ongoingCall;

    public void cancelOngoingCall() {
        Call call = ongoingCall;
        if (call != null) {
            call.cancel();
        }
    }

    /**
     * Generate commit message based on diff content
     * 
     * @param diffContent The git diff content
     * @return Generated commit message
     * @throws IOException if request fails
     */
    public String generateCommitMessage(@NotNull String diffContent) throws IOException {
        return generateCommitMessage(diffContent, null);
    }

    /**
     * Generate commit message with optional progress indicator for cancellation support
     */
    public String generateCommitMessage(@NotNull String diffContent, @Nullable ProgressIndicator indicator) throws IOException {
        OllamaSettingsState settings = OllamaSettingsState.getInstance();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(settings.timeout, TimeUnit.SECONDS)
                .readTimeout(settings.timeout, TimeUnit.SECONDS)
                .writeTimeout(settings.timeout, TimeUnit.SECONDS)
                .build();

        // Build the prompt
        String userPrompt = buildPrompt(diffContent);

        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", settings.modelName);
        requestBody.addProperty("prompt", userPrompt);
        requestBody.addProperty("system", settings.systemPrompt);
        requestBody.addProperty("stream", false);

        String jsonBody = gson.toJson(requestBody);

        // Log request information
        System.out.println("=== Ollama Request ===");
        System.out.println("Endpoint: " + settings.ollamaEndpoint + "/api/generate");
        System.out.println("Model: " + settings.modelName);
        System.out.println("System Prompt: " + settings.systemPrompt);
        System.out.println("User Prompt: " + userPrompt);
        System.out.println("Full Request Body: " + jsonBody);
        System.out.println("=====================");

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(settings.ollamaEndpoint + "/api/generate")
                .post(body)
                .build();

        Call call = client.newCall(request);
        ongoingCall = call;

        try {
            if (indicator != null && indicator.isCanceled()) {
                throw new ProcessCanceledException();
            }

            try (Response response = call.execute()) {
                if (indicator != null && indicator.isCanceled()) {
                    throw new ProcessCanceledException();
                }
                if (!response.isSuccessful()) {
                    String errorMsg = "Unexpected response code: " + response;
                    System.err.println("Ollama Error: " + errorMsg);
                    throw new IOException(errorMsg);
                }

                String responseBody = response.body().string();
                System.out.println("=== Ollama Response ===");
                System.out.println("Raw Response: " + responseBody);

                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (jsonResponse.has("response")) {
                    String rawResponse = jsonResponse.get("response").getAsString().trim();
                    System.out.println("Raw Message: " + rawResponse);

                    // Remove <think>...</think> tags and their content
                    String cleanedResponse = removeThinkTags(rawResponse);
                    
                    // Post-process to extract only the commit message
                    String finalMessage = extractCommitMessage(cleanedResponse);
                    System.out.println("Final Message: " + finalMessage);
                    System.out.println("======================");

                    return finalMessage;
                } else {
                    String errorMsg = "Invalid response format from Ollama";
                    System.err.println("Ollama Error: " + errorMsg);
                    throw new IOException(errorMsg);
                }
            }
        } catch (ProcessCanceledException canceled) {
            throw canceled;
        } catch (IOException ex) {
            if (indicator != null && indicator.isCanceled()) {
                throw new ProcessCanceledException();
            }
            throw ex;
        } finally {
            ongoingCall = null;
        }
    }

    /**
     * Remove <think>...</think> tags and their content from the response
     */
    private String removeThinkTags(String text) {
        if (text == null) {
            return "";
        }
        // Remove <think>...</think> tags and content (case-insensitive, handles
        // multiline)
        // (?i) enables case-insensitivity
        // (?s) enables dotall mode (dot matches newlines)
        return text.replaceAll("(?is)<think>.*?</think>", "").trim();
    }

    /**
     * Extract only the commit message from the response, removing any explanatory text
     */
    private String extractCommitMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text.trim();
        
        // Remove common unwanted prefixes
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
                // Remove any following colons or punctuation
                cleaned = cleaned.replaceFirst("^[:\\-\\s]+", "").trim();
            }
        }
        
        // Remove markdown code blocks
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "").trim();
        cleaned = cleaned.replaceAll("```.*", "").trim();
        
        // Remove explanatory sentences at the end
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
        
        // Split by lines and find the actual commit message
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundCommitStart = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Check if this line looks like a commit message start
            if (!foundCommitStart && isCommitMessageLine(line)) {
                foundCommitStart = true;
                result.append(line).append("\n");
            } else if (foundCommitStart) {
                // If we've found the start, include lines that are part of the commit message
                if (line.startsWith("- ") || isCommitMessageLine(line)) {
                    result.append(line).append("\n");
                } else if (line.toLowerCase().contains("this message") || 
                          line.toLowerCase().contains("you can use") ||
                          line.toLowerCase().contains("analysis")) {
                    // Stop if we hit explanatory text
                    break;
                }
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Check if a line looks like a commit message line
     */
    private boolean isCommitMessageLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        line = line.trim();
        
        // Check if it starts with a commit type
        String[] commitTypes = {"feat(", "fix(", "docs(", "style(", "refactor(", "test(", "chore("};
        for (String type : commitTypes) {
            if (line.toLowerCase().startsWith(type)) {
                return true;
            }
        }
        
        // Check if it's a bullet point
        if (line.startsWith("- ")) {
            return true;
        }
        
        return false;
    }

    /**
     * Build the prompt for commit message generation
     */
    private String buildPrompt(@NotNull String diffContent) {
        return "Analyze the following git diff and generate a commit message based on the system instructions.\n\n" +
                "Git Diff:\n" +
                diffContent + "\n\n" +
                "Generate the commit message now:";
    }

    /**
     * Test connection to Ollama service
     * 
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try {
            OllamaSettingsState settings = OllamaSettingsState.getInstance();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(settings.ollamaEndpoint + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
