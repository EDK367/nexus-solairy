package org.nexus.nexussolairy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.nexus.nexussolairy.controller.MainController;

public class NexuSolairyApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/nexus/nexussolairy/view/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 1440, 900);
        String css = getClass().getResource("/org/nexus/nexussolairy/css/nexu-solairy.css").toExternalForm();
        scene.getStylesheets().add(css);

        MainController controller = loader.getController();
        controller.setStage(stage, scene);

        stage.setTitle("Nexu-Solairy — Compiler Development Environment");
        stage.setMinWidth(1100);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();

        controller.handleShowWelcome();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
