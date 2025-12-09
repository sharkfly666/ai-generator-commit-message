package com.github.jdami.aicommit.settings;

import com.github.jdami.aicommit.settings.AiSettingsState.Provider;
import com.github.jdami.aicommit.settings.model.ProviderSettings;
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
    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
    private final JTextArea systemPromptArea = new JTextArea(5, 40);

    public AiSettingsComponent() {
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(systemPromptArea);

        providerCards.add(buildOllamaPanel(), Provider.OLLAMA.name());
        providerCards.add(buildOpenAiPanel(), Provider.OPENAI.name());

        mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Provider: "), providerCombo, 1, false)
                .addComponent(providerCards)
                .addLabeledComponent(new JBLabel("Timeout (seconds): "), timeoutSpinner, 1, false)
                .addLabeledComponent(new JBLabel("System Prompt: "), scrollPane, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));

        providerCombo.addActionListener(e -> switchProviderCard());
    }

    private JPanel buildOllamaPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Ollama Endpoint: "), ollamaEndpointField, 1, false)
                .addLabeledComponent(new JBLabel("Ollama Model: "), ollamaModelField, 1, false)
                .getPanel();
    }

    private JPanel buildOpenAiPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OpenAI API Base: "), openAiEndpointField, 1, false)
                .addLabeledComponent(new JBLabel("OpenAI Model: "), openAiModelField, 1, false)
                .addLabeledComponent(new JBLabel("OpenAI API Key: "), openAiApiKeyField, 1, false)
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

    public void setTimeout(int timeout) {
        timeoutSpinner.setValue(timeout);
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

    private void switchProviderCard() {
        CardLayout layout = (CardLayout) providerCards.getLayout();
        Provider provider = getProvider() != null ? getProvider() : Provider.OLLAMA;
        layout.show(providerCards, provider.name());
    }
}
