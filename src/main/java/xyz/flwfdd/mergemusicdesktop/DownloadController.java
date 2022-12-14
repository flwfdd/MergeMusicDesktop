package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTooltip;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.kordamp.ikonli.javafx.FontIcon;
import xyz.flwfdd.mergemusicdesktop.model.Config;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/13 22:08
 * @implNote
 */

public class DownloadController {

    @FXML
    VBox downloadBox;

    static class DownloadItem {
        static VBox downloadBox;
        SimpleDoubleProperty progress;
        int lastProgress = 0;

        DownloadItem(String name, File path, String tooltipText) {
            progress = new SimpleDoubleProperty(0);
            var box = new HBox();
            box.setAlignment(Pos.CENTER);
            box.setPrefWidth(580);
            box.setSpacing(11);
            box.setPadding(new Insets(6));
            box.setStyle("-fx-background-radius: 11px;-fx-background-color: #29D4FF22;");

            var nameLabel = new Label(name);
            nameLabel.setFont(new Font("Source Han Serif CN Medium", 16));
            nameLabel.setAlignment(Pos.CENTER_LEFT);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            box.getChildren().add(nameLabel);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            var tooltip = new MFXTooltip(nameLabel);
            tooltip.setText(tooltipText);
            tooltip.install();

            var progressLabel = new Label("0%");
            progressLabel.setFont(new Font("Source Han Serif CN Medium", 16));
            progressLabel.setAlignment(Pos.CENTER_LEFT);
            progressLabel.setMinWidth(Region.USE_PREF_SIZE);
            box.getChildren().add(progressLabel);

            var button = new MFXButton("查看", new FontIcon("mdral-folder_open:24"));
            button.getStyleClass().add("custom-button");
            button.setMinWidth(Region.USE_PREF_SIZE);
            button.setOnAction(event -> {
                try {
                    Desktop.getDesktop().open(path);
                } catch (IOException e) {
                    Config.getInstance().setMsg("打开失败Orz");
                    System.out.println("open download error" + e);
                }
            });
            box.getChildren().add(button);

            downloadBox.getChildren().add(0, box);

            progress.addListener(observable -> {
                if (lastProgress == (int) Math.floor(progress.get() * 100)) return;
                lastProgress = (int) Math.floor(progress.get() * 100);
                Platform.runLater(() -> {
                    progressLabel.setText(lastProgress + "%");
                    String style = "-fx-background-radius: 11px;-fx-background-color: linear-gradient(to right,#29D4FF66 1.24%,#29D4FF22 2.24%);";
                    style = style.replace("1.24%", lastProgress - 1 + "%");
                    style = style.replace("2.24%", lastProgress + 1 + "%");
                    box.setStyle(style);
                });
            });
        }

        SimpleDoubleProperty getProgress() {
            return progress;
        }

        static void setDownloadBox(VBox box) {
            downloadBox = box;
        }
    }

    static public SimpleDoubleProperty createDownloadItem(String name, File path, String tooltip) {
        return new DownloadItem(name, path, tooltip).getProgress();
    }

    public void initialize() {
        DownloadItem.setDownloadBox(downloadBox);
    }
}
