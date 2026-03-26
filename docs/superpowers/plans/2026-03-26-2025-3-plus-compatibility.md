# 2025.3+ Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the plugin to the JetBrains 2025.3+ toolchain, explicitly support IntelliJ IDEA 2026.1, and keep the existing commit-message generation behavior intact.

**Architecture:** Replace the legacy Gradle IntelliJ Plugin 1.x build with IntelliJ Platform Gradle Plugin 2.x on Java 21 and Gradle 8.14.4. Keep the runtime code mostly unchanged, but remove the Kotlin-based startup bridge so the project can build as a pure Java plugin against the 2025.3+ platform, then verify compatibility with explicit verifier runs for 2025.3 and 2026.1.

**Tech Stack:** Java 21, Gradle 8.14.4, IntelliJ Platform Gradle Plugin 2.12.0, IntelliJ IDEA 2025.3 and 2026.1, JUnit Jupiter 5.10.2

---

## File Structure

- `build.gradle.kts`: Root build logic; migrate to IntelliJ Platform Gradle Plugin 2.x, define Java 21 toolchain, test execution, and plugin verifier targets.
- `gradle.properties`: Single source of plugin version, platform version, and build compatibility metadata.
- `gradle/wrapper/gradle-wrapper.properties`: Pin the wrapper to Gradle 8.14.4.
- `src/main/java/com/github/jdami/aicommit/startup/PluginUpdateActivity.java`: Startup entrypoint; simplify to a Java-only `StartupActivity`.
- `src/main/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizer.java`: New helper to isolate plugin-version synchronization logic for unit testing.
- `src/test/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizerTest.java`: Regression tests for the new helper.
- `src/main/resources/META-INF/plugin.xml`: Update change notes for the compatibility release while keeping extension registration stable.
- `.github/workflows/release.yml`: Build and verify the plugin with Java 21 in CI.
- `README.md`: Document the new minimum IDE requirement and Java 21 build baseline.

### Task 1: Migrate the Build Baseline to 2025.3+

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle.kts`
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Test: `./gradlew buildPlugin --stacktrace`

- [ ] **Step 1: Rewrite `gradle.properties` with a single 2025.3+ metadata source**

Replace the current file contents with:

```properties
pluginGroup = com.github.jdami
pluginName = AI Commit Message Generator
pluginRepositoryUrl = https://github.com/jdami/ai-generator-commit-message
pluginVersion = 1.0.5

pluginSinceBuild = 253
pluginUntilBuild = 261.*

platformVersion = 2025.3
javaVersion = 21
gradleVersion = 8.14.4

org.gradle.configuration-cache = true
org.gradle.caching = true
```

- [ ] **Step 2: Rewrite `build.gradle.kts` for IntelliJ Platform Gradle Plugin 2.x**

Replace the current file contents with:

```kotlin
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.12.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")

    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdea, providers.gradleProperty("platformVersion"))
        pluginVerifier()
        zipSigner()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(providers.gradleProperty("javaVersion").get().toInt())
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2025.3")
            create(IntelliJPlatformType.IntellijIdea, "2026.1")
        }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = providers.gradleProperty("javaVersion").get().toInt()
    }

    test {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 3: Update the Gradle wrapper to 8.14.4**

Replace `gradle/wrapper/gradle-wrapper.properties` with:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 4: Run the build to capture the next compatibility break**

Run:

```bash
./gradlew buildPlugin --stacktrace
```

Expected: FAIL in `src/main/java/com/github/jdami/aicommit/startup/PluginUpdateActivity.java` because the build no longer brings in Kotlin types and the file still imports `kotlin.Unit` and `kotlin.coroutines.Continuation`.

- [ ] **Step 5: Commit the build baseline migration**

Run:

```bash
git add gradle.properties build.gradle.kts gradle/wrapper/gradle-wrapper.properties
git commit -m "chore(build): migrate to 2025.3+ IntelliJ platform baseline"
```

### Task 2: Remove the Kotlin Startup Bridge and Lock the Behavior with a Unit Test

**Files:**
- Create: `src/main/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizer.java`
- Create: `src/test/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizerTest.java`
- Modify: `src/main/java/com/github/jdami/aicommit/startup/PluginUpdateActivity.java`
- Test: `./gradlew test --tests com.github.jdami.aicommit.startup.PluginVersionSynchronizerTest`
- Test: `./gradlew buildPlugin --stacktrace`

- [ ] **Step 1: Add a failing regression test for plugin-version synchronization**

Create `src/test/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizerTest.java`:

```java
package com.github.jdami.aicommit.startup;

import com.github.jdami.aicommit.settings.AiSettingsState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginVersionSynchronizerTest {

    @Test
    void updatesStoredVersionWhenPluginVersionChanges() {
        AiSettingsState settings = new AiSettingsState();
        settings.pluginVersion = "1.0.4";

        boolean updated = PluginVersionSynchronizer.sync(settings, "1.0.5");

        assertTrue(updated);
        assertEquals("1.0.5", settings.pluginVersion);
    }

    @Test
    void ignoresBlankOrUnchangedVersions() {
        AiSettingsState settings = new AiSettingsState();
        settings.pluginVersion = "1.0.5";

        assertFalse(PluginVersionSynchronizer.sync(settings, "1.0.5"));
        assertFalse(PluginVersionSynchronizer.sync(settings, ""));
        assertEquals("1.0.5", settings.pluginVersion);
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails before implementation**

Run:

```bash
./gradlew test --tests com.github.jdami.aicommit.startup.PluginVersionSynchronizerTest --stacktrace
```

Expected: FAIL with `cannot find symbol` errors for `PluginVersionSynchronizer`.

- [ ] **Step 3: Implement the Java-only synchronizer and simplify the startup activity**

Create `src/main/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizer.java`:

```java
package com.github.jdami.aicommit.startup;

import com.github.jdami.aicommit.settings.AiSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PluginVersionSynchronizer {

    private PluginVersionSynchronizer() {
    }

    public static boolean sync(@NotNull AiSettingsState settings, @Nullable String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank() || currentVersion.equals(settings.pluginVersion)) {
            return false;
        }

        settings.pluginVersion = currentVersion;
        return true;
    }
}
```

Replace `src/main/java/com/github/jdami/aicommit/startup/PluginUpdateActivity.java` with:

```java
package com.github.jdami.aicommit.startup;

import com.github.jdami.aicommit.settings.AiSettingsState;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public final class PluginUpdateActivity implements StartupActivity {

    private static final PluginId PLUGIN_ID = PluginId.getId("com.github.jdami.ai-generator-commit-message");

    @Override
    public void runActivity(@NotNull Project project) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PLUGIN_ID);
        if (plugin == null) {
            return;
        }

        AiSettingsState settings = AiSettingsState.getInstance();
        if (PluginVersionSynchronizer.sync(settings, plugin.getVersion())) {
            System.out.println("Plugin version synced to " + plugin.getVersion());
        }
    }
}
```

- [ ] **Step 4: Re-run tests and the plugin build**

Run:

```bash
./gradlew test --tests com.github.jdami.aicommit.startup.PluginVersionSynchronizerTest --stacktrace
./gradlew buildPlugin --stacktrace
```

Expected: PASS for the unit test and `BUILD SUCCESSFUL` for `buildPlugin`.

- [ ] **Step 5: Commit the Java-only startup compatibility fix**

Run:

```bash
git add src/main/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizer.java src/main/java/com/github/jdami/aicommit/startup/PluginUpdateActivity.java src/test/java/com/github/jdami/aicommit/startup/PluginVersionSynchronizerTest.java
git commit -m "refactor(startup): remove kotlin bridge from plugin update activity"
```

### Task 3: Refresh Metadata, CI, and Compatibility Verification

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `.github/workflows/release.yml`
- Modify: `README.md`
- Test: `./gradlew verifyPlugin --stacktrace`
- Test: `./gradlew runIde`

- [ ] **Step 1: Update plugin change notes for the compatibility release**

Replace the current `<change-notes>` block in `src/main/resources/META-INF/plugin.xml` with:

```xml
<change-notes><![CDATA[
<h2>1.0.5</h2>
<ul>
    <li>Migrate the build to IntelliJ Platform Gradle Plugin 2.x</li>
    <li>Raise the minimum supported IDE version to IntelliJ IDEA 2025.3</li>
    <li>Verify compatibility with IntelliJ IDEA 2026.1</li>
</ul>
]]></change-notes>
```

- [ ] **Step 2: Update the release workflow to build and verify on Java 21**

Replace `.github/workflows/release.yml` with:

```yaml
name: Release Plugin

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Release version'
        required: true
        type: string
  push:
    tags:
      - 'v*.*.*'

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build plugin
        run: ./gradlew buildPlugin --stacktrace

      - name: Verify plugin compatibility
        run: ./gradlew verifyPlugin --stacktrace

      - name: Verify plugin build
        run: |
          echo "Checking build output..."
          ls -lh build/distributions/
          if [ ! -f build/distributions/*.zip ]; then
            echo "Error: Plugin ZIP file not found!"
            exit 1
          fi

      - name: Get version
        id: version
        run: |
          if [ "${{ github.event_name }}" == "workflow_dispatch" ]; then
            echo "version=${{ github.event.inputs.version }}" >> $GITHUB_OUTPUT
          else
            echo "version=${GITHUB_REF#refs/tags/v}" >> $GITHUB_OUTPUT
          fi

      - name: Get plugin filename
        id: plugin
        run: |
          PLUGIN_FILE=$(ls build/distributions/*.zip | head -n 1)
          echo "file=$PLUGIN_FILE" >> $GITHUB_OUTPUT
          echo "name=$(basename $PLUGIN_FILE)" >> $GITHUB_OUTPUT

      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          tag_name: v${{ steps.version.outputs.version }}
          name: Release ${{ steps.version.outputs.version }}
          files: ${{ steps.plugin.outputs.file }}
          body: |
            ## AI Commit Message Generator v${{ steps.version.outputs.version }}

            ### Installation
            1. Download the plugin ZIP file below
            2. In IntelliJ IDEA, go to Settings → Plugins → ⚙️ → Install Plugin from Disk
            3. Select the downloaded ZIP file
            4. Restart IntelliJ IDEA

            ### Compatibility
            - Minimum IDE: IntelliJ IDEA 2025.3
            - Verified IDEs: IntelliJ IDEA 2025.3 and 2026.1

            ### Plugin File
            - `${{ steps.plugin.outputs.name }}`
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 3: Update the README for the new support window**

Make these targeted replacements in `README.md`:

```markdown
### 📋 Prerequisites

- IntelliJ IDEA 2025.3 or higher
- **For Ollama**: [Ollama](https://ollama.ai/) running locally with a downloaded model (e.g., qwen3:8b, llama2, codellama)
- **For OpenAI**: Valid OpenAI API key
```

```markdown
### 🛠️ Tech Stack

- **Language**: Java 21
- **Build Tool**: Gradle 8.14.4
- **Framework**: IntelliJ Platform SDK
- **Target IDE**: IntelliJ IDEA 2025.3+
```

````markdown
#### Build Plugin

```bash
./gradlew buildPlugin
./gradlew verifyPlugin
```
````

- [ ] **Step 4: Run compatibility verification**

Run:

```bash
./gradlew verifyPlugin --stacktrace
```

Expected: `BUILD SUCCESSFUL` and verifier reports generated under `build/reports/pluginVerifier/` with no missing classes, invalid extension declarations, or removed API errors for `2025.3` and `2026.1`.

- [ ] **Step 5: Run a manual smoke check in the sandbox IDE**

Run:

```bash
./gradlew runIde
```

Expected: the sandbox IDE starts, the commit dialog still shows the `Commit助手` action, and clicking it can write generated text into the commit message field.

- [ ] **Step 6: Commit metadata, CI, and documentation updates**

Run:

```bash
git add src/main/resources/META-INF/plugin.xml .github/workflows/release.yml README.md
git commit -m "docs: document 2025.3 and 2026.1 compatibility"
```
