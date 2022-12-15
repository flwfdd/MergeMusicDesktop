package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTooltip;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import xyz.flwfdd.mergemusicdesktop.dialog.ConfirmController;
import xyz.flwfdd.mergemusicdesktop.dialog.InputController;
import xyz.flwfdd.mergemusicdesktop.dialog.SelectFavoriteController;
import xyz.flwfdd.mergemusicdesktop.model.table.FavoriteTable;
import xyz.flwfdd.mergemusicdesktop.music.Music;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/12/15 14:00
 * @implNote
 */

public class FavoriteController {
    @FXML
    MFXTableView<Music> favoriteTable;

    @FXML
    Label titleLabel;
    @FXML
    MFXButton changeButton;
    @FXML
    MFXButton clearButton;
    @FXML
    MFXButton renameButton;
    @FXML
    MFXButton playButton;
    @FXML
    MFXButton addButton;

    FavoriteTable favoriteTableModel;

    void createTooltip(Node owner,String s){
        var tooltip = new MFXTooltip(owner);
        tooltip.setText(s);
        tooltip.install();
    }

    public void initialize() {
        favoriteTableModel = FavoriteTable.getInstance();
        favoriteTableModel.bind(favoriteTable);

        titleLabel.textProperty().bind(favoriteTableModel.title);

        createTooltip(changeButton,"选择收藏夹");
        changeButton.setOnAction(e-> {
            int id = SelectFavoriteController.select();
            if (id == 0) return;
            favoriteTableModel.changeList(id);
        });

        createTooltip(playButton,"播放全部");
        playButton.setOnAction(e-> favoriteTableModel.playAll());

        createTooltip(renameButton,"重命名");
        renameButton.setOnAction(e-> {
            String s= InputController.input("你的（新收藏夹）名字是？");
            if(!s.isEmpty())favoriteTableModel.rename(s);
        });

        createTooltip(addButton,"全部添加到播放列表");
        addButton.setOnAction(e-> favoriteTableModel.addAll());

        createTooltip(clearButton,"删除收藏夹");
        clearButton.setOnAction(e-> {
            if(ConfirmController.confirm("当真要删除收藏夹 "+favoriteTableModel.getNowName()+" ？"))favoriteTableModel.deleteList();
        });
    }
}
