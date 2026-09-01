package org.nexus.nexussolairy.model.lexical;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TokenInfo {
    private final SimpleStringProperty token;
    private final SimpleStringProperty lexeme;
    private final SimpleIntegerProperty line;
    private final SimpleIntegerProperty column;

    public TokenInfo(String token, String lexeme, int line, int column) {
        this.token = new SimpleStringProperty(token);
        this.lexeme = new SimpleStringProperty(lexeme);
        this.line = new SimpleIntegerProperty(line);
        this.column = new SimpleIntegerProperty(column);
    }

    public String getToken() {
        return token.get();
    }

    public SimpleStringProperty tokenProperty() {
        return token;
    }

    public String getLexeme() {
        return lexeme.get();
    }

    public SimpleStringProperty lexemeProperty() {
        return lexeme;
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
}
