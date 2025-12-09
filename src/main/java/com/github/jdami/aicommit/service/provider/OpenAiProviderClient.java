package com.github.jdami.aicommit.service.provider;

import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.github.jdami.aicommit.service.util.CommitMessageCleaner;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

/**
 * Provider client for OpenAI-compatible APIs.
 */
public class OpenAiProviderClient extends BaseHttpProviderClient {

    @Override
    public String generate(GenerationInputs inputs, ProgressIndicator indicator) throws IOException {
        var client = buildClient(inputs);

        com.google.gson.JsonObject systemMessage = new com.google.gson.JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", inputs.systemPrompt);

        com.google.gson.JsonObject userMessage = new com.google.gson.JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", inputs.prompt);

        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        com.google.gson.JsonObject requestBody = new com.google.gson.JsonObject();
        requestBody.addProperty("model", inputs.model);
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);

        String url = normalizeBaseUrl(inputs.endpoint) + "/v1/chat/completions";
        String jsonBody = gson.toJson(requestBody);

        System.out.println("=== OpenAI Request ===");
        System.out.println("Endpoint: " + url);
        System.out.println("Model: " + inputs.model);
        System.out.println("System Prompt: " + inputs.systemPrompt);
        System.out.println("User Prompt: " + inputs.prompt);
        System.out.println("Full Request Body: " + jsonBody);
        System.out.println("=====================");

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + inputs.apiKey)
                .build();

        Call call = client.newCall(request);
        ongoingCall = call;

        try {
            checkCanceled(indicator);

            try (Response response = call.execute()) {
                checkCanceled(indicator);

                if (!response.isSuccessful()) {
                    String errorMsg = "Unexpected response code: " + response;
                    System.err.println("OpenAI Error: " + errorMsg);
                    throw new IOException(errorMsg);
                }

                String responseBody = response.body().string();
                System.out.println("=== OpenAI Response ===");
                System.out.println("Raw Response: " + responseBody);

                var jsonResponse = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
                if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()
                        && jsonResponse.getAsJsonArray("choices").size() > 0) {
                    var firstChoice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                    var message = firstChoice.has("message") ? firstChoice.getAsJsonObject("message") : null;
                    String rawResponse = message != null && message.has("content")
                            ? message.get("content").getAsString().trim()
                            : null;

                    if (rawResponse == null) {
                        throw new IOException("Invalid response format from OpenAI: missing content");
                    }

                    System.out.println("Raw Message: " + rawResponse);

                    String finalMessage = CommitMessageCleaner.clean(rawResponse);
                    System.out.println("Final Message: " + finalMessage);
                    System.out.println("======================");

                    return finalMessage;
                } else {
                    String errorMsg = "Invalid response format from OpenAI";
                    System.err.println("OpenAI Error: " + errorMsg);
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
}
