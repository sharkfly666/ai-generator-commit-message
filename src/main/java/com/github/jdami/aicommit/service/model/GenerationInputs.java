package com.github.jdami.aicommit.service.model;

/**
 * Provider-agnostic inputs required for generating a commit message.
 */
public class GenerationInputs {
    public final String prompt;
    public final String systemPrompt;
    public final String endpoint;
    public final String model;
    public final String apiKey;
    public final int timeoutSeconds;

    public GenerationInputs(String prompt,
                            String systemPrompt,
                            String endpoint,
                            String model,
                            String apiKey,
                            int timeoutSeconds) {
        this.prompt = prompt;
        this.systemPrompt = systemPrompt;
        this.endpoint = endpoint;
        this.model = model;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
    }
}
