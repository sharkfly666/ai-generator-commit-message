package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.settings.AiSettingsState.ContextWindowPreset;
import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.github.jdami.aicommit.service.provider.OllamaProviderClient;
import com.github.jdami.aicommit.service.provider.OpenAiProviderClient;
import com.github.jdami.aicommit.service.provider.OpenRouterProviderClient;
import com.github.jdami.aicommit.settings.AiSettingsState.Provider;
import com.github.jdami.aicommit.settings.model.ProviderSettings;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * UI component for Ollama settings
 */
public class AiSettingsComponent {

    private final JPanel mainPanel;
    private final JComboBox<Provider> providerCombo = new JComboBox<>(Provider.values());
    private final JPanel providerCards = new JPanel(new CardLayout());
    private final JBTextField ollamaEndpointField = new JBTextField();
    private final JBTextField ollamaModelField = new JBTextField();
    private final JBTextField openAiEndpointField = new JBTextField();
    private final JBTextField openAiModelField = new JBTextField();
    private final JBPasswordField openAiApiKeyField = new JBPasswordField();
    private final JBTextField openRouterEndpointField = new JBTextField();
    private final JBTextField openRouterModelField = new JBTextField();
    private final JBPasswordField openRouterApiKeyField = new JBPasswordField();
    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
    private final JComboBox<ContextWindowPreset> contextWindowCombo = new JComboBox<>(ContextWindowPreset.values());
    private final JTextArea systemPromptArea = new JTextArea(5, 40);

    private final JCheckBox proxyEnabledCheckBox = new JCheckBox("启用 HTTP 代理");
    private final JBTextField proxyHostField = new JBTextField();
    private final JSpinner proxyPortSpinner = new JSpinner(new SpinnerNumberModel(7890, 1, 65535, 1));

    private JBLabel createLabel(String text) {
        JBLabel label = new JBLabel(text);
        // Set a consistent width to ensure alignment across different FormBuilders
        // We must use the label's natural height, passing -1 is invalid for Dimension
        Dimension naturalSize = label.getPreferredSize();
        label.setPreferredSize(new Dimension(JBUI.scale(120), naturalSize.height));
        return label;
    }

    public AiSettingsComponent() {
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new com.intellij.ui.components.JBScrollPane(systemPromptArea);

        providerCards.add(buildOllamaPanel(), Provider.OLLAMA.name());
        providerCards.add(buildOpenAiPanel(), Provider.OPENAI.name());
        providerCards.add(buildOpenRouterPanel(), Provider.OPENROUTER.name());

        // Action Panel: Spinner + Test Button
        // We use a panel to hold them together
        JPanel timeoutAndTestPanel = new JPanel(new BorderLayout());
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerPanel.add(timeoutSpinner);
        
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(getProvider())); // Dynamically get current provider

        timeoutAndTestPanel.add(spinnerPanel, BorderLayout.WEST);
        timeoutAndTestPanel.add(testButton, BorderLayout.EAST);

        // Context Window Limit Panel
        JPanel contextWindowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contextWindowCombo.setSelectedItem(ContextWindowPreset.SMALL_8K); // Default
        contextWindowPanel.add(contextWindowCombo);

        // Create release link panel
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel linkLabel = new JLabel("<html>插件发布地址: <a href='https://linux.do/t/topic/1415731/65'>LINUX.DO</a></html>");
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI("https://linux.do/t/topic/1415731/65"));
                } catch (Exception ex) {
                    Messages.showErrorDialog("无法打开链接: " + ex.getMessage(), "错误");
                }
            }
        });
        linkPanel.add(linkLabel);

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("AI Provider: "), providerCombo, 1, false)
                .addVerticalGap(5)
                .addComponent(new com.intellij.ui.TitledSeparator("Provider Settings"))
                .addComponent(providerCards)
                .addLabeledComponent(createLabel("Timeout(s): "), timeoutAndTestPanel, 1, false)
                .addVerticalGap(5)
                .addComponent(new com.intellij.ui.TitledSeparator("Network Proxy"))
                .addComponent(proxyEnabledCheckBox)
                .addLabeledComponent(createLabel("代理地址: "), proxyHostField, 1, false)
                .addLabeledComponent(createLabel("代理端口: "), proxyPortSpinner, 1, false)
                .addVerticalGap(5)
                .addComponent(new com.intellij.ui.TitledSeparator("Content Limit Settings"))
                .addLabeledComponent(createLabel("上下文窗口: "), contextWindowPanel, 1, false)
                .addVerticalGap(5)
                .addComponent(new com.intellij.ui.TitledSeparator("Generation Parameters"))
                .addLabeledComponent(createLabel("System Prompt: "), scrollPane, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .addComponent(linkPanel)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));

        providerCombo.addActionListener(e -> switchProviderCard());

        proxyEnabledCheckBox.addActionListener(e -> updateProxyFieldsEnabled());
        updateProxyFieldsEnabled();
    }

    private JPanel createApiKeyPanel(JBPasswordField apiKeyField) {
        // Fix Expansion: Set columns to limit preferred width
        apiKeyField.setColumns(30);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(apiKeyField, BorderLayout.CENTER);
        
        JCheckBox showPassword = new JCheckBox("Show");
        showPassword.addActionListener(e -> {
            char echoChar = showPassword.isSelected() ? 0 : '•';
            apiKeyField.setEchoChar(echoChar);
        });
        panel.add(showPassword, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildOllamaPanel() {
        JBLabel hintLabel = new JBLabel("提示: 如果 URL 以 # 结尾，将直接使用该地址作为完整请求 URL (不拼接 /api/generate)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), ollamaEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), ollamaModelField, 1, false)
                .getPanel();
    }

    private JPanel buildOpenAiPanel() {
        JBLabel hintLabel = new JBLabel("提示: 如果 URL 以 # 结尾，将直接使用该地址作为完整请求 URL (不拼接 /v1/chat/completions)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), openAiEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), openAiModelField, 1, false)
                .addLabeledComponent(createLabel("API Key: "), createApiKeyPanel(openAiApiKeyField), 1, false)
                .getPanel();
    }

    private JPanel buildOpenRouterPanel() {
        JBLabel hintLabel = new JBLabel("提示: 如果 URL 以 # 结尾，将直接使用该地址作为完整请求 URL (不拼接 /v1/chat/completions)");
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setFont(JBUI.Fonts.smallFont());

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(createLabel("Endpoint URL: "), openRouterEndpointField, 1, false)
                .addComponentToRightColumn(hintLabel)
                .addLabeledComponent(createLabel("Model Name: "), openRouterModelField, 1, false)
                .addLabeledComponent(createLabel("API Key: "), createApiKeyPanel(openRouterApiKeyField), 1, false)
                .getPanel();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public String getOllamaEndpoint() {
        return ollamaEndpointField.getText() != null ? ollamaEndpointField.getText() : "";
    }

    public Provider getProvider() {
        return (Provider) providerCombo.getSelectedItem();
    }

    public void setProvider(Provider provider) {
        providerCombo.setSelectedItem(provider);
        switchProviderCard();
    }

    public void setProviders(ProviderSettings providers) {
        if (providers == null) {
            return;
        }
        setOllamaEndpoint(providers.ollama != null ? providers.ollama.endpoint : "");
        setOllamaModel(providers.ollama != null ? providers.ollama.model : "");
        setOpenAiEndpoint(providers.openAi != null ? providers.openAi.endpoint : "");
        setOpenAiModel(providers.openAi != null ? providers.openAi.model : "");
        setOpenAiApiKey(providers.openAi != null ? providers.openAi.apiKey : "");
        setOpenRouterEndpoint(providers.openRouter != null ? providers.openRouter.endpoint : "");
        setOpenRouterModel(providers.openRouter != null ? providers.openRouter.model : "");
        setOpenRouterApiKey(providers.openRouter != null ? providers.openRouter.apiKey : "");
    }

    public void setOllamaEndpoint(String endpoint) {
        ollamaEndpointField.setText(endpoint != null ? endpoint : "");
    }

    public String getOllamaModel() {
        return ollamaModelField.getText() != null ? ollamaModelField.getText() : "";
    }

    public void setOllamaModel(String modelName) {
        ollamaModelField.setText(modelName != null ? modelName : "");
    }

    public int getTimeout() {
        return (Integer) timeoutSpinner.getValue();
    }

    public boolean isProxyEnabled() {
        return proxyEnabledCheckBox.isSelected();
    }

    public void setProxyEnabled(boolean enabled) {
        proxyEnabledCheckBox.setSelected(enabled);
        updateProxyFieldsEnabled();
    }

    public String getProxyHost() {
        return proxyHostField.getText() != null ? proxyHostField.getText() : "";
    }

    public void setProxyHost(String host) {
        proxyHostField.setText(host != null ? host : "");
    }

    public int getProxyPort() {
        return (Integer) proxyPortSpinner.getValue();
    }

    public void setProxyPort(int port) {
        int safePort = port > 0 && port <= 65535 ? port : 7890;
        proxyPortSpinner.setValue(safePort);
    }

    private void updateProxyFieldsEnabled() {
        boolean enabled = proxyEnabledCheckBox.isSelected();
        proxyHostField.setEnabled(enabled);
        proxyPortSpinner.setEnabled(enabled);
    }

    public void setTimeout(int timeout) {
        timeoutSpinner.setValue(timeout);
    }

    public int getMaxDiffChars() {
        ContextWindowPreset selected = (ContextWindowPreset) contextWindowCombo.getSelectedItem();
        return selected != null ? selected.getMaxChars() : ContextWindowPreset.SMALL_8K.getMaxChars();
    }

    public void setMaxDiffChars(int maxDiffChars) {
        contextWindowCombo.setSelectedItem(ContextWindowPreset.fromMaxChars(maxDiffChars));
    }

    public String getOpenAiEndpoint() {
        return openAiEndpointField.getText() != null ? openAiEndpointField.getText() : "";
    }

    public void setOpenAiEndpoint(String endpoint) {
        openAiEndpointField.setText(endpoint != null ? endpoint : "");
    }

    public String getOpenAiModel() {
        return openAiModelField.getText() != null ? openAiModelField.getText() : "";
    }

    public void setOpenAiModel(String model) {
        openAiModelField.setText(model != null ? model : "");
    }

    public String getOpenAiApiKey() {
        return openAiApiKeyField.getPassword() != null ? String.valueOf(openAiApiKeyField.getPassword()) : "";
    }

    public void setOpenAiApiKey(String apiKey) {
        openAiApiKeyField.setText(apiKey != null ? apiKey : "");
    }

    public String getSystemPrompt() {
        return systemPromptArea.getText() != null ? systemPromptArea.getText() : "";
    }

    public void setSystemPrompt(String prompt) {
        systemPromptArea.setText(prompt != null ? prompt : "");
    }

    public String getOpenRouterEndpoint() {
        return openRouterEndpointField.getText() != null ? openRouterEndpointField.getText() : "";
    }

    public void setOpenRouterEndpoint(String endpoint) {
        openRouterEndpointField.setText(endpoint != null ? endpoint : "");
    }

    public String getOpenRouterModel() {
        return openRouterModelField.getText() != null ? openRouterModelField.getText() : "";
    }

    public void setOpenRouterModel(String model) {
        openRouterModelField.setText(model != null ? model : "");
    }

    public String getOpenRouterApiKey() {
        return openRouterApiKeyField.getPassword() != null ? String.valueOf(openRouterApiKeyField.getPassword()) : "";
    }

    public void setOpenRouterApiKey(String apiKey) {
        openRouterApiKeyField.setText(apiKey != null ? apiKey : "");
    }

    private void switchProviderCard() {
        CardLayout layout = (CardLayout) providerCards.getLayout();
        Provider provider = getProvider() != null ? getProvider() : Provider.OLLAMA;
        layout.show(providerCards, provider.name());
    }

    private void testConnection(Provider provider) {
        // Get configuration based on provider
        final String endpoint;
        final String model;
        final String apiKey;

        switch (provider) {
            case OLLAMA:
                endpoint = getOllamaEndpoint().trim();
                model = getOllamaModel().trim();
                apiKey = "";
                break;
            case OPENAI:
                endpoint = getOpenAiEndpoint().trim();
                model = getOpenAiModel().trim();
                apiKey = getOpenAiApiKey().trim();
                break;
            case OPENROUTER:
                endpoint = getOpenRouterEndpoint().trim();
                model = getOpenRouterModel().trim();
                apiKey = getOpenRouterApiKey().trim();
                break;
            default:
                throw new IllegalStateException("Unknown provider: " + provider);
        }

        // Read UI values on EDT before entering the background task
        final int timeoutSeconds = getTimeout();
        final boolean testProxyEnabled = isProxyEnabled();
        final String testProxyHost = getProxyHost().trim();
        final int testProxyPort = getProxyPort();

        // Validate inputs
        if (endpoint.isEmpty()) {
            Messages.showErrorDialog("Endpoint cannot be empty", "Test Connection Failed");
            return;
        }
        if (model.isEmpty()) {
            Messages.showErrorDialog("Model cannot be empty", "Test Connection Failed");
            return;
        }
        if ((provider == Provider.OPENAI || provider == Provider.OPENROUTER) && apiKey.isEmpty()) {
            Messages.showErrorDialog("API Key cannot be empty", "Test Connection Failed");
            return;
        }
        if (testProxyEnabled) {
            if (testProxyHost.isEmpty()) {
                Messages.showErrorDialog("Proxy host cannot be empty when proxy is enabled", "Test Connection Failed");
                return;
            }
            if (testProxyHost.contains("://")) {
                Messages.showErrorDialog("Proxy host must not include a scheme (use host only, e.g. 127.0.0.1)", "Test Connection Failed");
                return;
            }
            if (testProxyHost.contains("/") || testProxyHost.contains("?")) {
                Messages.showErrorDialog("Proxy host is invalid: " + testProxyHost, "Test Connection Failed");
                return;
            }
            if (testProxyPort <= 0 || testProxyPort > 65535) {
                Messages.showErrorDialog("Proxy port must be between 1 and 65535", "Test Connection Failed");
                return;
            }
        }

        final java.util.concurrent.atomic.AtomicReference<String> resultRef = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<Exception> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

        boolean finished = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                GenerationInputs inputs = new GenerationInputs(
                        "Test connection",
                        "You are a test assistant. Reply with 'OK' if you receive this message.",
                        endpoint,
                        model,
                        apiKey,
                        timeoutSeconds,
                        testProxyEnabled,
                        testProxyEnabled ? testProxyHost : null,
                        testProxyEnabled ? testProxyPort : 0
                );

                String response;
                switch (provider) {
                    case OLLAMA:
                        response = new OllamaProviderClient().generate(inputs, null);
                        break;
                    case OPENAI:
                        response = new OpenAiProviderClient().generate(inputs, null);
                        break;
                    case OPENROUTER:
                        response = new OpenRouterProviderClient().generate(inputs, null);
                        break;
                    default:
                        throw new IllegalStateException("Unknown provider: " + provider);
                }

                resultRef.set(response);
            } catch (Exception ex) {
                errorRef.set(ex);
            }
        }, "Testing Connection...", true, null);

        if (!finished) {
            // User cancelled the progress dialog
            return;
        }

        if (errorRef.get() != null) {
            Messages.showErrorDialog(
                    "Connection failed: " + errorRef.get().getMessage(),
                    "Test Connection Failed"
            );
            return;
        }

        String finalResponse = resultRef.get();
        if (finalResponse == null) {
            Messages.showErrorDialog("Connection failed: empty response", "Test Connection Failed");
            return;
        }
        if (finalResponse.length() > 100) {
            finalResponse = finalResponse.substring(0, 100) + "...";
        }
        Messages.showInfoMessage(
                "Connection successful!\n\nProvider: " + provider + "\nModel: " + model + "\nResponse: " + finalResponse,
                "Test Connection Successful"
        );
    }
}
