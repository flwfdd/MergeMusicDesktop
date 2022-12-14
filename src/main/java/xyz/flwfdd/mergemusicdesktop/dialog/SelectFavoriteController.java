package xyz.flwfdd.mergemusicdesktop.dialog;

import io.github.palexdev.materialfx.controls.MFXListView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import xyz.flwfdd.mergemusicdesktop.model.table.FavoriteTable;
import xyz.flwfdd.mergemusicdesktop.music.DB;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/14 23:50
 * @implNote 选择收藏夹弹出框
 */

public class SelectFavoriteController {

    @FXML
    HBox headBar;
    @FXML
    MFXListView<String> favList;
    @FXML
    MFXTextField newNameField;
    @FXML
    Label msgLabel;

    @FXML
    void onClose() {
        selected = 0;
        stage.close();
    }

    @FXML
    void onNew() {
        if (newNameField.getText().isBlank()) {
            msgLabel.setText("名字不能为空！");
            return;
        }
        int id = FavoriteTable.getInstance().newList(newNameField.getText());
        lists.put(id, newNameField.getText());
        items.add((items.size() + 1) + "." + newNameField.getText());
    }

    @FXML
    void onOK() {
        favList.getSelectionModel().getSelectedValues().forEach(v -> selected = getId(v));
        stage.close();
    }

    @FXML
    void onDelete() {
        AtomicInteger id = new AtomicInteger(0);
        AtomicReference<String> name = new AtomicReference<>();
        favList.getSelectionModel().getSelectedValues().forEach(x -> {
            id.set(getId(x));
            name.set(x.substring(x.indexOf('.') + 1));
        });
        if (id.get() != 0) {
            int size = DB.getInstance().getList(id.get()).size();
            if (ConfirmController.confirm("当真要删除收藏夹 " + name.get() + "（" + size + "首）" + " ？")){
                FavoriteTable.getInstance().deleteList(id.get());
                init();
            }
        }
    }


    static int selected;

    static Stage stage;
    static SelectFavoriteController controller;
    double xOffset = 0;
    double yOffset = 0;
    ObservableList<String> items = FXCollections.observableArrayList();
    Map<Integer, String> lists = new HashMap<>();

    int getId(String s) {
        String s1 = s.substring(s.indexOf('.') + 1);
        AtomicInteger id = new AtomicInteger();
        lists.forEach((k, v) -> {
            if (v.equals(s1)) id.set(k);
        });
        return id.get();
    }

    void init() {
        lists = DB.getInstance().getLists();
        items.clear();
        AtomicInteger i = new AtomicInteger(1);
        lists.forEach((k, v) -> {
            items.add(i.get() + "." + v);
            i.set(i.get() + 1);
        });
    }

    public void initialize() {
        favList.setTrackColor(Color.valueOf("#C2F3FF"));
        favList.setThumbColor(Color.valueOf("#49BAD6"));
        favList.setThumbHoverColor(Color.valueOf("#57DDFF"));
        favList.features().enableBounceEffect();
        favList.features().enableSmoothScrolling(1);

        favList.setItems(items);

        favList.getSelectionModel().selectionProperty().addListener((InvalidationListener) observable ->
                favList.getSelectionModel().getSelectedValues().forEach(x ->
                        msgLabel.setText("已选择：" + x.substring(x.indexOf('.') + 1))));

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

    public static int select() {
        // 返回选择的ID 没有选择就返回0
        if (controller == null) try {
            FXMLLoader loader = new FXMLLoader(SelectFavoriteController.class.getResource("select-favorite-view.fxml"));
            stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.getScene().setFill(Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            controller = loader.getController();
        } catch (Exception e) {
            System.out.println("open select favorite dialog error:" + e);
        }

        controller.init();
        stage.showAndWait();
        return selected;
    }
}
