package org.nexus.nexussolairy.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.nexus.nexussolairy.model.enums.LanguageType;

import java.io.File;
import java.util.function.BiConsumer;

public class NewProjectDialog {
    private final Stage stage;
    private final TextField nameField;
    private final TextField locationField;

    // dialogo para crear un nuevo proyecto
    public NewProjectDialog(Stage owner, BiConsumer<String, LanguageType> onCreate) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Nexu-Solairy Project");

        VBox root = new VBox(16);
        root.getStyleClass().add("new-project-root");
        root.setPadding(new Insets(20));
        root.setPrefWidth(480);

        Label title = new Label("Create Compiler Project");
        title.getStyleClass().add("new-project-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(14);

        Label nameLabel = new Label("Project Name:");
        nameLabel.getStyleClass().add("form-label");
        nameField = new TextField("NexuProject");
        nameField.getStyleClass().add("form-field");

        Label locationLabel = new Label("Location:");
        locationLabel.getStyleClass().add("form-label");
        // el usuario y su ruta por default
        locationField = new TextField(System.getProperty("user.home"));
        locationField.getStyleClass().add("form-field");

        Button browseBtn = new Button("Browse...");
        browseBtn.getStyleClass().addAll("btn", "btn-default");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Project Location");
            dc.setInitialDirectory(new File(locationField.getText()));
            File selected = dc.showDialog(stage);
            if (selected != null) {
                locationField.setText(selected.getAbsolutePath());
            }
        });

        HBox locBox = new HBox(6, locationField, browseBtn);
        HBox.setHgrow(locationField, Priority.ALWAYS);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(locationLabel, 0, 1);
        grid.add(locBox, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("btn", "btn-default");
        cancelBtn.setOnAction(e -> stage.close());

        Button createBtn = new Button("Create Project");
        createBtn.getStyleClass().addAll("btn", "btn-accent-cyan");
        createBtn.setDefaultButton(true);
        createBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                stage.close();
                if (onCreate != null) {
                    onCreate.accept(locationField.getText().trim() + File.separator + name, LanguageType.UNKNOWN);
                }
            }
        });

        buttonsBox.getChildren().addAll(cancelBtn, createBtn);

        root.getChildren().addAll(title, grid, buttonsBox);

        Scene scene = new Scene(root);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
    }

    public void show() {
        stage.centerOnScreen();
        stage.show();
    }
}
