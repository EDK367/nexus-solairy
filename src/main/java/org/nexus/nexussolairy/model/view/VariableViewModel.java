package org.nexus.nexussolairy.model.view;

import javafx.beans.property.SimpleStringProperty;

public class VariableViewModel {
    private final SimpleStringProperty name;
    private final SimpleStringProperty value;
    private final SimpleStringProperty type;
    private final SimpleStringProperty scope;

    public VariableViewModel(String name, String value, String type, String scope) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.type = new SimpleStringProperty(type);
        this.scope = new SimpleStringProperty(scope);
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public String getValue() {
        return value.get();
    }

    public SimpleStringProperty valueProperty() {
        return value;
    }

    public String getType() {
        return type.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public String getScope() {
        return scope.get();
    }

    public SimpleStringProperty scopeProperty() {
        return scope;
    }
}
