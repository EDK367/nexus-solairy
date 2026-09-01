package org.nexus.nexussolairy.model.view;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class QuadrupleViewModel {
    private final SimpleIntegerProperty index;
    private final SimpleStringProperty operator;
    private final SimpleStringProperty arg1;
    private final SimpleStringProperty arg2;
    private final SimpleStringProperty result;

    public QuadrupleViewModel(int index, String operator, String arg1, String arg2, String result) {
        this.index = new SimpleIntegerProperty(index);
        this.operator = new SimpleStringProperty(operator);
        this.arg1 = new SimpleStringProperty(arg1);
        this.arg2 = new SimpleStringProperty(arg2);
        this.result = new SimpleStringProperty(result);
    }

    public int getIndex() {
        return index.get();
    }

    public SimpleIntegerProperty indexProperty() {
        return index;
    }

    public String getOperator() {
        return operator.get();
    }

    public SimpleStringProperty operatorProperty() {
        return operator;
    }

    public String getArg1() {
        return arg1.get();
    }

    public SimpleStringProperty arg1Property() {
        return arg1;
    }

    public String getArg2() {
        return arg2.get();
    }

    public SimpleStringProperty arg2Property() {
        return arg2;
    }

    public String getResult() {
        return result.get();
    }

    public SimpleStringProperty resultProperty() {
        return result;
    }
}
