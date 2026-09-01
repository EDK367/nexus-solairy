package org.nexus.nexussolairy.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class WelcomeDialog {
    private final Stage stage;

    public WelcomeDialog(Stage owner, Runnable onNewProject, Runnable onOpenProject, Runnable onOpenFile, List<String> recentProjects, Consumer<File> onOpenRecent) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("Welcome to Nexu-Solairy");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("welcome-root");
        root.setPadding(new Insets(24));
        root.setPrefSize(720, 480);

        VBox headerBox = new VBox(12);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 18, 0));

        StackPane logoPane = new StackPane();
        Circle outerCircle = new Circle(28);
        outerCircle.setFill(Color.TRANSPARENT);
        outerCircle.setStroke(Color.web("#00D9FF"));
        outerCircle.setStrokeWidth(2.5);

        Circle innerCircle = new Circle(14);
        innerCircle.setFill(Color.web("#8B5CF6"));
        innerCircle.setOpacity(0.85);

        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(
                -6.0, -8.0,
                8.0, 0.0,
                -6.0, 8.0
        );
        triangle.setFill(Color.web("#00D9FF"));

        logoPane.getChildren().addAll(outerCircle, innerCircle, triangle);

        Label title = new Label("NEXU-SOLAIRY");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("Compiler Development Environment");
        subtitle.getStyleClass().add("welcome-subtitle");

        headerBox.getChildren().addAll(logoPane, title, subtitle);
        root.setTop(headerBox);

        HBox centerBox = new HBox(20);
        centerBox.setAlignment(Pos.CENTER);

        VBox actionsBox = new VBox(12);
        actionsBox.setPrefWidth(260);
        actionsBox.setAlignment(Pos.TOP_LEFT);

        Label actionsHeader = new Label("Start");
        actionsHeader.getStyleClass().add("welcome-section-title");

        Button newProjBtn = new Button("✦  New Project...");
        newProjBtn.getStyleClass().addAll("btn", "btn-accent-cyan", "welcome-action-btn");
        newProjBtn.setMaxWidth(Double.MAX_VALUE);
        newProjBtn.setOnAction(e -> {
            stage.close();
            if (onNewProject != null) onNewProject.run();
        });

        Button openProjBtn = new Button("Open Project...");
        openProjBtn.getStyleClass().addAll("btn", "btn-default", "welcome-action-btn");
        openProjBtn.setMaxWidth(Double.MAX_VALUE);
        openProjBtn.setOnAction(e -> {
            stage.close();
            if (onOpenProject != null) onOpenProject.run();
        });

        Button openFileBtn = new Button("Open File...");
        openFileBtn.getStyleClass().addAll("btn", "btn-default", "welcome-action-btn");
        openFileBtn.setMaxWidth(Double.MAX_VALUE);
        openFileBtn.setOnAction(e -> {
            stage.close();
            if (onOpenFile != null) onOpenFile.run();
        });

        actionsBox.getChildren().addAll(actionsHeader, newProjBtn, openProjBtn, openFileBtn);

        VBox recentBox = new VBox(10);
        HBox.setHgrow(recentBox, Priority.ALWAYS);
        recentBox.setAlignment(Pos.TOP_LEFT);

        Label recentHeader = new Label("Recent Projects");
        recentHeader.getStyleClass().add("welcome-section-title");

        ListView<String> recentList = new ListView<>();
        recentList.getStyleClass().add("welcome-recent-list");
        VBox.setVgrow(recentList, Priority.ALWAYS);

        if (recentProjects != null && !recentProjects.isEmpty()) {
            recentList.getItems().addAll(recentProjects);
        } else {
            recentList.setPlaceholder(new Label("No recent projects"));
        }

        recentList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = recentList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    stage.close();
                    if (onOpenRecent != null) onOpenRecent.accept(new File(selected));
                }
            }
        });

        recentBox.getChildren().addAll(recentHeader, recentList);

        centerBox.getChildren().addAll(actionsBox, recentBox);
        root.setCenter(centerBox);

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
