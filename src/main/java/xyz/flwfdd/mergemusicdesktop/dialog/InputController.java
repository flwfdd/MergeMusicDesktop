package xyz.flwfdd.mergemusicdesktop.dialog;

import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/15 16:24
 * @implNote 输入弹出框
 */

public class InputController {
    @FXML
    HBox headBar;
    @FXML
    Text msgText;
    @FXML
    MFXTextField inputField;

    @FXML
    void onClose() {
        input="";
        stage.close();
    }

    @FXML
    void onOK() {
        input=inputField.getText();
        stage.close();
    }

    public void init(String msg){
        inputField.setText("");
        msgText.setText(msg);
    }

    static String input;
    static Stage stage;
    static InputController controller;
    double xOffset = 0;
    double yOffset = 0;

    public void initialize() {
        //窗口拖动
        headBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        headBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    public static String input(String msg) {
        // 返回选择的ID 没有选择就返回0
        if (controller == null) try {
            FXMLLoader loader = new FXMLLoader(InputController.class.getResource("input-view.fxml"));
            stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.getScene().setFill(Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            controller = loader.getController();
        } catch (Exception e) {
            System.out.println("open confirm dialog error:" + e);
        }

        controller.init(msg);
        stage.showAndWait();
        return input;
    }
}
