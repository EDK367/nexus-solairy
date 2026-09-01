package org.nexus.nexussolairy.view.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

public class CommandPaletteDialog {

    private final Stage stage;
    private final TextField searchField;
    private final ListView<CommandItem> commandListView;
    private final FilteredList<CommandItem> filteredCommands;

    public CommandPaletteDialog(Stage owner, List<CommandItem> commands) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        ObservableList<CommandItem> masterList = FXCollections.observableArrayList(commands);
        filteredCommands = new FilteredList<>(masterList, p -> true);

        VBox root = new VBox(10);
        root.getStyleClass().add("command-palette-root");
        root.setPadding(new Insets(12));
        root.setPrefWidth(550);
        root.setMaxHeight(400);

        HBox searchContainer = new HBox(8);
        searchContainer.setAlignment(Pos.CENTER_LEFT);
        searchContainer.getStyleClass().add("command-search-box");

        Label searchIcon = new Label("⌕");
        searchIcon.getStyleClass().add("command-search-icon");

        searchField = new TextField();
        searchField.setPromptText("Type a command or search action (e.g. Run, Open, AST)...");
        searchField.getStyleClass().add("command-search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchContainer.getChildren().addAll(searchIcon, searchField);

        commandListView = new ListView<>(filteredCommands);
        commandListView.getStyleClass().add("command-list-view");
        VBox.setVgrow(commandListView, Priority.ALWAYS);

        commandListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CommandItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(10);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Label catLabel = new Label(item.getCategory());
                    catLabel.getStyleClass().add("command-category-badge");

                    Label titleLabel = new Label(item.getTitle());
                    titleLabel.getStyleClass().add("command-title-label");
                    HBox.setHgrow(titleLabel, Priority.ALWAYS);

                    Label shortcutLabel = new Label(item.getShortcut() != null ? item.getShortcut() : "");
                    shortcutLabel.getStyleClass().add("command-shortcut-badge");

                    box.getChildren().addAll(catLabel, titleLabel, shortcutLabel);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredCommands.setPredicate(item -> {
                if (newVal == null || newVal.trim().isEmpty()) return true;
                String lower = newVal.toLowerCase().trim();
                return item.getTitle().toLowerCase().contains(lower) ||
                        item.getCategory().toLowerCase().contains(lower);
            });
            if (!filteredCommands.isEmpty()) {
                commandListView.getSelectionModel().select(0);
            }
        });

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN) {
                commandListView.requestFocus();
                commandListView.getSelectionModel().selectNext();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                executeSelected();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });

        commandListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                executeSelected();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });

        commandListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                executeSelected();
            }
        });

        root.getChildren().addAll(searchContainer, commandListView);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);

        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                stage.close();
            }
        });
    }

    private void executeSelected() {
        CommandItem selected = commandListView.getSelectionModel().getSelectedItem();
        stage.close();
        if (selected != null && selected.getAction() != null) {
            selected.getAction().run();
        }
    }

    public void show() {
        searchField.clear();
        filteredCommands.setPredicate(p -> true);
        if (!filteredCommands.isEmpty()) {
            commandListView.getSelectionModel().select(0);
        }
        stage.centerOnScreen();
        stage.show();
        searchField.requestFocus();
    }
}
