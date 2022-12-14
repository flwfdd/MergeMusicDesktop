package xyz.flwfdd.mergemusicdesktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/5 14:52
 * @implNote 主程序入口
 */

public class MainApplication extends Application {

    public Stage mainStage;
    public static Application instance;

    @Override
    public void start(Stage stage) throws IOException {
        instance=this;
        mainStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 888, 666);
        ((MainController) fxmlLoader.getController()).setApp(stage,scene);
        stage.setTitle("MergeMusic");
        // 只加载一个大图片的话最终图标会不清晰
        stage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("16.png"))));
        stage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("32.png"))));
        stage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("64.png"))));
        stage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("128.png"))));
        stage.getIcons().add(new Image(Objects.requireNonNull(MainApplication.class.getResourceAsStream("256.png"))));
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.show();

        stage.setOnCloseRequest(windowEvent -> System.exit(0));
    }

    public static void main(String[] args) {
        launch();
    }
}