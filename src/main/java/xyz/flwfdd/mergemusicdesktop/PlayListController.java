package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.fxml.FXML;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import xyz.flwfdd.mergemusicdesktop.model.Player;
import xyz.flwfdd.mergemusicdesktop.model.table.PlayTable;
import xyz.flwfdd.mergemusicdesktop.music.Music;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/30 21:32
 * @implNote 播放列表
 */
public class PlayListController {
    @FXML
    MFXTableView<Music> playTable;
    PlayTable playTableModel;

    public void initialize() {
        playTableModel = PlayTable.getInstance();
        playTableModel.bind(playTable);

        // 在列表中标注正在播放的
        var playingMusic = Player.getInstance().playingMusicProperty();
        playingMusic.addListener(observable -> playTable.getCells().forEach((ind, row) -> {
            if (row.getData().getMid().equals(playingMusic.get().getMid())) {
                ((Text)row.getCells().get(0).getGraphic()).setText("◉");
            } else ((Text)row.getCells().get(0).getGraphic()).setText(ind + 1 + "");
        }));
        var colIndex = playTable.getTableColumns().get(0);
        colIndex.setRowCellFactory(music -> {
            Text text=new Text();
            var rowCell=new MFXTableRowCell<Music,String>((music1 -> {
                if (playingMusic.get() != null && music1.getMid().equals(playingMusic.get().getMid()))text.setText("◉");
                else text.setText(playTable.getItems().indexOf(music1) + 1 + "");
                text.setFill(Paint.valueOf(music1.getPlatform().getColor()));
                return "";
            }));
            rowCell.setGraphic(text);
            return rowCell;
        });
    }
}
