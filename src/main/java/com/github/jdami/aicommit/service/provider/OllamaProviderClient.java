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
 * Provider client for Ollama.
 */
public class OllamaProviderClient extends BaseHttpProviderClient {

    @Override
    public String generate(GenerationInputs inputs, ProgressIndicator indicator) throws IOException {
        var client = buildClient(inputs);

        com.google.gson.JsonObject requestBody = new com.google.gson.JsonObject();
        requestBody.addProperty("model", inputs.model);
        requestBody.addProperty("prompt", inputs.prompt);
        requestBody.addProperty("system", inputs.systemPrompt);
        requestBody.addProperty("stream", false);

        String jsonBody = gson.toJson(requestBody);

        String url = normalizeBaseUrl(inputs.endpoint) + "/api/generate";
        System.out.println("=== Ollama Request ===");
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
                .build();

        Call call = client.newCall(request);
        ongoingCall = call;

        try {
            checkCanceled(indicator);

            try (Response response = call.execute()) {
                checkCanceled(indicator);

                if (!response.isSuccessful()) {
                    String errorMsg = "Unexpected response code: " + response;
                    System.err.println("Ollama Error: " + errorMsg);
                    throw new IOException(errorMsg);
                }

                String responseBody = response.body().string();
                System.out.println("=== Ollama Response ===");
                System.out.println("Raw Response: " + responseBody);

                var jsonResponse = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
                if (jsonResponse.has("response")) {
                    String rawResponse = jsonResponse.get("response").getAsString().trim();
                    System.out.println("Raw Message: " + rawResponse);

                    String finalMessage = CommitMessageCleaner.clean(rawResponse);
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
}
