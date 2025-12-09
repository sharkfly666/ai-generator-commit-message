package com.github.jdami.aicommit.service;

import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Abstraction for different AI providers that can generate commit messages.
 */
public interface AiProviderClient {

    /**
     * Generate commit message based on provided inputs.
     */
    String generate(GenerationInputs inputs, @Nullable ProgressIndicator indicator) throws IOException;

    /**
     * Cancel ongoing network call if present.
     */
    void cancel();
}
