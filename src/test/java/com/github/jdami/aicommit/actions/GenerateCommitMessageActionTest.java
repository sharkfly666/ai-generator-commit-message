package com.github.jdami.aicommit.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.vcs.commit.CommitMessageUi;
import com.intellij.vcs.commit.CommitWorkflowUi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCommitMessageActionTest {
    @Test
    void messageOnlyEditorDoesNotOfferGeneration() {
        CommitMessageI message = messageControl(new AtomicReference<>());
        DataContext context = context(Map.of(VcsDataKeys.COMMIT_MESSAGE_CONTROL, message));
        assertFalse(GenerateCommitMessageAction.hasCommitContext(context));
        assertNull(GenerateCommitMessageAction.resolveCommitInput(context, null));
    }

    @Test
    void missingOrUnrelatedPanelDoesNotThrow() {
        DataContext context = context(Map.of(Refreshable.PANEL_KEY,
                proxy(Refreshable.class, (name, args) -> null)));
        assertFalse(GenerateCommitMessageAction.hasCommitContext(context));
        assertNull(GenerateCommitMessageAction.resolveCommitInput(context, null));
    }

    @Test
    void standaloneEditorUsesSelectedChangesAndItsOwnMessageControl() {
        AtomicReference<String> text = new AtomicReference<>();
        ContentRevision revision = proxy(ContentRevision.class, (name, args) -> null);
        Change change = new Change(revision, null);
        DataContext context = context(Map.of(VcsDataKeys.COMMIT_MESSAGE_CONTROL, messageControl(text),
                VcsDataKeys.SELECTED_CHANGES, new Change[]{change}));
        assertTrue(GenerateCommitMessageAction.hasCommitContext(context));
        var input = GenerateCommitMessageAction.resolveCommitInput(context, null);
        assertNotNull(input);
        assertEquals(List.of(change), input.changes());
        input.setMessage().accept("fix: selected commit");
        assertEquals("fix: selected commit", text.get());
    }

    @Test
    void modernWorkflowWorksWithoutLegacyPanelOrMessageControl() {
        AtomicReference<String> text = new AtomicReference<>();
        FilePath file = proxy(FilePath.class, (name, args) -> null);
        CommitMessageUi message = proxy(CommitMessageUi.class, (name, args) -> {
            if (name.equals("setText")) text.set((String) args[0]);
            return null;
        });
        CommitWorkflowUi workflow = proxy(CommitWorkflowUi.class, (name, args) -> switch (name) {
            case "getCommitMessageUi" -> message;
            case "getIncludedChanges" -> List.of();
            case "getIncludedUnversionedFiles" -> List.of(file);
            default -> throw new AssertionError("Unexpected workflow call: " + name);
        });
        DataContext context = context(Map.of(VcsDataKeys.COMMIT_WORKFLOW_UI, workflow));
        assertTrue(GenerateCommitMessageAction.hasCommitContext(context));
        var input = GenerateCommitMessageAction.resolveCommitInput(context, null);
        assertNotNull(input);
        assertEquals(List.of(file), input.unversionedFiles());
        input.setMessage().accept("feat: new file");
        assertEquals("feat: new file", text.get());
    }

    @Test
    void legacyPanelStillReceivesGeneratedMessage() {
        AtomicReference<String> text = new AtomicReference<>();
        CheckinProjectPanel panel = proxy(CheckinProjectPanel.class, (name, args) -> switch (name) {
            case "getSelectedChanges", "getVirtualFiles" -> List.of();
            case "setCommitMessage" -> { text.set((String) args[0]); yield null; }
            default -> throw new AssertionError("Unexpected panel call: " + name);
        });
        DataContext context = context(Map.of(Refreshable.PANEL_KEY, panel));
        assertTrue(GenerateCommitMessageAction.hasCommitContext(context));
        var input = GenerateCommitMessageAction.resolveCommitInput(context, null);
        assertNotNull(input);
        input.setMessage().accept("fix: legacy panel");
        assertEquals("fix: legacy panel", text.get());
    }

    private static CommitMessageI messageControl(AtomicReference<String> text) {
        return proxy(CommitMessageI.class, (name, args) -> {
            if (name.equals("setCommitMessage")) text.set((String) args[0]);
            return null;
        });
    }

    @SuppressWarnings("removal")
    private static DataContext context(Map<DataKey<?>, Object> values) {
        return name -> values.entrySet().stream()
                .filter(entry -> entry.getKey().is(name))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, args) -> handler.apply(method.getName(), args));
    }
}
