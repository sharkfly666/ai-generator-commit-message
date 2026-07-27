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
import java.net.InetSocketAddress;
import java.net.Proxy;
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

    protected OkHttpClient buildClient(GenerationInputs inputs) throws IOException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(inputs.timeoutSeconds, TimeUnit.SECONDS);

        applyProxy(builder, inputs);
        return builder.build();
    }

    private void applyProxy(OkHttpClient.Builder builder, GenerationInputs inputs) throws IOException {
        if (!inputs.proxyEnabled) {
            return;
        }

        String host = inputs.proxyHost != null ? inputs.proxyHost.trim() : "";
        int port = inputs.proxyPort;
        if (host.isEmpty()) {
            throw new IOException("Proxy is enabled but host is empty");
        }
        if (host.contains("://")) {
            throw new IOException("Proxy host must not include a scheme (use host only, e.g. 127.0.0.1)");
        }
        if (host.contains("/") || host.contains("?")) {
            throw new IOException("Proxy host is invalid: " + host);
        }
        if (port <= 0 || port > 65535) {
            throw new IOException("Proxy port must be between 1 and 65535");
        }

        builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
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
