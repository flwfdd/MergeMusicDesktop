package xyz.flwfdd.mergemusicdesktop;

import io.github.palexdev.materialfx.controls.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import xyz.flwfdd.mergemusicdesktop.model.table.SearchTable;
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
    MFXTextField searchKey;

    @FXML
    MFXComboBox<Music.Platform> searchPlatform;

    @FXML
    MFXComboBox<Music.Type> searchType;

    @FXML
    MFXTableView<Music> searchTable;
    SearchTable searchTableModel=new SearchTable();

    @FXML
    MFXProgressBar loadingBar;

    @FXML
    void onSearch() { //搜索按钮
        searchTableModel.search();
    }

    @FXML
    void onBack(){ //返回按钮
        searchTableModel.back();
    }

    void initSearchTable(){ //初始化歌曲表
        searchTableModel.bind(searchTable);
    }

    void initSearchInput(){ //初始化搜索栏和多选框
        searchKey.setOnKeyPressed(e->{ //回车搜索
            if(e.getCode()==KeyCode.ENTER)onSearch();
        });
        searchPlatform.setItems(FXCollections.observableList(Arrays.stream(Music.Platform.values()).toList()));
        searchType.setItems(FXCollections.observableList(Arrays.stream(Music.Type.values()).toList()));
        searchPlatform.selectFirst();
        searchType.selectFirst();
        searchTableModel.searchKeyProperty().bindBidirectional(searchKey.textProperty());
        searchTableModel.searchPlatformProperty().bindBidirectional(searchPlatform.valueProperty());
        searchTableModel.searchTypeProperty().bindBidirectional(searchType.valueProperty());
    }

    void initLoadingBar(){ //初始化加载条
        loadingBar.visibleProperty().bind(searchTableModel.loadingProperty());
    }

    public void initialize() {
        initSearchTable();
        initSearchInput();
        initLoadingBar();
    }
}
