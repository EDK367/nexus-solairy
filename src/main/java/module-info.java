module org.nexus.nexussolairy {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.feather;
    requires org.fxmisc.richtext;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires org.antlr.antlr4.runtime;

    opens org.nexus.nexussolairy to javafx.fxml;
    opens org.nexus.nexussolairy.controller to javafx.fxml;
    opens org.nexus.nexussolairy.view to javafx.fxml;
    exports org.nexus.nexussolairy;
    exports org.nexus.nexussolairy.controller;
    exports org.nexus.nexussolairy.view;
    exports org.nexus.nexussolairy.model.enums;
    opens org.nexus.nexussolairy.model.enums to javafx.base;
    exports org.nexus.nexussolairy.view.utils;
    opens org.nexus.nexussolairy.view.utils to javafx.fxml;
    exports org.nexus.nexussolairy.model.lexical;
    opens org.nexus.nexussolairy.model.lexical to javafx.base;
    exports org.nexus.nexussolairy.model.semantic;
    opens org.nexus.nexussolairy.model.semantic to javafx.base;
    exports org.nexus.nexussolairy.model.semantic.view;
    opens org.nexus.nexussolairy.model.semantic.view to javafx.base;
    exports org.nexus.nexussolairy.model.file;
    opens org.nexus.nexussolairy.model.file to javafx.base;
    exports org.nexus.nexussolairy.model.view;
    opens org.nexus.nexussolairy.model.view to javafx.base;
    exports org.nexus.nexussolairy.model.syntactic;
    opens org.nexus.nexussolairy.model.syntactic to javafx.base;
    exports org.nexus.nexussolairy.service.ui;
    exports org.nexus.nexussolairy.ui;
    opens org.nexus.nexussolairy.ui to javafx.fxml;
}