package org.nexus.nexussolairy.model.syntactic;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SyntaxError {
    private final SimpleStringProperty severity;
    private final SimpleIntegerProperty line;
    private final SimpleIntegerProperty column;
    private final SimpleStringProperty message;

    public SyntaxError(String severity, int line, int column, String message) {
        this.severity = new SimpleStringProperty(severity);
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
