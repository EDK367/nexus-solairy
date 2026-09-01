package org.nexus.nexussolairy.model.lexical;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class LexerError {
    private final SimpleIntegerProperty line;
    private final SimpleIntegerProperty column;
    private final SimpleStringProperty message;

    public LexerError(int line, int column, String message) {
        this.line = new SimpleIntegerProperty(line);
        this.column = new SimpleIntegerProperty(column);
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

    public String getMessage() {
        return message.get();
    }

    public SimpleStringProperty messageProperty() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("Error lexico en: (%d - %d). Descripcion: %s", this.line, this.column, this.message);
    }
}
