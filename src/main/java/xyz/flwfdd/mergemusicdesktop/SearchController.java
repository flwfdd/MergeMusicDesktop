package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import xyz.flwfdd.mergemusicdesktop.model.MusicTable;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.Arrays;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/5 14:52
 * @implNote 搜索面板
 */

public class SearchController {

    @FXML
    private MFXTextField searchKey;

    @FXML
    private MFXComboBox<Music.Platform> searchPlatform;

    @FXML
    private MFXComboBox<Music.Type> searchType;

    void initComboBox(){ //初始化多选框
        searchPlatform.setItems(FXCollections.observableList(Arrays.stream(Music.Platform.values()).toList()));
        searchType.setItems(FXCollections.observableList(Arrays.stream(Music.Type.values()).toList()));
        searchPlatform.selectFirst();
        searchType.selectFirst();
    }

    @FXML
    private MFXTableView<Music> searchTable;
    MusicTable searchTableModel=new MusicTable();

    @FXML
    protected void onSearch() {
        searchTableModel.search(searchKey.getText(),searchPlatform.getSelectedItem(),searchType.getSelectedItem());
    }

    void createTooltip(MFXTableRowCell<Music,String> rowCell){ //创建单元格悬浮提示
        var tooltip=new MFXTooltip(rowCell);
        tooltip.textProperty().bind(rowCell.textProperty());
        tooltip.install();
    }

    void initSearchTable(){
        // 设置列
        MFXTableColumn<Music> colIndex=new MFXTableColumn<>("   序号",false);
        colIndex.setRowCellFactory(music-> new MFXTableRowCell<>(music1 -> (searchTableModel.getMusicList().indexOf(music1) + 1 + "")));
        colIndex.setMinWidth(60);
        searchTable.getTableColumns().add(colIndex);

        MFXTableColumn<Music> colOpt=new MFXTableColumn<>("操作",true);
        colOpt.setRowCellFactory(music -> {
            var rowCell=new MFXTableRowCell<Music,String>(m->"");
            var box=new HBox();
            music.getOperations().forEach(opt->{
                var button=new MFXButton(opt.text);
                button.setOnMouseClicked(e->opt.func.run());
                button.setStyle("-fx-border-width: 0.5;-fx-border-color:black;-fx-border-radius: 11%;-fx-background-radius:11%;-fx-font-size: 16px;-fx-background-color: transparent");
                var tooltip=new MFXTooltip(button,opt.tooltip);
                tooltip.install();
                button.setRippleColor(Color.rgb(87,221,255));
                box.getChildren().add(button);
                box.setSpacing(4);
            });
            rowCell.setGraphic(box);
            return rowCell;
        });
        searchTable.getTableColumns().add(colOpt);

        MFXTableColumn<Music> colName=new MFXTableColumn<>("名字",true);
        colName.setRowCellFactory(music->{
            var rowCell=new MFXTableRowCell<>(Music::getName);
            createTooltip(rowCell);
            return rowCell;
        });
        searchTable.getTableColumns().add(colName);

        MFXTableColumn<Music> colArtists=new MFXTableColumn<>("作者",true);
        colArtists.setRowCellFactory(music->{
            var rowCell=new MFXTableRowCell<Music,String>(music1 -> String.join(",", music1.getArtists()));
            createTooltip(rowCell);
            return rowCell;
        });
        searchTable.getTableColumns().add(colArtists);

        MFXTableColumn<Music> colAlbum=new MFXTableColumn<>("专辑",true);
        colAlbum.setRowCellFactory(music->{
            var rowCell=new MFXTableRowCell<>(Music::getAlbumName);
            createTooltip(rowCell);
            return rowCell;
        });
        searchTable.getTableColumns().add(colAlbum);

        // 平均分布各列
        Platform.runLater(()->{
            var w=(searchTable.getWidth()-colIndex.getWidth()-colOpt.getWidth())/3-1;
            colName.setMinWidth(w);
            colArtists.setMinWidth(w);
            colAlbum.setMinWidth(w);
        });


        // 设置行
        searchTable.setTableRowFactory(music->{
            MFXTableRow<Music> row=new MFXTableRow<>(searchTable,music);

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

        // 设置滚动监听
        searchTable.setOnScroll(scrollEvent -> {
            if(scrollEvent.getDeltaY()<0)searchTableModel.searchNext();
        });

        // 设置滚动特效
        searchTable.features().enableBounceEffect(2.4,24);
        searchTable.features().enableSmoothScrolling(1);

        // 绑定数据
        var musicList=searchTableModel.getMusicList();
        musicList.add(null);
        searchTable.setItems(musicList);
        musicList.clear();
    }


    public void initialize() {
        initSearchTable();
        initComboBox();
    }
}