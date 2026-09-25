package org.nexus.nexussolairy.model.view;

import javafx.beans.property.SimpleStringProperty;

public class StackViewModel {
    private final SimpleStringProperty address;
    private final SimpleStringProperty type;
    private final SimpleStringProperty value;
    private final SimpleStringProperty details;

    public StackViewModel(String address, String type, String value, String details) {
        this.address = new SimpleStringProperty(address);
        this.type = new SimpleStringProperty(type);
        this.value = new SimpleStringProperty(value);
        this.details = new SimpleStringProperty(details);
    }

    public String getAddress() {
        return address.get();
    }

    public SimpleStringProperty addressProperty() {
        return address;
    }

    public String getType() {
        return type.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public String getValue() {
        return value.get();
    }

    public SimpleStringProperty valueProperty() {
        return value;
    }

    public String getDetails() {
        return details.get();
    }

    public SimpleStringProperty detailsProperty() {
        return details;
    }
}
