package xyz.flwfdd.mergemusicdesktop.model.table;

import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.ArrayList;
import java.util.List;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 22:17
 * @implNote 音乐列表的数据模型
 */
public class MusicTable {

    ObservableList<Music> musicList;

    MFXTableView<Music> tableView;

    public static class Operation { //操作栏
        public String icon, text;
        public Runnable func;

        Operation(String icon, String text, Runnable func) {
            this.icon = icon;
            this.func = func;
            this.text = text;
        }
    }

    public ObservableList<Music> getMusicList() {
        return musicList;
    }

    public MusicTable() {
        musicList = FXCollections.observableArrayList();
    }

    static void createTooltip(MFXTableRowCell<Music, String> rowCell) { //创建单元格悬浮提示
        var tooltip = new MFXTooltip(rowCell);
        tooltip.textProperty().bind(rowCell.textProperty());
        tooltip.install();
    }

    static MFXContextMenuItem createMenuItem(Operation menu) {
        String s = menu.text;
        // 太长会显示溢出 而且调节样式无果 就手动裁剪文字
        if (s.length() > 10) s = s.substring(0, 11) + "…";
        MFXContextMenuItem menuItem = new MFXContextMenuItem(s, new FontIcon(menu.icon));
        menuItem.setTooltipSupplier(() -> {
            var tooltip = new Tooltip();
            tooltip.setText(menu.text);
            return tooltip;
        });
        // 不知道为什么有一些显示左边会凸出去 只能这样硬调一下
        menuItem.setPadding(new Insets(0, 0, 0, 11));
        menuItem.setOnAction(e -> menu.func.run());
        return menuItem;
    }

    void initView(MFXTableView<Music> tableView) { //初始化视图
        // 设置列
        MFXTableColumn<Music> colIndex = new MFXTableColumn<>("   序号");
        colIndex.setRowCellFactory(music -> {
            Text text = new Text();
            var rowCell = new MFXTableRowCell<Music, String>(music1 -> {
                text.setText(tableView.getItems().indexOf(music1) + 1 + "");
                text.setFill(Paint.valueOf(music1.getPlatform().getColor()));
                return "";
            });
            rowCell.setGraphic(text);
            return rowCell;
        });
        colIndex.setMinWidth(42);
        tableView.getTableColumns().add(colIndex);

        MFXTableColumn<Music> colOpt = new MFXTableColumn<>("操作");
        colOpt.setRowCellFactory(music -> {
            var box = new HBox();
            var rowCell = new MFXTableRowCell<Music, String>(m -> {
                box.getChildren().clear();
                this.getOperations(m).forEach(opt -> {
                    var button = new MFXButton();
                    button.setGraphic(new FontIcon(opt.icon + ":17"));
                    button.setOnMouseClicked(e -> opt.func.run());
                    button.getStyleClass().add("option-button");
                    var tooltip = new MFXTooltip(button, opt.text);
                    tooltip.install();
                    button.setRippleColor(Color.rgb(87, 221, 255));
                    box.getChildren().add(button);
                    box.setSpacing(0);
                });
                return "";
            });
            rowCell.setGraphic(box);
            rowCell.setPadding(new Insets(0));
            return rowCell;
        });
        colOpt.setMinWidth(84);
        tableView.getTableColumns().add(colOpt);

        MFXTableColumn<Music> colName = new MFXTableColumn<>("名字");
        colName.setRowCellFactory(music -> {
            var rowCell = new MFXTableRowCell<>(Music::getName);
            createTooltip(rowCell);
            return rowCell;
        });
        tableView.getTableColumns().add(colName);

        MFXTableColumn<Music> colArtists = new MFXTableColumn<>("作者");
        colArtists.setRowCellFactory(music -> {
            var rowCell = new MFXTableRowCell<Music, String>(music1 -> String.join(",", music1.getArtists()));
            createTooltip(rowCell);
            return rowCell;
        });
        tableView.getTableColumns().add(colArtists);

        MFXTableColumn<Music> colAlbum = new MFXTableColumn<>("专辑");
        colAlbum.setRowCellFactory(music -> {
            var rowCell = new MFXTableRowCell<>(Music::getAlbumName);
            createTooltip(rowCell);
            return rowCell;
        });
        tableView.getTableColumns().add(colAlbum);

        // 平均分布各列
        Platform.runLater(() -> {
            var w = (tableView.getWidth() - colIndex.getWidth() - colOpt.getWidth()) / 3 - 2;
            colName.setMinWidth(w);
            colArtists.setMinWidth(w);
            colAlbum.setMinWidth(w);
        });

        // 设置行
        tableView.setTableRowFactory(music -> {
            MFXTableRow<Music> row = new MFXTableRow<>(tableView, music);

            row.setPrefHeight(42);

            // 右键菜单
            MFXContextMenu contextMenu = new MFXContextMenu(row);
            getMenus(music).forEach(menu -> contextMenu.addItem(createMenuItem(menu)));
            row.dataProperty().addListener((observableValue, oldMusic, newMusic) -> {
                contextMenu.getItems().clear();
                getMenus(newMusic).forEach(menu -> contextMenu.addItem(createMenuItem(menu)));
            });
            contextMenu.install();
            return row;
        });

        // 设置滚动特效
        tableView.features().enableBounceEffect(2.4, 24);
        tableView.features().enableSmoothScrolling(1);
    }

    public void bind(MFXTableView<Music> tableView) {
        // 将视图与数据绑定起来
        this.tableView = tableView;
        initView(tableView);
        var musicList = this.getMusicList();
        if (musicList.isEmpty()) {
            musicList.add(null);
            tableView.setItems(musicList);
            musicList.clear();
        } else tableView.setItems(musicList);
    }

    List<Operation> getOperations(Music music) { //操作栏
        List<Operation> operations = new ArrayList<>();
        operations.add(new MusicTable.Operation("mdrmz-play_arrow", "播放", () -> PlayTable.getInstance().play(music)));
        operations.add(new MusicTable.Operation("mdral-add", "添加到播放列表", () -> PlayTable.getInstance().add(music,true)));
        return operations;
    }

    List<Operation> getMenus(Music music) { //右键菜单
        List<Operation> menus = new ArrayList<>();
        menus.add(new MusicTable.Operation("mdral-info", "详细信息", () -> System.out.println("Detail:" + music)));
        return menus;
    }

}

