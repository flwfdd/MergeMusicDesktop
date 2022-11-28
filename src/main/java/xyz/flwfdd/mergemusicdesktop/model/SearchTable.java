package xyz.flwfdd.mergemusicdesktop.model;

import io.github.palexdev.materialfx.controls.MFXTableView;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.*;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/23 10:44
 * @implNote 搜索列表的数据模型
 */
public class SearchTable extends MusicTable{

    public static class SearchCase{ //搜索实例
        public int page;
        public boolean haveNext;
        public String key;
        public Music.Platform platform;
        public Music.Type type;
        public ObservableList<Music> list;
        public InvalidationListener musicTableListener;

        SearchCase(List<Music> l){
            haveNext=false;
            list=FXCollections.observableArrayList();
            list.addAll(l);
        }

        SearchCase(String key, Music.Platform platform, Music.Type type){
            this.key=key;
            this.platform=platform;
            this.type=type;
            page=0;
            haveNext=true;
            list=FXCollections.observableArrayList();
        }
    }

    final int limit;
    SimpleStringProperty searchKey;
    SimpleObjectProperty<Music.Platform> searchPlatform;
    SimpleObjectProperty<Music.Type> searchType;
    SimpleObjectProperty<SearchCase> nowSearchCase;

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }

    SimpleBooleanProperty loading=new SimpleBooleanProperty(false); //保证单个搜索线程

    Deque<SearchCase> historySearchCase;

    public SearchTable(){
        super();
        limit=24;
        historySearchCase=new LinkedList<>();
        searchKey=new SimpleStringProperty();
        searchPlatform=new SimpleObjectProperty<>();
        searchType=new SimpleObjectProperty<>();

        nowSearchCase=new SimpleObjectProperty<>();
        nowSearchCase.addListener((observableValue, oldCase, newCase) -> {
            if(oldCase!=null)oldCase.list.removeListener(oldCase.musicTableListener);
            musicList.setAll(newCase.list);
            newCase.musicTableListener=observable->musicList.setAll(newCase.list);
            newCase.list.addListener(newCase.musicTableListener);
        });
    }

    void pushHistory(){
        if(nowSearchCase.get()==null||nowSearchCase.get().list.isEmpty())return;
//        historySearchCase.addLast(FXCollections.observableList(musicList.stream().toList()));
        historySearchCase.addLast(nowSearchCase.get());
        if(historySearchCase.size()>24)historySearchCase.removeFirst();
    }

    public void search(){
        if(loading.get())return;
        pushHistory();
        nowSearchCase.set(new SearchCase(searchKey.get(),searchPlatform.get(),searchType.get()));
        searchNext();
    }

    public void searchNext(){ //获取下一页
        if(loading.get()||!nowSearchCase.get().haveNext)return;
        loading.set(true);
        new Thread(new Task<Void>() {
            List<Music> l;
            @Override
            protected Void call(){
                l=Music.search(nowSearchCase.get().key,nowSearchCase.get().platform,nowSearchCase.get().type,limit,nowSearchCase.get().page);
                return null;
            }

            @Override
            protected void succeeded() {
                nowSearchCase.get().list.addAll(l);
                nowSearchCase.get().haveNext=(l.size()!=0);
                nowSearchCase.get().page++;
                tableView.getSelectionModel().clearSelection();
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
                pushHistory();
                nowSearchCase.set(new SearchCase(l));
                tableView.getSelectionModel().clearSelection();
                loading.set(false);
            }
        }).start();
    }

    public void back(){
        if(historySearchCase.isEmpty())return;
        nowSearchCase.set(historySearchCase.removeLast());
        tableView.getSelectionModel().clearSelection();
    }

    @Override
    List<Operation> getOperations(Music music){
        List<Operation> operations= new ArrayList<>();
        Music.Type type=music.getType();
        if(type== Music.Type.LIST||type== Music.Type.USER){
            operations.add(new MusicTable.Operation("mdrmz-unfold_more", "展开", ()->this.unfold(music)));
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
        List<Operation> menus=new ArrayList<>();
        Set<String>keySet=new LinkedHashSet<>();
        keySet.add(music.getName());
        keySet.addAll(music.getArtists());
        if(!music.getAlbumName().isBlank())keySet.add(music.getAlbumName());
        keySet.forEach(key-> menus.add(new Operation("mdomz-search",key,()->{
            searchKey.set(key);
            search();
        })));
        if(music.getType()== Music.Type.MUSIC)menus.addAll(super.getMenus(music));
        else menus.add(new MusicTable.Operation("mdral-info", "详细信息",()->System.out.println("List Detail:"+music)));
        return menus;
    }

    public SimpleStringProperty searchKeyProperty() {
        return searchKey;
    }

    public SimpleObjectProperty<Music.Platform> searchPlatformProperty() {
        return searchPlatform;
    }

    public SimpleObjectProperty<Music.Type> searchTypeProperty() {
        return searchType;
    }
}
