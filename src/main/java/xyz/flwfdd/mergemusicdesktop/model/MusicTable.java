package xyz.flwfdd.mergemusicdesktop.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import xyz.flwfdd.mergemusicdesktop.music.Music;

import java.util.List;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/17 22:17
 */
public class MusicTable {
    final int limit;
    String searchKey;
    Music.Platform searchPlatform;
    Music.Type searchType;
    int offset;
    boolean loading=false; //保证单个搜索线程

    ObservableList<Music> musicList;

    public ObservableList<Music> getMusicList() {
        return musicList;
    }

    public MusicTable(){
        limit=24;
        musicList= FXCollections.observableArrayList();
    }

    public void search(String keyword, Music.Platform platform, Music.Type type){
        if(loading)return;
        searchKey=keyword;
        searchPlatform=platform;
        searchType=type;
        offset=0;
        musicList.clear();
        searchNext();
    }

    public void searchNext(){ //获取下一页
        if(loading)return;
        new Thread(new Task<Void>() {
            List<Music>l;
            @Override
            protected Void call(){
                loading=true;
                l=Music.search(searchKey,searchPlatform,searchType,limit,offset);
                return null;
            }

            @Override
            protected void succeeded() {
                musicList.addAll(l);
                loading=false;
            }
        }).start();
    }
}
