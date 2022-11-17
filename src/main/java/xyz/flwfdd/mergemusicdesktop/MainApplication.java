package xyz.flwfdd.mergemusicdesktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    public Stage mainStage;

    @Override
    public void start(Stage stage) throws IOException {
        mainStage=stage;
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 666, 420);
        stage.setTitle("MergeMusic");
        stage.setScene(scene);
        stage.show();

        MainController controller=fxmlLoader.getController();
        controller.setMain(this,stage);
    }

    public static void main(String[] args) {
        launch();
    }
}