package com.github.jdami.aicommit.startup;

import com.github.jdami.aicommit.settings.AiSettingsState;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Activity to run on project startup to check for plugin updates
 */
public class PluginUpdateActivity implements StartupActivity {

    private static final String PLUGIN_ID = "com.github.jdami.ai-generator-commit-message";

    @Override
    public void runActivity(@NotNull Project project) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        if (plugin == null) {
            return;
        }

        String currentVersion = plugin.getVersion();
        AiSettingsState settings = AiSettingsState.getInstance();

        // If version changed or not set, reset settings to defaults
        if (!currentVersion.equals(settings.pluginVersion)) {
            System.out.println("Plugin updated from " + settings.pluginVersion + " to " + currentVersion
                    + ". Resetting settings.");
            settings.resetToDefaults();
            settings.pluginVersion = currentVersion;
        }
    }
}
