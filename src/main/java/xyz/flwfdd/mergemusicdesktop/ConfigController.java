package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import xyz.flwfdd.mergemusicdesktop.model.Config;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/23 19:35
 * @implNote 配置面板
 */
public class ConfigController {
    @FXML
    MFXScrollPane scrollPane;

    @FXML
    VBox configBox;

    Config configModel;

    void initConfigBox() {
        configModel.configItems.forEach(item -> {
            if (item.type == Config.Type.TEXT) {
                var textFiled = new MFXTextField();
                textFiled.setFloatingText(item.description);
                textFiled.setFloatingTextGap(0);
                textFiled.getStyleClass().add("custom-text-field");
                textFiled.textProperty().bindBidirectional(item.valueProperty);
                textFiled.setEditable(item.editable);
                textFiled.setPrefColumnCount(24);
                configBox.getChildren().add(textFiled);
            }
        });
        var buttonBox = new HBox();
        buttonBox.setSpacing(11);

        var resetButton = new MFXButton("重置", new FontIcon("mdrmz-settings_backup_restore:24"));
        resetButton.getStyleClass().add("custom-button");
        resetButton.setOnAction(e -> configModel.resetConfigItems());
        buttonBox.getChildren().add(resetButton);

        var saveButton = new MFXButton("保存", new FontIcon("mdrmz-save:24"));
        saveButton.getStyleClass().add("custom-button");
        saveButton.setOnAction(e -> configModel.saveConfigItems());
        buttonBox.getChildren().add(saveButton);

        configBox.getChildren().add(buttonBox);
    }

    public void initialize() {
        configModel = Config.getInstance();
        ScrollUtils.addSmoothScrolling(scrollPane);
        initConfigBox();
    }
}
