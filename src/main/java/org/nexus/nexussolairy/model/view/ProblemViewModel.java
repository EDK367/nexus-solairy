package org.nexus.nexussolairy.model.view;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ProblemViewModel {
    private final SimpleStringProperty severity;
    private final SimpleStringProperty file;
    private final SimpleIntegerProperty line;
    private final SimpleIntegerProperty column;
    private final SimpleStringProperty message;

    public ProblemViewModel(String severity, String file, int line, int column, String message) {
        this.severity = new SimpleStringProperty(severity);
        this.file = new SimpleStringProperty(file);
        this.line = new SimpleIntegerProperty(line);
        this.column = new SimpleIntegerProperty(column);
        this.message = new SimpleStringProperty(message);
    }

    public String getSeverity() {
        return severity.get();
    }

    public SimpleStringProperty severityProperty() {
        return severity;
    }

    public String getFile() {
        return file.get();
    }

    public SimpleStringProperty fileProperty() {
        return file;
    }

    public int getLine() {
        return line.get();
    }

    public SimpleIntegerProperty lineProperty() {
        return line;
    }

    public int getColumn() {
        return column.get();
    }

    public SimpleIntegerProperty columnProperty() {
        return column;
    }

    public String getMessage() {
        return message.get();
    }

    public SimpleStringProperty messageProperty() {
        return message;
    }
}
