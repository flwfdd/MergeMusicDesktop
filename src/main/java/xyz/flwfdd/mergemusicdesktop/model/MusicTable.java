package xyz.flwfdd.mergemusicdesktop.model;

import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.ArrayList;
import java.util.List;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 22:17
 */
public class MusicTable {

    ObservableList<Music> musicList;

    MFXTableView<Music> tableView;

    public static class Operation{
        public String icon,tooltip;
        public Runnable func;

        Operation(String icon,Runnable func,String tooltip){
            this.icon=icon;
            this.func=func;
            this.tooltip=tooltip;
        }
    }

    public ObservableList<Music> getMusicList() {
        return musicList;
    }

    public MusicTable(){
        musicList= FXCollections.observableArrayList();
    }

    static void createTooltip(MFXTableRowCell<Music, String> rowCell){ //创建单元格悬浮提示
        var tooltip=new MFXTooltip(rowCell);
        tooltip.textProperty().bind(rowCell.textProperty());
        tooltip.install();
    }

    void initView(MFXTableView<Music> tableView){ //初始化视图
        // 设置列
        MFXTableColumn<Music> colIndex=new MFXTableColumn<>("   序号",false);
        colIndex.setRowCellFactory(music-> new MFXTableRowCell<>(music1 -> (tableView.getItems().indexOf(music1) + 1 + "")));
        colIndex.setMinWidth(60);
        tableView.getTableColumns().add(colIndex);

        MFXTableColumn<Music> colOpt=new MFXTableColumn<>("操作",true);
        colOpt.setRowCellFactory(music -> {
            var box=new HBox();
            var rowCell=new MFXTableRowCell<Music,String>(m->{
                box.getChildren().clear();
                this.getOperations(m).forEach(opt->{
                    var button=new MFXButton();
                    button.setGraphic(new FontIcon(opt.icon+":17"));
                    button.setOnMouseClicked(e->opt.func.run());
                    button.getStyleClass().add("option-button");
                    var tooltip=new MFXTooltip(button,opt.tooltip);
                    tooltip.install();
                    button.setRippleColor(Color.rgb(87,221,255));
                    box.getChildren().add(button);
                    box.setSpacing(4);
                });
                return "";
            });
            rowCell.setGraphic(box);
            return rowCell;
        });
        tableView.getTableColumns().add(colOpt);

        MFXTableColumn<Music> colName=new MFXTableColumn<>("名字",true);
        colName.setRowCellFactory(music->{
            var rowCell=new MFXTableRowCell<>(Music::getName);
            createTooltip(rowCell);
            return rowCell;
        });
        tableView.getTableColumns().add(colName);

        MFXTableColumn<Music> colArtists=new MFXTableColumn<>("作者",true);
        colArtists.setRowCellFactory(music->{
            var rowCell=new MFXTableRowCell<Music,String>(music1 -> String.join(",", music1.getArtists()));
            createTooltip(rowCell);
            return rowCell;
        });
        tableView.getTableColumns().add(colArtists);

        MFXTableColumn<Music> colAlbum=new MFXTableColumn<>("专辑",true);
        colAlbum.setRowCellFactory(music->{
            var rowCell=new MFXTableRowCell<>(Music::getAlbumName);
            createTooltip(rowCell);
            return rowCell;
        });
        tableView.getTableColumns().add(colAlbum);

        // 平均分布各列
        Platform.runLater(()->{
            var w=(tableView.getWidth()-colIndex.getWidth()-colOpt.getWidth())/3-1;
            colName.setMinWidth(w);
            colArtists.setMinWidth(w);
            colAlbum.setMinWidth(w);
        });


        // 设置行
        tableView.setTableRowFactory(music->{
            MFXTableRow<Music> row=new MFXTableRow<>(tableView,music);

            row.setPrefHeight(42);

            // 右键菜单
            MFXContextMenu menu=new MFXContextMenu(row);
            MFXContextMenuItem a=new MFXContextMenuItem("a");
            a.setOnAction(e-> System.out.println("Click A"));
            menu.addItem(a);
            menu.addItem(new MFXContextMenuItem("b"));
            menu.install();

            return row;
        });

        // 设置滚动特效
        tableView.features().enableBounceEffect(2.4,24);
        tableView.features().enableSmoothScrolling(1);
    }

    public void bind(MFXTableView<Music> tableView){
        this.tableView=tableView;
        initView(tableView);
        // 绑定数据
        var musicList=this.getMusicList();
        if(musicList.isEmpty()){
            musicList.add(null);
            tableView.setItems(musicList);
            musicList.clear();
        }
        else tableView.setItems(musicList);
    }

    List<Operation> getOperations(Music music){
        List<Operation> operations=new ArrayList<>();
        operations.add(new MusicTable.Operation("mdrmz-play_arrow",()->PlayList.getInstance().play(music), "播放"));
        operations.add(new MusicTable.Operation("mdral-add", ()->PlayList.getInstance().add(music), "添加到播放列表"));
        return operations;
    }

}

