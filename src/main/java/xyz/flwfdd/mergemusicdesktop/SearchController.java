package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
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

    @FXML
    private MFXButton searchButton;

    @FXML
    private MFXTableView<Music> searchTable;
    MusicTable searchTableModel=new MusicTable();

    @FXML
    protected void onSearch() {
        searchTableModel.search(searchKey.getText(),searchPlatform.getSelectedItem(),searchType.getSelectedItem());
    }

    void initSearchTable(){
        MusicTable.initView(searchTable);
        searchTableModel.bind(searchTable);
        // 设置滚动监听自动搜索
        searchTable.setOnScroll(scrollEvent -> {
            if(scrollEvent.getDeltaY()<0)searchTableModel.searchNext();
        });
    }

    void initSearchInput(){ //初始化搜索栏和多选框
        searchKey.setOnKeyPressed(e->{
            if(e.getCode()==KeyCode.ENTER)onSearch();
        });
        searchPlatform.setItems(FXCollections.observableList(Arrays.stream(Music.Platform.values()).toList()));
        searchType.setItems(FXCollections.observableList(Arrays.stream(Music.Type.values()).toList()));
        searchPlatform.selectFirst();
        searchType.selectFirst();
    }

    public void initialize() {
        initSearchTable();
        initSearchInput();
    }
}
