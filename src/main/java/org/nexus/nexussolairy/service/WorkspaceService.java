package org.nexus.nexussolairy.service;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.nexus.nexussolairy.model.view.EditorTabModel;
import org.nexus.nexussolairy.model.view.ExecutionSession;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.file.Project;

import java.util.function.Consumer;

public class WorkspaceService {
    private final ObjectProperty<Project> currentProject;
    private final ObjectProperty<EditorTabModel> activeTab;
    private final ObservableList<EditorTabModel> openTabs;
    private final ObservableList<ExecutionSession> executionSessions;
    private final ObjectProperty<ExecutionSession> activeSession;
    private final StringProperty statusMessage;
    private final StringProperty cursorPosition;
    private final ObjectProperty<LanguageType> activeLanguage;
    private Consumer<String> notificationHandler;

    private int sessionCounter = 1;

    // servicios como el lenguaje para su coloreado corresponiente
    public WorkspaceService() {
        this.currentProject = new SimpleObjectProperty<>(null);
        this.activeTab = new SimpleObjectProperty<>(null);
        this.openTabs = FXCollections.observableArrayList();
        this.executionSessions = FXCollections.observableArrayList();
        this.activeSession = new SimpleObjectProperty<>(null);
        this.statusMessage = new SimpleStringProperty("Ready");
        this.cursorPosition = new SimpleStringProperty("Ln 1, Col 1");
        this.activeLanguage = new SimpleObjectProperty<>(LanguageType.PIG_LATIN);
    }


    public Project getCurrentProject() {
        return currentProject.get();
    }

    public void setCurrentProject(Project project) {
        this.currentProject.set(project);
    }

    public EditorTabModel getActiveTab() {
        return activeTab.get();
    }

    public void setActiveTab(EditorTabModel tab) {
        this.activeTab.set(tab);
        if (tab != null) {
            this.activeLanguage.set(tab.getLanguageType());
        }
    }

    public ObservableList<EditorTabModel> getOpenTabs() {
        return openTabs;
    }

    public ObservableList<ExecutionSession> getExecutionSessions() {
        return executionSessions;
    }

    public ObjectProperty<ExecutionSession> activeSessionProperty() {
        return activeSession;
    }

    public ExecutionSession getActiveSession() {
        return activeSession.get();
    }

    public void setActiveSession(ExecutionSession session) {
        this.activeSession.set(session);
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public void setStatusMessage(String message) {
        this.statusMessage.set(message);
    }

    public StringProperty cursorPositionProperty() {
        return cursorPosition;
    }

    public void setCursorPosition(int line, int col) {
        this.cursorPosition.set(String.format("Ln %d, Col %d", line, col));
    }

    public ObjectProperty<LanguageType> activeLanguageProperty() {
        return activeLanguage;
    }

    public void setNotificationHandler(Consumer<String> notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    public void notifyUser(String message) {
        if (notificationHandler != null) {
            notificationHandler.accept(message);
        }
        setStatusMessage(message);
    }

    public ExecutionSession createNewExecutionSession(String fileName, LanguageType language) {
        ExecutionSession session = new ExecutionSession(sessionCounter++, fileName, language);
        executionSessions.add(session);
        setActiveSession(session);
        return session;
    }
}
