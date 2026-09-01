package org.nexus.nexussolairy.model.view;

import org.nexus.nexussolairy.model.enums.LanguageType;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExecutionSession {
    public enum SessionStatus {
        RUNNING, FINISHED, STOPPED, ERROR
    }

    public static class LogEntry {
        private final String type;
        private final String message;

        public LogEntry(String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    private final int id;
    private final String title;
    private final String targetFile;
    private final LanguageType language;
    private final String startTime;
    private SessionStatus status;
    private final List<LogEntry> logEntries;

    public ExecutionSession(int id, String targetFile, LanguageType language) {
        this.id = id;
        this.title = "Run #" + id;
        this.targetFile = targetFile;
        this.language = language;
        this.startTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.status = SessionStatus.RUNNING;
        this.logEntries = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public LanguageType getLanguage() {
        return language;
    }

    public String getStartTime() {
        return startTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public List<LogEntry> getLogEntries() {
        return logEntries;
    }

    public void addLog(String type, String message) {
        logEntries.add(new LogEntry(type, message));
    }
}
