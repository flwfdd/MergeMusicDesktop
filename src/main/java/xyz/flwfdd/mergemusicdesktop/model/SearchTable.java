package xyz.flwfdd.mergemusicdesktop.model;

import io.github.palexdev.materialfx.controls.MFXTableView;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/23 10:44
 * @implNote 搜索列表的数据模型
 */
public class SearchTable extends MusicTable{
    final int limit;
    String searchKey;
    Music.Platform searchPlatform;
    Music.Type searchType;
    int page;

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }

    SimpleBooleanProperty loading=new SimpleBooleanProperty(false); //保证单个搜索线程

    boolean haveNext=false; //是否可以加载更多

    Stack<ObservableList<Music>> historyMusicList;

    public SearchTable(){
        super();
        limit=24;
        historyMusicList=new Stack<>();
    }

    public void search(String keyword, Music.Platform platform, Music.Type type){
        if(loading.get())return;
        searchKey=keyword;
        searchPlatform=platform;
        searchType=type;
        page=0;
        musicList.clear();
        haveNext=true;
        historyMusicList.clear();
        searchNext();
    }

    public void searchNext(){ //获取下一页
        if(loading.get()||!haveNext||!historyMusicList.isEmpty())return;
        loading.set(true);
        new Thread(new Task<Void>() {
            List<Music> l;
            @Override
            protected Void call(){
                l=Music.search(searchKey,searchPlatform,searchType,limit,page);
                return null;
            }

            @Override
            protected void succeeded() {
                musicList.addAll(l);
                haveNext=(l.size()!=0);
                page++;
                loading.set(false);
            }
        }).start();
    }

    void unfold(Music music){
        if(loading.get())return;
        loading.set(true);
        new Thread(new Task<Void>() {
            List<Music> l;
            @Override
            protected Void call(){
                l=music.unfold();
                return null;
            }

            @Override
            protected void succeeded() {
                historyMusicList.add(FXCollections.observableList(musicList.stream().toList()));
                musicList.setAll(l);
                tableView.getSelectionModel().clearSelection();
                loading.set(false);
            }
        }).start();
    }

    public void back(){
        if(historyMusicList.isEmpty())return;
        musicList.setAll(historyMusicList.pop());
        tableView.getSelectionModel().clearSelection();
    }

    @Override
    List<Operation> getOperations(Music music){
        List<Operation> operations= new ArrayList<>();
        Music.Type type=music.getType();
        if(type== Music.Type.LIST||type== Music.Type.USER){
            operations.add(new MusicTable.Operation("mdrmz-unfold_more", ()->this.unfold(music), "展开"));
        }
        if(type==Music.Type.MUSIC||type==Music.Type.LIST) {
            operations.addAll(super.getOperations(music));
        }
        return operations;
    }

    @Override
    void initView(MFXTableView<Music> tableView){
        super.initView(tableView);
        // 设置滚动监听自动搜索
        tableView.setOnScroll(scrollEvent -> {
            if(scrollEvent.getDeltaY()<0)this.searchNext();
        });
    }

    @Override
    List<Operation> getMenus(Music music){
        if(music.getType()== Music.Type.MUSIC)return super.getMenus(music);
        List<Operation> menus=new ArrayList<>();
        menus.add(new MusicTable.Operation("mdral-info",()->System.out.println("List Detail:"+music), "详细信息"));
        return menus;
    }
}
