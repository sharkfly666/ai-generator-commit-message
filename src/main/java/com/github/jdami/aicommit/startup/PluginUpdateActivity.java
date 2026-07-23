package com.github.jdami.aicommit.startup;

import com.github.jdami.aicommit.settings.AiSettingsState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import kotlin.coroutines.Continuation;

/**
 * Activity to run on project startup to initialize plugin state.
 * Implements both StartupActivity (legacy) and ProjectActivity (new) for compatibility.
 */
public class PluginUpdateActivity implements StartupActivity, ProjectActivity {

    private static final String PLUGIN_VERSION = "1.1.0";

    @Override
    public void runActivity(@NotNull Project project) {
        run(project);
    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        run(project);
        return Unit.INSTANCE;
    }

    private void run(Project project) {
        AiSettingsState settings = AiSettingsState.getInstance();
        if (settings == null) return;

        // Track first-run detection by ensuring pluginVersion is set
        if (settings.pluginVersion == null || settings.pluginVersion.isEmpty()) {
            settings.pluginVersion = PLUGIN_VERSION;
        }
    }
}
