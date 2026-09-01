package org.nexus.nexussolairy.model.view;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class CallStackViewModel {
    private final SimpleIntegerProperty frame;
    private final SimpleStringProperty functionName;
    private final SimpleStringProperty location;

    public CallStackViewModel(int frame, String functionName, String location) {
        this.frame = new SimpleIntegerProperty(frame);
        this.functionName = new SimpleStringProperty(functionName);
        this.location = new SimpleStringProperty(location);
    }

    public int getFrame() {
        return frame.get();
    }

    public SimpleIntegerProperty frameProperty() {
        return frame;
    }

    public String getFunctionName() {
        return functionName.get();
    }

    public SimpleStringProperty functionNameProperty() {
        return functionName;
    }

    public String getLocation() {
        return location.get();
    }

    public SimpleStringProperty locationProperty() {
        return location;
    }
}
