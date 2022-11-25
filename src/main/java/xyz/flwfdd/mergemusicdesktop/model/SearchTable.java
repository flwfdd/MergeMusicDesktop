package xyz.flwfdd.mergemusicdesktop.model;

import io.github.palexdev.materialfx.controls.MFXTableView;
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
    final int limit;
    SimpleStringProperty searchKey;
    SimpleObjectProperty<Music.Platform> searchPlatform;
    SimpleObjectProperty<Music.Type> searchType;

    int page;

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }

    SimpleBooleanProperty loading=new SimpleBooleanProperty(false); //保证单个搜索线程

    boolean haveNext=false; //是否可以加载更多

    Deque<ObservableList<Music>> historyMusicList;

    public SimpleStringProperty searchKeyProperty() {
        return searchKey;
    }

    public SimpleObjectProperty<Music.Platform> searchPlatformProperty() {
        return searchPlatform;
    }

    public SimpleObjectProperty<Music.Type> searchTypeProperty() {
        return searchType;
    }

    public SearchTable(){
        super();
        limit=24;
        historyMusicList=new LinkedList<>();
        searchKey=new SimpleStringProperty();
        searchPlatform=new SimpleObjectProperty<>();
        searchType=new SimpleObjectProperty<>();
    }

    void pushHistory(){
        historyMusicList.addLast(FXCollections.observableList(musicList.stream().toList()));
        if(historyMusicList.size()>24)historyMusicList.removeFirst();
    }

    public void search(){
        if(loading.get())return;
        page=0;
        musicList.clear();
        haveNext=true;
        searchNext();
    }

    public void searchNext(){ //获取下一页
        if(loading.get()||!haveNext)return;
        loading.set(true);
        new Thread(new Task<Void>() {
            List<Music> l;
            @Override
            protected Void call(){
                l=Music.search(searchKey.get(),searchPlatform.get(),searchType.get(),limit,page);
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
                pushHistory();
                musicList.setAll(l);
                tableView.getSelectionModel().clearSelection();
                loading.set(false);
            }
        }).start();
    }

    public void back(){
        if(historyMusicList.isEmpty())return;
        musicList.setAll(historyMusicList.removeLast());
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
        music.getArtists().forEach(artist-> menus.add(new Operation("mdomz-search",artist,()->{
            searchKey.set(artist);
            pushHistory();
            search();
        })));
        if(!music.getAlbumName().isBlank())menus.add(new MusicTable.Operation("mdomz-search",music.getAlbumName(),()->{
            searchKey.set(music.getAlbumName());
            pushHistory();
            search();
        }));
        if(music.getType()== Music.Type.MUSIC)menus.addAll(super.getMenus(music));
        else menus.add(new MusicTable.Operation("mdral-info", "详细信息",()->System.out.println("List Detail:"+music)));
        return menus;
    }
}
