package xyz.flwfdd.mergemusicdesktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/5 14:52
 * @implNote 主程序入口
 */

public class MainApplication extends Application {

    public Stage mainStage;

    @Override
    public void start(Stage stage) throws IOException {
        mainStage=stage;
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 888, 666);
        stage.setTitle("MergeMusic");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}