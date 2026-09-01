package org.nexus.nexussolairy.model.semantic;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SemanticError {
    private final SimpleIntegerProperty line;
    private final SimpleIntegerProperty column;
    private final SimpleStringProperty type;
    private final SimpleStringProperty message;

    public SemanticError(int line, int column, String type, String message) {
        this.line = new SimpleIntegerProperty(line);
        this.column = new SimpleIntegerProperty(column);
        this.type = new SimpleStringProperty(type);
        this.message = new SimpleStringProperty(message);
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

    public String getType() {
        return type.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public String getMessage() {
        return message.get();
    }

    public SimpleStringProperty messageProperty() {
        return message;
    }
}
