package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import xyz.flwfdd.mergemusicdesktop.model.MusicTable;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.Arrays;

public class MainController {

    MainApplication app;
    Stage stage;

    @FXML
    private MFXTextField searchKey;

    @FXML
    private MFXComboBox<Music.Platform> searchPlatform;

    @FXML
    private MFXComboBox<Music.Type> searchType;

    void initComboBox(){
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

    void initSearchTable(){
        // 设置列
        MFXTableColumn<Music> colName=new MFXTableColumn<>("名字",true);
        colName.setRowCellFactory(music->new MFXTableRowCell<>(Music::getName));
        searchTable.getTableColumns().add(colName);

        MFXTableColumn<Music> colArtists=new MFXTableColumn<>("作者",true);
        colArtists.setRowCellFactory(music->new MFXTableRowCell<>(music1 -> String.join(",", music1.getArtists())));
        searchTable.getTableColumns().add(colArtists);

        // 设置右键菜单
        searchTable.setTableRowFactory(m->{
            MFXTableRow<Music> row=new MFXTableRow<>(searchTable,m);
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
//                System.out.println(scrollEvent.getEventType());
        });

        // 设置滚动特效
        searchTable.features().enableBounceEffect(2,24);
        searchTable.features().enableSmoothScrolling(0.5);

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

    public void setMain(MainApplication app,Stage stage){
        this.app=app;
        this.stage=stage;
    }
}