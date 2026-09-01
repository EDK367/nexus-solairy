package org.nexus.nexussolairy.view.utils;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class NotificationToast {

    public static void show(StackPane container, String message) {
        if (container == null || message == null || message.isEmpty()) return;

        HBox toast = new HBox(8);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().add("notification-toast");
        toast.setPadding(new Insets(8, 16, 8, 16));
        toast.setMaxWidth(380);
        toast.setMaxHeight(36);

        Label icon = new Label("ⓘ");
        icon.getStyleClass().add("toast-icon");

        Label label = new Label(message);
        label.getStyleClass().add("toast-text");

        toast.getChildren().addAll(icon, label);
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(0, 24, 24, 0));

        toast.setOpacity(0);
        container.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.millis(2500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> container.getChildren().remove(toast));

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.play();
    }
}
