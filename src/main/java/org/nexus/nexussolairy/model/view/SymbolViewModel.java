package org.nexus.nexussolairy.model.view;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SymbolViewModel {
    private final SimpleStringProperty name;
    private final SimpleStringProperty dataType;
    private final SimpleStringProperty symbolKind;
    private final SimpleStringProperty scopeKind;
    private final SimpleStringProperty languageType;
    private final SimpleStringProperty value;
    private final SimpleIntegerProperty line;
    private final SimpleIntegerProperty column;

    public SymbolViewModel(String name, String dataType, String symbolKind, String scopeKind, String languageType, String value, int line, int column) {
        this.name = new SimpleStringProperty(name);
        this.dataType = new SimpleStringProperty(dataType);
        this.symbolKind = new SimpleStringProperty(symbolKind);
        this.scopeKind = new SimpleStringProperty(scopeKind);
        this.languageType = new SimpleStringProperty(languageType);
        this.value = new SimpleStringProperty(value);
        this.line = new SimpleIntegerProperty(line);
        this.column = new SimpleIntegerProperty(column);
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getDataType() {
        return dataType.get();
    }

    public SimpleStringProperty dataTypeProperty() {
        return dataType;
    }

    public String getSymbolKind() {
        return symbolKind.get();
    }

    public SimpleStringProperty symbolKindProperty() {
        return symbolKind;
    }

    public String getScopeKind() {
        return scopeKind.get();
    }

    public SimpleStringProperty scopeKindProperty() {
        return scopeKind;
    }

    public String getLanguageType() {
        return languageType.get();
    }

    public SimpleStringProperty languageTypeProperty() {
        return languageType;
    }

    public String getValue() {
        return value.get();
    }

    public SimpleStringProperty valueProperty() {
        return value;
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
