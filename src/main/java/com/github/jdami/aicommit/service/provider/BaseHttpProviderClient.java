package com.github.jdami.aicommit.service.provider;

import com.github.jdami.aicommit.service.AiProviderClient;
import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.google.gson.Gson;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Common HTTP utilities for provider clients.
 */
public abstract class BaseHttpProviderClient implements AiProviderClient {

    protected static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    protected final Gson gson = new Gson();
    protected volatile Call ongoingCall;

    @Override
    public void cancel() {
        Call call = ongoingCall;
        if (call != null) {
            call.cancel();
        }
    }

    protected OkHttpClient buildClient(GenerationInputs inputs) {
        return new OkHttpClient.Builder()
                .connectTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    protected void checkCanceled(@Nullable ProgressIndicator indicator) {
        if (indicator != null && indicator.isCanceled()) {
            throw new ProcessCanceledException();
        }
    }

    protected String normalizeBaseUrl(String base) {
        if (base == null || base.isEmpty()) {
            return "";
        }
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }
}
