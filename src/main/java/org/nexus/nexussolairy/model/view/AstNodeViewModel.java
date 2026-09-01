package org.nexus.nexussolairy.model.view;

import java.util.ArrayList;
import java.util.List;

public class AstNodeViewModel {
    private final String id;
    private final String label;
    private final String type;
    private final String value;
    private final List<AstNodeViewModel> children;

    public AstNodeViewModel(String id, String label, String type, String value) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<AstNodeViewModel> getChildren() {
        return children;
    }

    public void addChild(AstNodeViewModel child) {
        this.children.add(child);
    }
}
